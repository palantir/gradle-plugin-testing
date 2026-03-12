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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableSet;
import com.palantir.baseline.tasks.CheckUnusedDependenciesParentTask;
import com.palantir.gradle.plugintesting.TestDependencyVersionsTask.TestDependency;
import com.palantir.gradle.suppressibleerrorprone.SuppressibleErrorProneExtension;
import com.palantir.gradle.suppressibleerrorprone.SuppressibleErrorPronePlugin;
import com.palantir.gradle.versions.VersionsLockExtension;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin;
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata;
import org.gradle.util.GradleVersion;

public abstract class PluginTestingPlugin implements Plugin<Project> {
    /**
     * Used in tests to pick up the current version of this plugin.
     */
    static final String PLUGIN_VERSION_PROPERTY_NAME = "pluginTestingPluginVersion";

    static final List<String> CORE_MAVEN_NAMES = List.of(
            "plugin-testing-core", "configuration-cache-spec", "gradle-plugin-testing-junit", "discover-tests-cli");

    public static final Set<String> PATCHABLE_CHECKS =
            Set.of("GradleTestStringFormatting", "GradleTestPluginsBlock", "GradleTestAppendProperty");
    public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private static final String MAVEN_GROUP = "com.palantir.gradle.plugintesting";
    private static final Logger log = Logging.getLogger(PluginTestingPlugin.class);

    @Inject
    protected abstract ProviderFactory getProviderFactory();

    @Inject
    protected abstract ProjectLayout getProjectLayout();

    private static String coreMavenCoordinates(String name) {
        return MAVEN_GROUP + ":" + name;
    }

