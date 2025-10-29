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
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@AutoService(BugChecker.class)
@BugPattern(
        severity = SeverityLevel.ERROR,
        summary = "Use the varargs overload of methods to get syntax highlighting, rather than manually formatting"
                + " strings with .formatted() or String.format()")
public final class GradleTestStringFormatting extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {

    private static final Matcher<ExpressionTree> FORMATTED_STRING = Matchers.anyOf(
            Matchers.instanceMethod().onExactClass("java.lang.String").named("formatted"),
            Matchers.staticMethod().onClass("java.lang.String").named("format"));

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (GradlePluginTestHelpers.notWithinGradlePluginTests(tree, state)) {
            return Description.NO_MATCH;
        }

        if (!GradlePluginTestHelpers.isLibraryMethod(tree, state)) {
            return Description.NO_MATCH;
        }

        if (!hasFormatMethodOverload(tree, state)) {
            return Description.NO_MATCH;
        }

        List<? extends ExpressionTree> arguments = tree.getArguments();
        if (arguments.isEmpty()) {
            return Description.NO_MATCH;
        }

        ExpressionTree firstArg = arguments.get(0);

        if (FORMATTED_STRING.matches(firstArg, state)) {
            return describeMatch(tree);
        }

        if (firstArg instanceof IdentifierTree identifier) {
            if (isIdentifierInitialisedWithFormattedString(identifier, state)) {
                return describeMatch(tree);
            }
        }

        return Description.NO_MATCH;
    }

    private static boolean hasFormatMethodOverload(MethodInvocationTree tree, VisitorState state) {
        return Optional.ofNullable(ASTHelpers.getSymbol(tree))
                .map(methodSymbol -> hasFormatOverloadInClass(methodSymbol, state))
                .orElse(false);
    }

    private static boolean hasFormatOverloadInClass(Symbol.MethodSymbol methodSymbol, VisitorState state) {
        String methodName = methodSymbol.getSimpleName().toString();
        Symbol.ClassSymbol classSymbol = methodSymbol.enclClass();

        return StreamSupport.stream(classSymbol.members().getSymbols().spliterator(), false)
                .filter(Symbol.MethodSymbol.class::isInstance)
                .map(Symbol.MethodSymbol.class::cast)
                .filter(method -> method.getSimpleName().toString().equals(methodName))
                .filter(method ->
                        ASTHelpers.hasAnnotation(method, "com.google.errorprone.annotations.FormatMethod", state))
                .anyMatch(method -> hasFormatStringParameter(method, state));
    }

    private static boolean hasFormatStringParameter(Symbol.MethodSymbol method, VisitorState state) {
        return method.getParameters().stream()
                .anyMatch(param ->
                        ASTHelpers.hasAnnotation(param, "com.google.errorprone.annotations.FormatString", state));
    }

    private static boolean isIdentifierInitialisedWithFormattedString(IdentifierTree identifier, VisitorState state) {
        return Optional.ofNullable(ASTHelpers.getSymbol(identifier))
                .filter(Symbol.VarSymbol.class::isInstance)
                .map(Symbol.VarSymbol.class::cast)
                .map(varSymbol -> JavacTrees.instance(state.context).getTree(varSymbol))
                .filter(VariableTree.class::isInstance)
                .map(VariableTree.class::cast)
                .map(VariableTree::getInitializer)
                .map(initializer -> GradleTestStringFormatting.FORMATTED_STRING.matches(initializer, state))
                .orElse(false);
    }
}
