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
import com.palantir.gradle.testing.RestrictedCreation;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.io.FileUtils;

final class ConfigurationCacheInvoker extends GradleInvoker {

    private final Path rootProjectDir;
    private final GradleInvoker delegate;

    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    ConfigurationCacheInvoker(Path rootProjectDir, GradleInvoker delegate) {
        this.rootProjectDir = rootProjectDir;
        this.delegate = delegate;
    }

    @Override
    public GradleInvocation with(Options options) {
        // not reusing configuration-cache among multiple gradle invocations in a test.
        cleanupConfigurationCache();

        List<String> withConfigurationCacheEnabled = ImmutableList.<String>builder()
                .addAll(options.args())
                .add("--configuration-cache")
                .build();
        GradleInvocation initialGradleInvocation = delegate.with(
                Options.from(options).args(withConfigurationCacheEnabled).build());

        GradleInvocation secondGradleInvocation = delegate.with(Options.from(options)
                .args(ImmutableList.<String>builder()
                        .addAll(withConfigurationCacheEnabled)
                        .add("--dry-run")
                        .build())
                .build());
        return new ConfigurationCacheInvocation(rootProjectDir, initialGradleInvocation, secondGradleInvocation);
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
