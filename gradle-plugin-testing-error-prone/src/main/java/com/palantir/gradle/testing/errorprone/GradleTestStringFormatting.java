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

@AutoService(BugChecker.class)
@BugPattern(
        severity = SeverityLevel.ERROR,
        summary = "Use the varargs overload of methods in ProjectFile and JavaSrcDir to get syntax "
                + "highlighting, rather than manually formatting strings with .formatted() or String.format()")
public final class GradleTestStringFormatting extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {

    private static final Matcher<ExpressionTree> FORMATTED_STRING = Matchers.anyOf(
            Matchers.instanceMethod().onExactClass("java.lang.String").named("formatted"),
            Matchers.staticMethod().onClass("java.lang.String").named("format"));

    private static final Matcher<ExpressionTree> TARGET_METHOD = Matchers.anyOf(
            Matchers.instanceMethod().onDescendantOf("com.palantir.gradle.testing.files.ProjectFile"),
            Matchers.instanceMethod().onDescendantOf("com.palantir.gradle.testing.files.java.JavaSrcDir"));

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (!GradleTestFormatHelpers.isWithinGradlePluginTests(tree, state)) {
            return Description.NO_MATCH;
        }

        if (!TARGET_METHOD.matches(tree, state)) {
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
            if (isIdentifierInitialisedWith(identifier, state)) {
                return describeMatch(tree);
            }
        }

        return Description.NO_MATCH;
    }

    /**
     * Checks if an identifier references a variable that was initialised with the given matcher.
     */
    private static boolean isIdentifierInitialisedWith(IdentifierTree identifier, VisitorState state) {
        return Optional.ofNullable(ASTHelpers.getSymbol(identifier))
                .filter(Symbol.VarSymbol.class::isInstance)
                .map(Symbol.VarSymbol.class::cast)
                .map(varSymbol -> JavacTrees.instance(state.context).getTree(varSymbol))
                .filter(VariableTree.class::isInstance)
                .map(VariableTree.class::cast)
                .map(VariableTree::getInitializer)
                .map(initialiser -> GradleTestStringFormatting.FORMATTED_STRING.matches(initialiser, state))
                .orElse(false);
    }
}
