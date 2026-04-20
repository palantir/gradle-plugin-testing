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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.plugintesting.GradleDistributionBaseUrl;
import com.palantir.gradle.testing.RestrictedCreation;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.gradle.testkit.runner.GradleRunner;

record DefaultGradleInvoker(Path rootProjectDir, GradleVersion gradleVersion) implements GradleInvoker {
    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public DefaultGradleInvoker {}

    @Override
    public GradleInvocation with(Options options) {
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        Writer captureWriter = new OutputStreamWriter(capturedOutput, StandardCharsets.UTF_8);

        GradleRunner runner = GradleRunner.create()
                .withProjectDir(rootProjectDir.toFile())
                .withDebug(GradleInvoker.shouldRunInTestkitDebugMode())
                .forwardStdOutput(captureWriter)
                .forwardStdError(captureWriter)
                .withPluginClasspath()
                .withGradleDistribution(gradleDistributionUri())
                .withArguments(ImmutableList.<String>builder()
                        .addAll(options.args())
                        .add("--stacktrace")
                        .add("-P__TESTING=true")
                        .addAll(options.testingEnvironmentVariables().entrySet().stream()
                                .map(e -> String.format("-P__TESTING_%s=%s", e.getKey(), e.getValue()))
                                .toList())
                        .build());
        options.customGradleUserHome().ifPresent(gradleUserHome -> runner.withTestKitDir(gradleUserHome.toFile()));

        String title = buildTitle(options.args());
        return new DefaultGradleInvocation(runner, title, capturedOutput);
    }

    private URI gradleDistributionUri() {
        String baseUrl = Optional.ofNullable(
                        System.getProperty(GradleDistributionBaseUrl.GRADLE_DISTRIBUTION_BASE_URL_SYSTEM_PROPERTY))
                .orElse(GradleDistributionBaseUrl.DEFAULT_BASE_URL);
        return URI.create(baseUrl + "/gradle-" + gradleVersion.version() + "-bin.zip");
    }

    private String buildTitle(List<String> args) {
        StringBuilder title = new StringBuilder();
        title.append("gradle ");
        title.append(String.join(" ", args));
        title.append(" [Gradle ").append(gradleVersion.version()).append("]");
        return title.toString();
    }
}
