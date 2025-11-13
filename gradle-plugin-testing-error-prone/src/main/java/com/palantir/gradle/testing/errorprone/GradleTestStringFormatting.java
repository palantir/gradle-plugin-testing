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
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@AutoService(BugChecker.class)
@BugPattern(
        severity = SeverityLevel.ERROR,
        summary = "Use the varargs overload of methods to get syntax highlighting, rather than manually formatting"
                + " strings with .formatted() or String.format()")
public final class GradleTestStringFormatting extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {

    private static final Matcher<ExpressionTree> INSTANCE_FORMATTED =
            Matchers.instanceMethod().onExactClass("java.lang.String").named("formatted");

    private static final Matcher<ExpressionTree> STRING_FORMAT =
            Matchers.staticMethod().onClass("java.lang.String").named("format");

    private static final Matcher<ExpressionTree> FORMATTED_STRING = Matchers.anyOf(INSTANCE_FORMATTED, STRING_FORMAT);

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (GradlePluginTestHelpers.notGradlePluginTestsLibraryMethod(tree)) {
            return Description.NO_MATCH;
        }

        if (GradlePluginTestHelpers.notWithinGradlePluginTests(tree, state)) {
            return Description.NO_MATCH;
        }

        if (!GradlePluginTestHelpers.hasFormatMethodOverload(tree, state)) {
            return Description.NO_MATCH;
        }

        List<? extends ExpressionTree> arguments = tree.getArguments();
        if (arguments.isEmpty()) {
            return Description.NO_MATCH;
        }

        ExpressionTree firstArg = arguments.get(0);

        if (INSTANCE_FORMATTED.matches(firstArg, state)) {
            return buildDescription(tree)
                    .addFix(createFixForInstanceFormatted(tree, (MethodInvocationTree) firstArg, state))
                    .build();
        }

        if (STRING_FORMAT.matches(firstArg, state)) {
            return buildDescription(tree)
                    .addFix(createFixForStringFormat(tree, (MethodInvocationTree) firstArg, state))
                    .build();
        }

        if (firstArg instanceof IdentifierTree identifier) {
            if (isIdentifierInitialisedWithFormattedString(identifier, state)) {
                // Don't provide auto-fix for pre-formatted variables as it could leave around an unused variables
                return describeMatch(tree);
            }
        }

        return Description.NO_MATCH;
    }

    private static boolean isIdentifierInitialisedWithFormattedString(IdentifierTree identifier, VisitorState state) {
        return Optional.ofNullable(ASTHelpers.getSymbol(identifier))
                .filter(Symbol.VarSymbol.class::isInstance)
                .map(Symbol.VarSymbol.class::cast)
                .flatMap(varSymbol -> GradlePluginTestHelpers.findVariableInitializer(varSymbol, state))
                .map(initializer -> FORMATTED_STRING.matches(initializer, state))
                .orElse(false);
    }

    private static SuggestedFix createFixForInstanceFormatted(
            MethodInvocationTree outerCall, MethodInvocationTree formattedCall, VisitorState state) {
        MemberSelectTree memberSelect = (MemberSelectTree) formattedCall.getMethodSelect();
        ExpressionTree formatString = memberSelect.getExpression();
        List<? extends ExpressionTree> formatArgs = formattedCall.getArguments();

        return buildFix(outerCall, formatString, formatArgs, state);
    }

    private static SuggestedFix createFixForStringFormat(
            MethodInvocationTree outerCall, MethodInvocationTree formatCall, VisitorState state) {
        List<? extends ExpressionTree> formatCallArgs = formatCall.getArguments();
        if (formatCallArgs.isEmpty()) {
            return SuggestedFix.emptyFix();
        }

        ExpressionTree formatString = formatCallArgs.get(0);
        List<? extends ExpressionTree> formatArgs = formatCallArgs.subList(1, formatCallArgs.size());

        return buildFix(outerCall, formatString, formatArgs, state);
    }

    private static SuggestedFix buildFix(
            MethodInvocationTree outerCall,
            ExpressionTree formatString,
            List<? extends ExpressionTree> formatArgs,
            VisitorState state) {
        List<String> allArgs = new ArrayList<>();

        allArgs.add(state.getSourceForNode(formatString));

        formatArgs.stream().map(state::getSourceForNode).forEach(allArgs::add);

        outerCall.getArguments().stream().skip(1).map(state::getSourceForNode).forEach(allArgs::add);

        String methodSelect = state.getSourceForNode(outerCall.getMethodSelect());
        String arguments = String.join(", ", allArgs);

        return SuggestedFix.replace(outerCall, methodSelect + "(" + arguments + ")");
    }
}
