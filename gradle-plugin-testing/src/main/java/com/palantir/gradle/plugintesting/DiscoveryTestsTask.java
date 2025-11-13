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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.OutputFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DiscoveryTestsTask extends JavaExec {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryTestsTask.class);

    @InputFiles
    public abstract ConfigurableFileCollection getTestClasspath();

    @InputFiles
    public abstract ConfigurableFileCollection getTestSourceFiles();

    @InputFiles
    public abstract ConfigurableFileCollection getRuntimeClasspath();

    @OutputFile
    public abstract RegularFileProperty getDiscoveredTestsFile();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @Inject
    public abstract ProjectLayout getProjectLayout();

    public DiscoveryTestsTask() {
        setClasspath(getProject().files(getRuntimeClasspath(), getTestClasspath()));
        getDiscoveredTestsFile()
                .convention(getProjectLayout().getBuildDirectory().file("discovered-groovy-tests"));
        getOutputFile().convention(getProjectLayout().getBuildDirectory().file("project-groovy-tests"));
        getMainClass().set("com.palantir.gradle.plugintesting.GroovyTestClassesDiscoveryRunner");
        getArgumentProviders().add(this::getArguments);

        doLast(new Action<Task>() {
            @Override
            public void execute(Task _task) {
                try {
                    Set<String> groovyTestClasses =
                            Files.readAllLines(getDiscoveredTestsFile()
                                            .getAsFile()
                                            .get()
                                            .toPath())
                                    .stream()
                                    .map(testClass -> testClass.replace(".", "/") + ".groovy")
                                    .collect(Collectors.toSet());
                    Set<String> testClasses = getTestSourceFiles().getAsFileTree().getFiles().stream()
                            .map(File::toPath)
                            .filter(path -> pathMatches(path, groovyTestClasses))
                            .map(Path::toString)
                            .collect(Collectors.toSet());
                    if (testClasses.size() != groovyTestClasses.size()) {
                        throw new RuntimeException(
                                "Could not find all test source classes for the discovered test classes");
                    }
                    Files.writeString(getOutputFile().getAsFile().get().toPath(), String.join("\n", testClasses));
                } catch (IOException e) {
                    throw new UncheckedIOException("failed to compute class files", e);
                }
            }
        });
    }

    private boolean pathMatches(Path path, Set<String> groovyTestClasses) {
        return groovyTestClasses.stream().anyMatch(path::endsWith);
    }

    private List<String> getArguments() {
        return List.of(getDiscoveredTestsFile().get().getAsFile().toPath().toString());
    }
}
