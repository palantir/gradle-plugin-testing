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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

@CacheableTask
public abstract class TestDependencyVersionsTask extends DefaultTask {

    public TestDependencyVersionsTask() {
        getOutputFile()
                .convention(getProject()
                        .getLayout()
                        .getBuildDirectory()
                        .file("plugin-testing/dependency-versions.properties"));
    }

    @Classpath
    abstract Property<Configuration> getClasspathConfiguration();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @TaskAction
    public final void doAction() {
        List<String> depSet = getDependencyStrings(getClasspathConfiguration().get());
        String depsString = String.join("\n", depSet);
        try {
            Files.write(getOutputFile().get().getAsFile().toPath(), depsString.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a list of all dependencies, sorted and deduplicated.
     */
    private static List<String> getDependencyStrings(Configuration config) {
        // return config.getIncoming().getDependencies().stream()
        return config.getResolvedConfiguration().getFirstLevelModuleDependencies().stream()
                .map(dep -> dep.getModuleGroup() + ":" + dep.getModuleName() + "=" + dep.getModuleVersion())
                .sorted()
                .distinct()
                .collect(Collectors.toList());
    }
}
