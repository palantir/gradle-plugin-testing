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
package com.palantir.gradle.plugintesting;

import com.palantir.baseline.tasks.CheckUnusedDependenciesParentTask;
import com.palantir.gradle.plugintesting.TestDependencyVersionsTask.TestDependency;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;

public class PluginTestingPlugin implements Plugin<Project> {
    /**
     * Used in tests to pick up the current version of this plugin.
     */
    static final String PLUGIN_VERSION_PROPERTY_NAME = "pluginTestingPluginVersion";

    static final List<String> CORE_MAVEN_NAMES = List.of("plugin-testing-core", "configuration-cache-spec");

    private static final String MAVEN_GROUP = "com.palantir.gradle.plugintesting";

    private static String coreMavenCoordinates(String name) {
        return MAVEN_GROUP + ":" + name;
    }

    /**
     * Applies the plugin to the given project.
     */
    @Override
    public void apply(Project project) {
        project.getExtensions().create(PluginTestingExtension.EXTENSION_NAME, PluginTestingExtension.class);
        // need the SourceSetContainer extension so need to wait until java plugin is applied
        project.getPluginManager().withPlugin("java", _unused -> {
            doApply(project);
        });
    }

    private static void doApply(Project project) {
        PluginTestingExtension testUtilsExt = project.getExtensions().getByType(PluginTestingExtension.class);

        addTestDependency(project);

        TaskProvider<TestDependencyVersionsTask> testDependencyVersions = project.getTasks()
                .register("writeTestDependencyVersions", TestDependencyVersionsTask.class, task -> {
                    SourceSetContainer sourceSetContainer =
                            project.getExtensions().getByType(SourceSetContainer.class);
                    SourceSet sourceSet = sourceSetContainer.getByName(SourceSet.TEST_SOURCE_SET_NAME);
                    NamedDomainObjectProvider<Configuration> testRuntimeConfig =
                            project.getConfigurations().named(sourceSet.getRuntimeClasspathConfigurationName());
                    Provider<Iterable<TestDependency>> testRuntimeDependencies = testRuntimeConfig.map(
                            config -> config.getResolvedConfiguration().getFirstLevelModuleDependencies().stream()
                                    .map(dep -> new TestDependency(
                                            dep.getModuleGroup(), dep.getModuleName(), dep.getModuleVersion()))
                                    .collect(Collectors.toUnmodifiableSet()));
                    task.getTestRuntimeDependencies().set(testRuntimeDependencies);
                });

        Provider<String> testDependenciesFileAbsolutePath = project.provider(() ->
                testDependencyVersions.get().getOutputFile().get().getAsFile().getAbsolutePath());

        project.getTasks().withType(Test.class).configureEach(test -> {
            test.dependsOn(testDependencyVersions);

            // doFirst so any custom settings on the extension are applied before reading the values and setting
            // the system properties.

            // Do not replace with a lambda! Lambdas aren't build cacheable.
            // https://github.com/gradle/gradle/issues/5510
            test.doFirst(new Action<>() {
                @Override
                public void execute(Task _task) {
                    // add system property for name of file to read for dependency versions
                    test.systemProperty(
                            TestDependencyVersions.TEST_DEPENDENCIES_FILE_SYSTEM_PROPERTY,
                            testDependenciesFileAbsolutePath.get());

                    // add system property for what versions of gradle should be used in tests
                    String versions =
                            String.join(",", testUtilsExt.getGradleVersions().get());
                    test.systemProperty(GradleTestVersions.TEST_GRADLE_VERSIONS_SYSTEM_PROPERTY, versions);

                    // add system property to ignore gradle deprecations so that nebula tests don't fail
                    if (testUtilsExt.getIgnoreGradleDeprecations().get()) {
                        // from
                        // https://github.com/nebula-plugins/nebula-test/blob/main/src/main/groovy/nebula/test/IntegrationBase.groovy
                        test.systemProperty("ignoreDeprecations", "true");
                    }
                }
            });
        });
    }

    /**
     * Add test dependency on the utility jars to the project so that an explicit dependency statement isn't needed.
     * This is done by getting the Implementation-Version metainfo from the compiled jar when this plugin is used
     * for real, but that doesn't work when running tests in this repo, so we can also look it up via a gradle property
     * that tests set.
     *
     * This also adds the dependency to the checkUnusedDependencies ignore list if the baseline plugin is applied so
     * those tasks do not fail if nothing uses the dependency (yet).  This is somewhat common since an automated tool
     * (like excavator) could apply this plugin to a repo before a human attempts to use the utility methods.
     *
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
            CORE_MAVEN_NAMES.forEach(name -> conf.getDependencies()
                    .add(project.getDependencies().create(coreMavenCoordinates(name) + ":" + version)));
        });

        // add to ignore list for CheckUnusedDependencies
        project.getPluginManager().withPlugin("com.palantir.baseline-exact-dependencies", _unused -> {
            project.getTasks().withType(CheckUnusedDependenciesParentTask.class).configureEach(task -> {
                CORE_MAVEN_NAMES.forEach(name -> task.ignore(MAVEN_GROUP, name));
            });
        });
    }
}
