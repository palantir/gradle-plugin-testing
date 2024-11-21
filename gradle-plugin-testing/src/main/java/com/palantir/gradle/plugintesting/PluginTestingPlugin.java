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

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;

public class PluginTestingPlugin implements Plugin<Project> {
    /**
     * Used in tests to pick up the current version of this plugin.
     */
    static final String PLUGIN_VERSION_PROPERTY_NAME = "pluginTestingPluginVersion";

    /**
     * Applies the plugin to the given project.
     */
    @Override
    public void apply(Project project) {
        PluginTestingExtension testUtilsExt =
                project.getExtensions().create(PluginTestingExtension.EXTENSION_NAME, PluginTestingExtension.class);

        addTestDependency(project);

        SourceSetContainer sourceSetContainer = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet sourceSet = sourceSetContainer.getByName(SourceSet.TEST_SOURCE_SET_NAME);
        NamedDomainObjectProvider<Configuration> testRuntimeConfig =
                project.getConfigurations().named(sourceSet.getRuntimeClasspathConfigurationName());

        // need to use the afterEvalute hook so that any custom settings on the extension are applied before reading
        // the values in the test configuration blocks.
        project.afterEvaluate(p -> configureTestTasks(p, testUtilsExt, testRuntimeConfig));
    }

    private void configureTestTasks(
            Project project,
            PluginTestingExtension testUtilsExt,
            NamedDomainObjectProvider<Configuration> testRuntimeConfig) {

        project.getTasks().withType(Test.class).configureEach(test -> {
            // add system property for all test dependencies so that TestDepVersions can resolve them
            Set<String> depSet = getDependencyStrings(testRuntimeConfig.get());
            String depsString = String.join(",", depSet);
            test.systemProperty(TestDepVersions.TEST_DEPENDENCIES_SYSTEM_PROPERTY, depsString);

            // add system property for what versions of gradle should be used in tests
            String versions = String.join(",", testUtilsExt.getGradleVersions().get());
            test.systemProperty(GradleTestVersions.TEST_GRADLE_VERSIONS_SYSTEM_PROPERTY, versions);

            // add system property to ignore gradle deprecations so that nebula tests don't fail
            if (testUtilsExt.getIgnoreGradleDeprecations().get()) {
                // from
                // https://github.com/nebula-plugins/nebula-test/blob/main/src/main/groovy/nebula/test/IntegrationBase.groovy
                test.systemProperty("ignoreDeprecations", "true");
            }
        });
    }

    /**
     * Add test dependency on this plugin to the project so can access utility methods from code if needed.  This is
     * normally done by getting the Implementation_Version setting in the compiled jar, but that doesn't always work
     * when running tests so we can also look it up via a gradle property that tests set.
     */
    private static void addTestDependency(Project project) {
        SourceSetContainer sourceSetContainer = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet testSourceSet = sourceSetContainer.getByName(SourceSet.TEST_SOURCE_SET_NAME);
        String version = Optional.ofNullable((String) project.findProperty(PLUGIN_VERSION_PROPERTY_NAME))
                .or(() -> Optional.ofNullable(
                        PluginTestingPlugin.class.getPackage().getImplementationVersion()))
                .orElseThrow(() -> new RuntimeException("PluginTestingPlugin implementation version not found"));

        String testImplConfigName = testSourceSet.getImplementationConfigurationName();
        project.getConfigurations().named(testImplConfigName).configure(conf -> {
            conf.getDependencies()
                    .add(project.getDependencies()
                            .create("com.palantir.gradle.plugintesting:plugin-testing-core:" + version));
        });
    }

    private static Set<String> getDependencyStrings(Configuration config) {
        return config.getAllDependencies().stream()
                .filter(ModuleDependency.class::isInstance)
                .map(dep -> dep.getGroup() + ":" + dep.getName() + ":" + dep.getVersion())
                .collect(Collectors.toSet());
    }
}
