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

import com.palantir.gradle.testing.project.RootProject;
import java.util.Optional;

/**
 * Base test class that provides helper methods for testing plugins.
 * This class provides a method to set up the plugin version property that is used by tests.
 *
 * <p>In the old Nebula-based testing framework, this class would override runTasks to automatically
 * inject the plugin version property as a command-line argument. In the new framework, we instead
 * add the property to gradle.properties, which is cleaner and more maintainable.
 *
 * <p>Tests that extend this class should call {@link #setupPluginVersion(RootProject)} in their
 * {@code @BeforeEach} method to configure the project with the plugin version.
 */
public abstract class AbstractTestingPluginTest {

    /**
     * Sets up the plugin version property in gradle.properties.
     * This replaces the old pattern of overriding runTasks to inject the property as a command-line argument.
     *
     * <p>In the old Nebula framework, AbstractTestingPluginSpec would override runTasks like this:
     * <pre>{@code
     * @Override
     * ExecutionResult runTasks(String... tasks) {
     *     def projectVersion = Optional.ofNullable(System.getProperty('projectVersion')).orElseThrow()
     *     String[] strings = tasks + ["-P${PluginTestingPlugin.PLUGIN_VERSION_PROPERTY_NAME}=${projectVersion}"]
     *     return super.runTasks(strings)
     * }
     * }</pre>
     *
     * <p>In the new framework, we add the property to gradle.properties instead:
     * <pre>{@code
     * @BeforeEach
     * void beforeEach(RootProject rootProject) {
     *     setupPluginVersion(rootProject);
     *     // ... other setup
     * }
     * }</pre>
     *
     * @param rootProject the root project to configure
     */
    protected void setupPluginVersion(RootProject rootProject) {
        String projectVersion =
                Optional.ofNullable(System.getProperty("projectVersion")).orElseThrow();
        rootProject
                .gradlePropertiesFile()
                .appendProperty(PluginTestingPlugin.PLUGIN_VERSION_PROPERTY_NAME, projectVersion);
    }
}
