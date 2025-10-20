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
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;
import java.util.stream.StreamSupport;

@AutoService(BugChecker.class)
@BugPattern(severity = SeverityLevel.ERROR, summary = """
    Avoid using temporary directories or files in Gradle tests, as it can be hard to view the \
    contents of it for debugging. Instead, just make a directory or file using `RootProject` or \
    `SubProject`. These will remain around after the test completes in the `build/gradle-plugin-testing` \
    directory, aiding debugging.
    """)
public final class GradleTestTemporaryFile extends BugChecker
        implements BugChecker.MethodInvocationTreeMatcher, BugChecker.VariableTreeMatcher {
    private static final Matcher<ExpressionTree> MANUAL_TEMPORARY_METHOD_MATCHER = Matchers.staticMethod()
            .onClassAny(
                    "java.io.File",
                    "java.nio.file.Files",
                    "com.google.common.io.Files",
                    "org.apache.commons.io.FileUtils")
            .namedAnyOf(
                    "createTempDir",
                    "createTempDirectory",
                    "createTempFile",
                    "getTempDirectory",
                    "getTempDirectoryPath");

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (!MANUAL_TEMPORARY_METHOD_MATCHER.matches(tree, state)) {
            return Description.NO_MATCH;
        }

        boolean withinGradlePluginTestsClass = withinGradlePluginTestsClass(state);

        if (!withinGradlePluginTestsClass) {
            return Description.NO_MATCH;
        }

        return describeMatch(tree);
    }

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
        if (!ASTHelpers.hasAnnotation(tree, "org.junit.jupiter.api.io.TempDir", state)) {
            return Description.NO_MATCH;
        }

        if (!withinGradlePluginTestsClass(state)) {
            return Description.NO_MATCH;
        }

        return describeMatch(tree);
    }

    private static boolean withinGradlePluginTestsClass(VisitorState state) {
        return StreamSupport.stream(state.getPath().spliterator(), false)
                .anyMatch(parentTree -> ASTHelpers.hasAnnotation(
                        parentTree, "com.palantir.gradle.testing.junit.GradlePluginTests", state));
    }
}
