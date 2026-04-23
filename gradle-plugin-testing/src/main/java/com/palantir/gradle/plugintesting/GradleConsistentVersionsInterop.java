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

import com.palantir.gradle.versions.VersionsLockExtension;
import org.gradle.api.Project;

/**
 * Isolates GCV type references so {@link PluginTestingPlugin} loads without GCV on the classpath. Only call from
 * inside a {@code withPlugin("com.palantir.consistent-versions", ...)} callback.
 */
final class GradleConsistentVersionsInterop {

    static void registerTestScope(Project project, String resolvableConfigurationName) {
        project.getExtensions()
                .getByType(VersionsLockExtension.class)
                .test(scope -> scope.from(resolvableConfigurationName));
    }

    private GradleConsistentVersionsInterop() {}
}
