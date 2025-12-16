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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.fusesource.jansi.AnsiConsole;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.gradle.testkit.runner.UnexpectedBuildSuccess;

public final class DefaultGradleInvocation implements GradleInvocation {

    private final GradleRunner gradleRunner;
    private final ByteArrayOutputStream capturedOutput;
    private final String outputTitle;

    public DefaultGradleInvocation(
            GradleRunner gradleRunner, String outputTitle, ByteArrayOutputStream capturedOutput) {
        this.gradleRunner = gradleRunner;
        this.outputTitle = outputTitle;
        this.capturedOutput = capturedOutput;
    }

    @Override
    public InvocationResult buildsSuccessfully() {
        try {
            BuildResult result = gradleRunner.build();
            printFormattedOutput(true);
            return new InvocationResult(result);
        } catch (UnexpectedBuildFailure e) {
            printFormattedOutput(false);
            throw e;
        }
    }

    @Override
    public InvocationResult buildsWithFailure() {
        try {
            BuildResult result = gradleRunner.buildAndFail();
            printFormattedOutput(false);
            return new InvocationResult(result);
        } catch (UnexpectedBuildSuccess e) {
            printFormattedOutput(true);
            throw e;
        }
    }

    private void printFormattedOutput(boolean success) {
        String output = capturedOutput.toString(StandardCharsets.UTF_8);
        String formatted = success
                ? OutputBoxFormatter.formatSuccess(outputTitle, output)
                : OutputBoxFormatter.formatFailure(outputTitle, output);

        AnsiConsole.out().println(formatted);
    }
}
