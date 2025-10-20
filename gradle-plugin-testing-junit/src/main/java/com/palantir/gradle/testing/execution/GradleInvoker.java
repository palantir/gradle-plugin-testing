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

import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import java.nio.file.Path;
import org.gradle.testkit.runner.GradleRunner;

public final class GradleInvoker {
    private final Path rootProjectDir;
    private final GradleVersion gradleVersion;

    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public GradleInvoker(Path rootProjectDir, GradleVersion gradleVersion) {
        this.rootProjectDir = rootProjectDir;
        this.gradleVersion = gradleVersion;
    }

    public GradleInvocation withArgs(String... args) {
        return new GradleInvocation(GradleRunner.create()
                .withProjectDir(rootProjectDir.toFile())
                .withDebug(true)
                .forwardOutput()
                .withGradleVersion(gradleVersion.version())
                .withPluginClasspath()
                .withArguments(args));
    }
}
