/*
 * (c) Copyright 2024 Palantir Technologies Inc. All rights reserved.
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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

public class PluginTestingPlugin implements Plugin<Project> {

    /**
     * Applies the plugin to the given project.
     */
    @Override
    public void apply(Project project) {
        PluginTestingExtension testUtilsExt =
                project.getExtensions().create("gradleTestUtils", PluginTestingExtension.class);

        project.getTasks().withType(Test.class).configureEach(test -> {
            // add system properties when running tests
            String versions = String.join(",", testUtilsExt.getGradleVersions().get());
            test.systemProperty(GradleTestVersions.TEST_GRADLE_VERSIONS_SYSTEM_PROPERTY, versions);

            if (testUtilsExt.getIgnoreGradleDeprecations().get()) {
                // from
                // https://github.com/nebula-plugins/nebula-test/blob/main/src/main/groovy/nebula/test/IntegrationBase.groovy
                test.systemProperty("ignoreDeprecations", "true");
            }
        });
    }
}
