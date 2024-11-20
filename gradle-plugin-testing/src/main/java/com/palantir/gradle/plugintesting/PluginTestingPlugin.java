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
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;

public class PluginTestingPlugin implements Plugin<Project> {
    /**
     * Used in tests to pick up the current version of this plugin.
     */
    static final String PLUGIN_VERSION_PROPERTY_NAME = "pluginTestingPluginVersion";
    //    private static final String CONFIGURATION_NAME = "gradlePluginTesting";

    /**
     * Applies the plugin to the given project.
     */
    @Override
    public void apply(Project project) {
        PluginTestingExtension testUtilsExt =
                project.getExtensions().create("gradleTestUtils", PluginTestingExtension.class);

        SourceSetContainer sourceSetContainer = project.getExtensions().getByType(SourceSetContainer.class);
        addTestDependency(project, sourceSetContainer);

        SourceSet sourceSet = sourceSetContainer.getByName(SourceSet.TEST_SOURCE_SET_NAME);
        NamedDomainObjectProvider<Configuration> testRuntimeConfig =
                project.getConfigurations().named(sourceSet.getRuntimeClasspathConfigurationName());

        project.getTasks().withType(Test.class).configureEach(test -> {
            // TODO - can we figure out the sourceset or configuration that feeds this specific test task?  We can get
            // the runtime classpath, but that's a file collection

            DependencySet allDependencies = testRuntimeConfig.get().getAllDependencies();
            Set<String> depVersions = allDependencies.stream()
                    .filter(ModuleDependency.class::isInstance)
                    .map(dep -> dep.getGroup() + ":" + dep.getName() + ":" + dep.getVersion())
                    .collect(Collectors.toSet());
            String deps = String.join(",", depVersions);
            test.systemProperty(TestDepVersions.TEST_DEPENDENCIES_SYSTEM_PROPERTY, deps);

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

    /**
     * Add test dependency on this plugin to the project so can access utility methods from code if needed.
     */
    private static void addTestDependency(Project project, SourceSetContainer sourceSetContainer) {
        SourceSet testSourceSet = sourceSetContainer.getByName(SourceSet.TEST_SOURCE_SET_NAME);
        String version = Optional.ofNullable((String) project.findProperty(PLUGIN_VERSION_PROPERTY_NAME))
                .or(() -> Optional.ofNullable(
                        PluginTestingPlugin.class.getPackage().getImplementationVersion()))
                .orElseThrow(() -> new RuntimeException("PluginTestingPlugin implementation version not found"));

        // add this artifact as a dependency to the project
        //            String testImplConfig =
        //                    project.getConfigurations().create(CONFIGURATION_NAME).getName();
        String testImplConfigName = testSourceSet.getImplementationConfigurationName();
        project.getConfigurations().named(testImplConfigName).configure(conf -> {
            // TODO(#xxx): do we actually need this
            //            conf.getDependencies()
            //                    .add(project.getDependencies()
            //                            .create("com.palantir.gradle.plugintesting:gradle-plugin-testing:" +
            // version));
        });
    }

    //    public static Map<String, String> getDependencyVersions(Configuration config) {
    //        DependencySet dependencies = config.getAllDependencies();
    //        return dependencies.stream()
    //                .collect(Collectors.toMap(dep -> dep.getGroup() + ":" + dep.getName(), Dependency::getVersion));
    //    }
}