    /**
     * Applies the plugin to the given project.
     */
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaGradlePluginPlugin.class);
        project.getExtensions().create(PluginTestingExtension.EXTENSION_NAME, PluginTestingExtension.class);
        // need the SourceSetContainer extension so need to wait until java plugin is applied
        project.getPluginManager().withPlugin("java", _unused -> {
            doApply(project);
        });
    }

    @SuppressWarnings("for-rollout:TaskDependsOn")
    private void doApply(Project project) {
        PluginTestingExtension testUtilsExt = project.getExtensions().getByType(PluginTestingExtension.class);

        setupErrorprones(project);
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
                    Set<String> versionsFromConfig =
                            readGradleTestingVersionsConfigFile().getOrElse(Set.of());
                    Set<String> versionsFromExtension =
                            testUtilsExt.getGradleVersions().get();
                    Set<String> gradleTestVersions = ImmutableSet.<String>builder()
                            .addAll(versionsFromConfig)
                            .addAll(versionsFromExtension)
                            .build();
                    log.info(
                            "PluginTestingPlugin Gradle versions from gradle/gradle-test-versions.yml: {}",
                            versionsFromConfig);
                    log.info(
                            "PluginTestingPlugin Gradle versions from extensions (deprecated): {}",
                            versionsFromExtension);
                    if (gradleTestVersions.isEmpty()) {
                        gradleTestVersions = Set.of(GradleVersion.current().getVersion());
                        log.info(
                                "PluginTestingPlugin using default Gradle version (both extension and config file are"
                                        + " unset) {}",
                                gradleTestVersions);
                    }
                    String versions = String.join(",", gradleTestVersions);
                    test.systemProperty(GradleTestVersions.TEST_GRADLE_VERSIONS_SYSTEM_PROPERTY, versions);

                    // add system property for whether to use configuration-cache by default in tests
                    test.systemProperty(
                            ConfigurationCacheHelper.CONFIG_CACHE_SYSTEM_PROPERTY,
                            testUtilsExt.getConfigurationCacheEnabled().get());

                    // add system property to ignore gradle deprecations so that nebula tests don't fail
                    if (testUtilsExt.getIgnoreGradleDeprecations().get()) {
                        // from
                        // https://github.com/nebula-plugins/nebula-test/blob/main/src/main/groovy/nebula/test/IntegrationBase.groovy
                        test.systemProperty("ignoreDeprecations", "true");
                    }
                }
            });
        });

        addTestClassesDiscoveryTasks(project);

        addGradlePluginForTestingConfigurations(project);
    }

    private static void addTestClassesDiscoveryTasks(Project project) {
        project.getTasks()
                .register(
                        "discoverGradlePluginTestsWithDisabledConfigurationCache",
                        DiscoverTestClassesTask.class,
                        task -> {
                            task.getTestClassType().set("java");
                            task.getExtraArguments()
                                    .addAll(List.of(
                                            "withAnnotations",
                                            "--include",
                                            "com.palantir.gradle.testing.junit.DisabledConfigurationCache"));
                        });

        project.getTasks().register("discoverNebulaTestClassesToMigrate", DiscoverTestClassesTask.class, task -> {
            task.getTestClassType().set("groovy");
            task.getExtraArguments()
                    .addAll(List.of(
                            "subClassesOf",
                            "--include",
                            "nebula.test.IntegrationSpec,nebula.test.IntegrationTestKitSpec"));
        });

        project.getTasks().withType(DiscoverTestClassesTask.class, task -> {
            SourceSetContainer sourceSetContainer = project.getExtensions().getByType(SourceSetContainer.class);
            sourceSetContainer.all(sourceSet -> {
                if (!sourceSet.getName().equals(SourceSet.MAIN_SOURCE_SET_NAME)) {
                    task.getTestClasspath().from(sourceSet.getRuntimeClasspath());
                    task.getTestSourceFiles().from(sourceSet.getAllSource());
                }
            });
            task.getRootPath().set(project.getRootProject().getProjectDir());
        });
    }

    private static void setupErrorprones(Project project) {
        project.getPluginManager().withPlugin("net.ltgt.errorprone", _ignored -> {
            project.getPluginManager().apply(SuppressibleErrorPronePlugin.class);

            String errorProneJarCoordinate = coreMavenCoordinates("gradle-plugin-testing-error-prone") + ":"
                    + jarImplementationVersionOrTestVersion(project);

            project.getDependencies().add("errorprone", errorProneJarCoordinate);

            project.getExtensions()
                    .getByType(SuppressibleErrorProneExtension.class)
                    .getPatchChecks()
                    .addAll(PATCHABLE_CHECKS);
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
        String version = jarImplementationVersionOrTestVersion(project);

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

    private static String jarImplementationVersionOrTestVersion(Project project) {
        return Optional.ofNullable((String) project.findProperty(PLUGIN_VERSION_PROPERTY_NAME))
                .or(() -> Optional.ofNullable(
                        PluginTestingPlugin.class.getPackage().getImplementationVersion()))
                .orElseThrow(() -> new RuntimeException("PluginTestingPlugin implementation version not found"));
    }

    private static void addGradlePluginForTestingConfigurations(Project project) {
        NamedDomainObjectProvider<DependencyScopeConfiguration> gradlePluginForTesting =
                project.getConfigurations().dependencyScope("gradlePluginForTesting");

        NamedDomainObjectProvider<ResolvableConfiguration> gradlePluginForTestingResolvable =
                project.getConfigurations()
                        .resolvable(
                                "gradlePluginForTestingResolvable",
                                resolvable -> resolvable.extendsFrom(gradlePluginForTesting.get()));

        project.getTasks().named("pluginUnderTestMetadata", PluginUnderTestMetadata.class, pluginUnderTestMetadata -> {
            pluginUnderTestMetadata.getPluginClasspath().from(gradlePluginForTestingResolvable);
        });

        project.getRootProject().getPluginManager().withPlugin("com.palantir.consistent-versions", _plugin -> {
            project.getExtensions()
                    .getByType(VersionsLockExtension.class)
                    .test(scope -> scope.from(gradlePluginForTestingResolvable.getName()));
        });
    }

    private Provider<Set<String>> readGradleTestingVersionsConfigFile() {
        RegularFile testVersionsConfigPath =
                getProjectLayout().getSettingsDirectory().file("gradle/gradle-test-versions.yml");

        Provider<GradleTestVersionsConfig> config = getProviderFactory()
                .fileContents(testVersionsConfigPath)
                .getAsText()
                .map(text -> {
                    try {
                        return YAML_MAPPER.readValue(text, new TypeReference<>() {});
                    } catch (JsonProcessingException e) {
                        throw new IllegalArgumentException("Failed to parse gradle/gradle-test-versions.yml", e);
                    }
                });
        return config.map(GradleTestVersionsConfig::allVersions);
    }
}
