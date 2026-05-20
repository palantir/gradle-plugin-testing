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
import java.util.stream.Stream;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.OutputFile;

public abstract class DiscoverTestClassesTask extends JavaExec {

    private static final Logger log = Logging.getLogger(DiscoverTestClassesTask.class);

    @InputFiles
    public abstract ConfigurableFileCollection getTestClasspath();

    @InputFiles
    public abstract ConfigurableFileCollection getTestSourceFiles();

    @Input
    public abstract Property<File> getRootPath();

    @Input
    public abstract ListProperty<String> getExtraArguments();

    @Input
    public abstract Property<String> getTestClassType();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @OutputFile
    public abstract RegularFileProperty getDiscoveredTestsFile();

    @Inject
    public abstract ProjectLayout getProjectLayout();

    @SuppressWarnings("for-rollout:MissingOverride")
    @Inject
    public abstract ProviderFactory getProviderFactory();

    @SuppressWarnings("for-rollout:MissingOverride")
    @Inject
    public abstract ObjectFactory getObjectFactory();

    public DiscoverTestClassesTask() {
        setClasspath(getObjectFactory().fileCollection().from(getTestClasspath()));
        Provider<Directory> outputRootDirectory = getProviderFactory()
                .zip(
                        getProjectLayout().getBuildDirectory(),
                        getTestClassType(),
                        (buildDirectory, testType) ->
                                buildDirectory.dir(String.format("tests-discovery/%s/%s", getName(), testType)));

        getDiscoveredTestsFile().convention(outputRootDirectory.map(outputRoot -> outputRoot.file("test-classes")));
        getOutputFile().convention(outputRootDirectory.map(outputRoot -> outputRoot.file("test-classes-paths")));
        getMainClass().set("com.palantir.gradle.plugintesting.DiscoverTestsMain");
        getArgumentProviders().add(this::getArguments);

        doLast(new Action<Task>() {
            @Override
            public void execute(Task _task) {
                try {
                    Path parentPath = getRootPath().get().toPath();
                    Path discoveredTests =
                            getDiscoveredTestsFile().getAsFile().get().toPath();
                    Set<String> discoveredTestClassesPaths = Files.readAllLines(discoveredTests).stream()
                            .map(this::toFilePath)
                            .collect(Collectors.toSet());
                    Set<String> discoveredTestFiles = getTestSourceFiles().getAsFileTree().getFiles().stream()
                            .map(File::toPath)
                            .filter(path -> pathMatches(path, discoveredTestClassesPaths))
                            .map(path -> parentPath.relativize(path).toString())
                            .collect(Collectors.toSet());
                    if (discoveredTestFiles.size() != discoveredTestClassesPaths.size()) {
                        List<String> missingClasses = discoveredTestClassesPaths.stream()
                                .filter(classPath -> !anyMatch(classPath, discoveredTestFiles))
                                .toList();
                        throw new RuntimeException(String.format(
                                "Could not find all test source classes for the discovered test classes. Discovered"
                                        + " tests: %s, but some classes could not be found in the repo: %s",
                                discoveredTestFiles, missingClasses));
                    }
                    Files.write(getOutputFile().getAsFile().get().toPath(), discoveredTestFiles);
                } catch (IOException e) {
                    throw new UncheckedIOException("failed to compute class files", e);
                }
            }

            private String toFilePath(String line) {
                return line.replace('.', '/') + "." + getTestClassType().get();
            }
        });
    }

    private List<String> getArguments() {
        return Stream.concat(
                        Stream.of(
                                "--output",
                                getDiscoveredTestsFile().getAsFile().get().getPath(),
                                "testClasses",
                                "--test-engine",
                                getTestEngine()),
                        getExtraArguments().get().stream())
                .toList();
    }

    private String getTestEngine() {
        String classType = getTestClassType().get();
        if (classType.equals("java")) {
            return "junit-jupiter";
        } else if (classType.equals("groovy")) {
            return "spock";
        }
        throw new RuntimeException(String.format("Invalid classType %s", classType));
    }

    private static boolean anyMatch(String path, Set<String> testFilePath) {
        return testFilePath.stream().anyMatch(filePath -> filePath.endsWith(path));
    }

    private static boolean pathMatches(Path path, Set<String> testClasses) {
        return testClasses.stream().anyMatch(path::endsWith);
    }
}
