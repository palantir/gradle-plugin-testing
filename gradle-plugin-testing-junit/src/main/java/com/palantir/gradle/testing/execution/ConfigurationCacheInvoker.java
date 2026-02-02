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
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;

public final class ConfigurationCacheInvoker extends GradleInvoker {

    private final Path rootProjectDir;
    private final GradleInvoker gradleInvoker;

    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public ConfigurationCacheInvoker(Path rootProjectDir, GradleInvoker gradleInvoker) {
        this.rootProjectDir = rootProjectDir;
        this.gradleInvoker = gradleInvoker;
    }

    @Override
    public GradleInvocation with(Options options) {
        // not reusing configuration-cache among multiple gradle invocations in a test.
        cleanupConfigurationCache();

        Options argsWithConfigCache =
                options.asBuilder().addArgs("--configuration-cache").build();
        GradleInvocation initialGradleInvocation = gradleInvoker.with(argsWithConfigCache);
        GradleInvocation secondGradleInvocation = gradleInvoker.with(
                argsWithConfigCache.asBuilder().addArgs("--dry-run").build());
        return new ConfigurationCacheInvocation(rootProjectDir, initialGradleInvocation, secondGradleInvocation);
    }

    public Path getRootProjectDir() {
        return this.rootProjectDir;
    }

    private void cleanupConfigurationCache() {
        File configurationCacheDirectory =
                rootProjectDir.resolve(".gradle/configuration-cache").toFile();
        try {
            FileUtils.deleteDirectory(configurationCacheDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    String.format(
                            "Failed to delete the configuration cache directory `%s`. This is the first step when"
                                    + " running GradlePluginTests with configuration cache enabled.",
                            configurationCacheDirectory),
                    e);
        }
    }
}
