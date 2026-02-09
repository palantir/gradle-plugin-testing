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
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.Map;

@AutoService(BugChecker.class)
@BugPattern(severity = SeverityLevel.ERROR, summary = """
    Use the typed file method instead of .file() to get IDE syntax highlighting for file contents. \
    For example, use .yamlFile("config.yml") instead of .file("config.yml").\
    """)
public final class GradleTestTypedFile extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {
    private static final Matcher<ExpressionTree> DIRECTORY_FILE_METHOD = Matchers.instanceMethod()
            .onDescendantOf("com.palantir.gradle.testing.files.Directory")
            .named("file");

    private static final Map<String, String> EXTENSION_TO_METHOD = Map.of(
            ".yml", "yamlFile",
            ".yaml", "yamlFile",
            ".gradle", "gradleFile",
            ".properties", "propertiesFile");

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (!DIRECTORY_FILE_METHOD.matches(tree, state)) {
            return Description.NO_MATCH;
        }

        if (!GradlePluginTestHelpers.isWithinGradlePluginTests(tree, state)) {
            return Description.NO_MATCH;
        }

        if (tree.getArguments().size() != 1) {
            return Description.NO_MATCH;
        }

        ExpressionTree arg = tree.getArguments().get(0);

        if (!(arg instanceof LiteralTree lit) || !(lit.getValue() instanceof String filename)) {
            return Description.NO_MATCH;
        }

        String typedMethod = findTypedMethod(filename);
        if (typedMethod == null) {
            return Description.NO_MATCH;
        }

        return buildDescription(tree)
                .addFix(createFix(tree, typedMethod, state))
                .build();
    }

    private static String findTypedMethod(String filename) {
        for (Map.Entry<String, String> entry : EXTENSION_TO_METHOD.entrySet()) {
            if (filename.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static SuggestedFix createFix(MethodInvocationTree tree, String typedMethod, VisitorState state) {
        String methodSelect = state.getSourceForNode(tree.getMethodSelect());
        int lastDot = methodSelect.lastIndexOf('.');
        String newMethodSelect;
        if (lastDot >= 0) {
            newMethodSelect = methodSelect.substring(0, lastDot + 1) + typedMethod;
        } else {
            newMethodSelect = typedMethod;
        }
        String argSource = state.getSourceForNode(tree.getArguments().get(0));
        return SuggestedFix.replace(tree, newMethodSelect + "(" + argSource + ")");
    }
}
