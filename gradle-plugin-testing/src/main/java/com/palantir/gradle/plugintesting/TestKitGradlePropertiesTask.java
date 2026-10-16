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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "Not worth caching")
public abstract class TestKitGradlePropertiesTask extends DefaultTask {
    static final String PLUGIN_PORTAL_URL_OVERRIDE_PROPERTY = "org.gradle.internal.plugins.portal.url.override";

    private static final String FILENAME = "plugin-testing/test-kit/gradle.properties";

    @Inject
    protected abstract ProjectLayout getProjectLayout();

    public TestKitGradlePropertiesTask() {
        getOutputFile().convention(getProjectLayout().getBuildDirectory().file(FILENAME));
    }

    @Input
    public abstract Property<String> getPluginPortalUrl();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @TaskAction
    public final void doAction() {
        String content = "systemProp." + PLUGIN_PORTAL_URL_OVERRIDE_PROPERTY + "="
                + getPluginPortalUrl().get() + "\n";
        try {
            Files.writeString(getOutputFile().get().getAsFile().toPath(), content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
