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
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

@AutoService(BugChecker.class)
@BugPattern(
        severity = SeverityLevel.ERROR,
        summary = "Use `setProperty` instead of the deprecated `appendProperty` method on `PropertiesFile`")
public final class GradleTestAppendProperty extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {

    private static final Matcher<ExpressionTree> APPEND_PROPERTY = Matchers.instanceMethod()
            .onExactClass("com.palantir.gradle.testing.files.properties.PropertiesFile")
            .named("appendProperty");

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (!APPEND_PROPERTY.matches(tree, state)) {
            return Description.NO_MATCH;
        }

        return describeMatch(tree, SuggestedFixes.renameMethodInvocation(tree, "setProperty", state));
    }
}
