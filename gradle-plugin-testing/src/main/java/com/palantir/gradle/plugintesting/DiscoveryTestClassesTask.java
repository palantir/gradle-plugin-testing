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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.options.Option;
import org.gradle.internal.io.StreamByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DiscoveryTestClassesTask extends JavaExec {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryTestClassesTask.class);

    @InputFiles
    public abstract ConfigurableFileCollection getTestClasspath();

    @InputFiles
    public abstract ConfigurableFileCollection getTestSourceFiles();

    @InputFiles
    public abstract ConfigurableFileCollection getRuntimeClasspath();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @Inject
    public abstract ProjectLayout getProjectLayout();

    @Inject
    public abstract ProviderFactory getProviderFactory();

    @Input
    public abstract Property<File> getParentPath();

    @Input
    @Option(option = "testClassType", description = "Sets the testClassType to either java or groovy")
    public abstract Property<String> getTestClassType();

    public DiscoveryTestClassesTask() {
        setClasspath(getProject().files(getRuntimeClasspath(), getTestClasspath()));
        getOutputFile()
                .convention(getProviderFactory()
                        .zip(
                                getTestClassType(),
                                getProjectLayout().getBuildDirectory(),
                                (type, buildDir) -> buildDir.file(String.format("project-%s-tests", type))));
        getMainClass().set("com.palantir.gradle.plugintesting.DiscoverTestsMain");
        getArgumentProviders().add(this::getArguments);

        StreamByteBuffer buffer = new StreamByteBuffer();
        setStandardOutput(buffer.getOutputStream());

        doLast(new Action<Task>() {
            @Override
            public void execute(Task _task) {
                try (BufferedReader input =
                        new BufferedReader(new InputStreamReader(buffer.getInputStream(), StandardCharsets.UTF_8))) {
                    List<String> lines = input.lines().toList();
                    Set<String> groovyTestClasses = lines.stream()
                            .map(line -> line.replace('.', '/') + "."
                                    + getTestClassType().get())
                            .collect(Collectors.toSet());

                    Set<String> testClasses = getTestSourceFiles().getAsFileTree().getFiles().stream()
                            .map(File::toPath)
                            .filter(path -> pathMatches(path, groovyTestClasses))
                            .map(path -> getParentPath()
                                    .get()
                                    .toPath()
                                    .relativize(path)
                                    .toString())
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

    private List<String> getArguments() {
        String classType = getTestClassType().get();
        if (classType.equals("java")) {
            return List.of("gradlePluginTestClasses");
        } else if (classType.equals("groovy")) {
            return List.of("groovyTestClassesToMigrate");
        }
        throw new IllegalArgumentException(String.format("Unexpected argumentType %s", classType));
    }

    private boolean pathMatches(Path path, Set<String> groovyTestClasses) {
        log.info("path matches {} {}", path, groovyTestClasses);
        return groovyTestClasses.stream().anyMatch(path::endsWith);
    }
}
