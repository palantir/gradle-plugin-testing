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

package com.palantir.gradle.testing.files.gradle.sections;

import com.palantir.gradle.testing.files.gradle.SettingsGradleFile;

/**
 * Represents the pluginManagement block in settings.gradle with subsections for repositories and plugins.
 */
public final class PluginManagementSection extends GenericSection<SettingsGradleFile> {
    public PluginManagementSection(SettingsGradleFile gradleFile) {
        super(gradleFile, "pluginManagement");
    }

    public GenericSection<SettingsGradleFile> repositories() {
        return new GenericSection<>(getGradleFile(), this, "repositories");
    }

    public GenericSection<SettingsGradleFile> plugins() {
        return new GenericSection<>(getGradleFile(), this, "plugins");
    }
}
