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

package com.palantir.gradle.testing.junit;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Test;

@DisabledConfigurationCache
@GradlePluginTests
public class DisabledConfigurationCacheTestBefore {

    @Test
    void configuration_cache_is_disabled_by_default(GradleInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().plugins().add("java");
        ConfigurationCacheTests.setUpConfigurationCacheTask(rootProject);

        InvocationResult result = invoker.withArgs("checkConfigurationCache").buildsSuccessfully();
        result.assertThat().output().contains("isConfigurationCacheRequested=false");
        result.assertThat().task(":checkConfigurationCache").outcome().succeeded();
    }
}
