/*
 * (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.gradle.testing.errorprone;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@AutoService(BugChecker.class)
@BugPattern(severity = SeverityLevel.ERROR, summary = """
    Plugins must be added using .plugins().add() method. Use gradleFile.plugins().add("plugin-id") instead.
    """)
public final class GradleTestPluginsBlock extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {
    private static final Matcher<ExpressionTree> STRING_TYPE = Matchers.isSubtypeOf("java.lang.String");
    private static final Matcher<ExpressionTree> FILE_EDITOR_TYPE =
            Matchers.isSubtypeOf("com.palantir.gradle.testing.files.ProjectFile.FileEditor");

    private static final Pattern SIMPLE_PLUGIN_DECLARATIONS = Pattern.compile("apply plugin:\\s*['\"]([^'\"]+)['\"]"
            + "|plugins\\.apply\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)"
            + "|pluginManagement\\.apply\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)");
    private static final Pattern PLUGINS_BLOCK_ID =
            Pattern.compile("id\\s+['\"]([^'\"]+)['\"](?:\\s+apply\\s+(false|true))?");
    private static final Pattern PLUGINS_BLOCK = Pattern.compile("plugins\\s*\\{([^}]*)\\}", Pattern.DOTALL);
    private static final Pattern COMMENT = Pattern.compile("//.*|(?s)/\\*.*?\\*/");
    private static final Pattern EMPTY_PLUGINS = Pattern.compile("(?s)plugins\\s*\\{\\s*\\}");

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (!isContentMethod(tree, state)
                || GradlePluginTestHelpers.notGradlePluginTestsLibraryMethod(tree)
                || GradlePluginTestHelpers.notWithinGradlePluginTests(tree, state)) {
            return Description.NO_MATCH;
        }

        return tree.getArguments().stream()
                .findFirst()
                .flatMap(arg -> checkForPlugins(arg, tree, state))
                .orElse(Description.NO_MATCH);
    }

    private static boolean isContentMethod(MethodInvocationTree tree, VisitorState state) {
        return Optional.ofNullable(ASTHelpers.getSymbol(tree))
                .filter(method -> !method.getParameters().isEmpty())
                .filter(method -> tree.getArguments().stream()
                        .findFirst()
                        .map(firstArg ->
                                STRING_TYPE.matches(firstArg, state) || FILE_EDITOR_TYPE.matches(firstArg, state))
                        .orElse(false))
                .filter(method -> isProjectFileSubtype(method.enclClass(), state))
                .isPresent();
    }

    private static boolean isProjectFileSubtype(Symbol.ClassSymbol classSymbol, VisitorState state) {
        Type classType = classSymbol.type;
        Type projectFileType = state.getTypeFromString("com.palantir.gradle.testing.files.ProjectFile");
        return ASTHelpers.isSubtype(classType, projectFileType, state);
    }

    private Optional<Description> checkForPlugins(ExpressionTree arg, MethodInvocationTree tree, VisitorState state) {
        Optional<String> content =
                getStringLiteral(arg).or(() -> resolveStringContent(arg, state)).or(() -> extractStringFromLambda(arg));
        if (content.isEmpty()) {
            return Optional.empty();
        }

        List<PluginInfo> plugins = extractPlugins(content.get());
        if (plugins.isEmpty()) {
            return Optional.empty();
        }

        // Only autofix if arg is a literal or lambda expression (not a variable reference)
        // AND we're not calling a @FormatMethod as we might be formatting the plugins block which would get very
        // complicated
        // AND we're not in a method chain (can be a FLUP, although I think chaining will be very rare)
        boolean canAutofix = (arg instanceof LiteralTree || arg instanceof LambdaExpressionTree)
                && !GradlePluginTestHelpers.isFormatMethodInvocation(tree, state)
                && !isInMethodChain(tree, state);
        return Optional.of(
                canAutofix
                        ? buildDescription(tree)
                                .addFix(createFix(tree, content.get(), plugins, state))
                                .build()
                        : buildDescription(tree).build());
    }

    private static boolean isInMethodChain(MethodInvocationTree tree, VisitorState state) {
        // Check if this is being called on a chain of content methods (receiver is a content method invocation)
        if (ASTHelpers.getReceiver(tree) instanceof MethodInvocationTree receiver && isContentMethod(receiver, state)) {
            return true;
        }
        // Check if something is being chained off of this call - walk up tree looking for a content MethodInvocation
        // using us
        return Stream.iterate(state.getPath().getParentPath(), Objects::nonNull, TreePath::getParentPath)
                .map(TreePath::getLeaf)
                .filter(MethodInvocationTree.class::isInstance)
                .map(MethodInvocationTree.class::cast)
                .filter(parentMethod -> ASTHelpers.getReceiver(parentMethod) == tree)
                .anyMatch(parentMethod -> isContentMethod(parentMethod, state));
    }

    private static Optional<String> getStringLiteral(ExpressionTree expr) {
        return expr instanceof LiteralTree lit && lit.getValue() instanceof String s
                ? Optional.of(s)
                : Optional.empty();
    }

    private static Optional<String> resolveStringContent(ExpressionTree expr, VisitorState state) {
        return ASTHelpers.getSymbol(expr) instanceof Symbol.VarSymbol var
                ? GradlePluginTestHelpers.findVariableInitializer(var, state)
                        .flatMap(GradleTestPluginsBlock::getStringLiteral)
                : Optional.empty();
    }

    private static Optional<String> extractStringFromLambda(ExpressionTree expr) {
        if (!(expr instanceof LambdaExpressionTree lambda)) {
            return Optional.empty();
        }

        // For expression lambda: text -> text + "string"
        if (lambda.getBody() instanceof BinaryTree binary) {
            return extractStringFromBinaryTree(binary);
        }

        // For block lambda: text -> { return text + "string"; }
        if (lambda.getBody() instanceof BlockTree block) {
            return block.getStatements().stream()
                    .filter(ReturnTree.class::isInstance)
                    .map(ReturnTree.class::cast)
                    .map(ReturnTree::getExpression)
                    .filter(BinaryTree.class::isInstance)
                    .map(BinaryTree.class::cast)
                    .findFirst()
                    .flatMap(GradleTestPluginsBlock::extractStringFromBinaryTree);
        }

        return Optional.empty();
    }

    private static Optional<String> extractStringFromBinaryTree(BinaryTree binary) {
        // Recursively search for string literals in the binary tree
        Optional<String> left = binary.getLeftOperand() instanceof LiteralTree
                ? getStringLiteral(binary.getLeftOperand())
                : binary.getLeftOperand() instanceof BinaryTree
                        ? extractStringFromBinaryTree((BinaryTree) binary.getLeftOperand())
                        : Optional.empty();

        Optional<String> right = binary.getRightOperand() instanceof LiteralTree
                ? getStringLiteral(binary.getRightOperand())
                : binary.getRightOperand() instanceof BinaryTree
                        ? extractStringFromBinaryTree((BinaryTree) binary.getRightOperand())
                        : Optional.empty();

        // Combine both sides
        return left.flatMap(l -> right.map(r -> l + r)).or(() -> left).or(() -> right);
    }

    private static List<PluginInfo> extractPlugins(String content) {
        String cleaned = COMMENT.matcher(content).replaceAll("");

        Stream<PluginInfo> simple = SIMPLE_PLUGIN_DECLARATIONS
                .matcher(cleaned)
                .results()
                .map(m -> {
                    String pluginId = IntStream.rangeClosed(1, 3)
                            .mapToObj(m::group)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElseThrow();
                    return new PluginInfo(pluginId, Optional.empty());
                });

        Stream<PluginInfo> blocks = PLUGINS_BLOCK.matcher(cleaned).results().flatMap(block -> PLUGINS_BLOCK_ID
                .matcher(block.group(1))
                .results()
                .map(i -> new PluginInfo(
                        i.group(1), Optional.ofNullable(i.group(2)).map(Boolean::valueOf))));

        return Stream.concat(simple, blocks).toList();
    }

    private static SuggestedFix createFix(
            MethodInvocationTree tree, String original, List<PluginInfo> plugins, VisitorState state) {
        String cleaned = removePluginDeclarations(original);
        String receiver = getReceiver(tree, state);
        String pluginsStmt = buildPluginsStatement(receiver, plugins, state, tree);

        // If the cleaned content is empty or whitespace-only, delete the entire method call
        if (cleaned.trim().isEmpty()) {
            return SuggestedFix.builder()
                    .replace(tree, "")
                    .postfixWith(tree, pluginsStmt)
                    .build();
        }

        String method = ASTHelpers.getSymbol(tree).getSimpleName().toString();
        String originalArg = state.getSourceForNode(tree.getArguments().get(0));
        String replacement = buildMethodCall(receiver, method, cleaned, originalArg, tree, state);

        return SuggestedFix.builder()
                .replace(tree, replacement)
                .postfixWith(tree, pluginsStmt)
                .build();
    }

    private static String removePluginDeclarations(String content) {
        String result = Stream.of(SIMPLE_PLUGIN_DECLARATIONS, PLUGINS_BLOCK_ID)
                .reduce(content, (acc, pattern) -> pattern.matcher(acc).replaceAll(""), (a, b) -> b);
        return EMPTY_PLUGINS.matcher(result).replaceAll("").trim();
    }

    private static String getReceiver(MethodInvocationTree tree, VisitorState state) {
        return Optional.ofNullable(state.getSourceForNode(tree.getMethodSelect()))
                .map(src -> {
                    int dot = src.lastIndexOf('.');
                    return dot >= 0 ? src.substring(0, dot) : src;
                })
                .orElse("");
    }

    private static String buildMethodCall(
            String receiver,
            String method,
            String cleaned,
            String originalArg,
            MethodInvocationTree tree,
            VisitorState state) {
        String content = formatContentArg(cleaned, originalArg);
        String additionalArgs = tree.getArguments().stream()
                .skip(1)
                .map(state::getSourceForNode)
                .reduce("", (acc, arg) -> acc + ", " + arg);

        return receiver + "." + method + "(" + content + additionalArgs + ");";
    }

    private static String formatContentArg(String content, String originalArg) {
        if (originalArg.contains("->")) {
            return formatLambdaArg(content, originalArg);
        }

        if (originalArg.startsWith("\"\"\"") && !content.isEmpty()) {
            return "\"\"\"\n%s\n\"\"\"".formatted(content);
        }

        return "\"" + content + "\"";
    }

    private static String formatLambdaArg(String content, String originalArg) {
        int arrowIndex = originalArg.indexOf("->");
        String lambdaParam = originalArg.substring(0, arrowIndex).trim();

        // Check if it's a block lambda: text -> { ... }
        String lambdaBody = originalArg.substring(arrowIndex + 2).trim();
        if (lambdaBody.startsWith("{")) {
            // Block lambda: reconstruct as text -> { return text + ""; }
            return "%s -> { return %s + \"%s\"; }".formatted(lambdaParam, lambdaParam, content);
        }

        // Expression lambda: text -> text + ""
        return "%s -> %s + \"%s\"".formatted(lambdaParam, lambdaParam, content);
    }

    private static String buildPluginsStatement(
            String receiver, List<PluginInfo> plugins, VisitorState state, MethodInvocationTree tree) {
        String calls = plugins.stream()
                .distinct()
                .map(p -> ".%s(\"%s\")".formatted(p.apply().orElse(true) ? "add" : "addWithoutApply", p.id()))
                .collect(Collectors.joining());

        return "\n    %s.plugins()%s".formatted(receiver, calls);
    }

    private record PluginInfo(String id, Optional<Boolean> apply) {}
}
