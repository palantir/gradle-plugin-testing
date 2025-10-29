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

package com.palantir.gradle.testing.execution;

import java.util.Map;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.gradle.testkit.runner.UnexpectedBuildSuccess;

public final class DefaultGradleInvocation implements GradleInvocation {

    private final GradleRunner gradleRunner;

    public DefaultGradleInvocation(GradleRunner gradleRunner) {
        this.gradleRunner = gradleRunner;
    }

    @Override
    public DefaultGradleInvocation withEnvironment(Map<String, String> environment) {
        gradleRunner.withEnvironment(environment);
        return this;
    }

    @Override
    public InvocationResult buildsSuccessfully() {
        try {
            return new InvocationResult(gradleRunner.build());
        } catch (UnexpectedBuildFailure unexpectedBuildFailure) {
            throw new UnexpectedInvocationFailure(unexpectedBuildFailure);
        }
    }

    @Override
    public InvocationResult buildsWithFailure() {
        try {
            return new InvocationResult(gradleRunner.buildAndFail());
        } catch (UnexpectedBuildSuccess unexpectedBuildSuccess) {
            throw new UnexpectedInvocationSuccess(unexpectedBuildSuccess);
        }
    }
}
