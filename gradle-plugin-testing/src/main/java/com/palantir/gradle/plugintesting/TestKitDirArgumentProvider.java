/*
 * (c) Copyright 2026 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.gradle.plugintesting;

import java.util.List;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.process.CommandLineArgumentProvider;

/**
 * Points TestKit at the directory containing the given {@code gradle.properties} as its Gradle user home.
 */
final class TestKitDirArgumentProvider implements CommandLineArgumentProvider {
    private static final String TEST_KIT_DIR_SYSTEM_PROPERTY = "org.gradle.testkit.dir";

    private final Provider<RegularFile> testKitGradleProperties;

    TestKitDirArgumentProvider(Provider<RegularFile> testKitGradleProperties) {
        this.testKitGradleProperties = testKitGradleProperties;
    }

    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public Provider<RegularFile> getTestKitGradleProperties() {
        return testKitGradleProperties;
    }

    @Override
    public Iterable<String> asArguments() {
        return testKitGradleProperties
                .map(gradleProperties -> List.of("-D" + TEST_KIT_DIR_SYSTEM_PROPERTY + "="
                        + gradleProperties.getAsFile().getParent()))
                .getOrElse(List.of());
    }
}
