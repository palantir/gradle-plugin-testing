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

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import java.util.Optional;

public final class GradlePluginTestHelpers {
    private static final Matcher<Tree> WITHIN_GRADLE_PLUGIN_TESTS_CLASS = Matchers.enclosingNode(Matchers.allOf(
            Matchers.isInstance(ClassTree.class),
            Matchers.hasAnnotation("com.palantir.gradle.testing.junit.GradlePluginTests")));

    private static final String LIBRARY_PACKAGE = "com.palantir.gradle.testing.";

    static boolean notWithinGradlePluginTests(Tree tree, VisitorState state) {
        return !WITHIN_GRADLE_PLUGIN_TESTS_CLASS.matches(tree, state);
    }

    static boolean isGradlePluginTestsLibraryMethod(MethodInvocationTree tree, VisitorState state) {
        return Optional.ofNullable(ASTHelpers.getSymbol(tree))
                .map(Symbol.MethodSymbol::enclClass)
                .map(classSymbol -> classSymbol.getQualifiedName().toString())
                .filter(className -> className.startsWith(LIBRARY_PACKAGE))
                .isPresent();
    }

    private GradlePluginTestHelpers() {}
}
