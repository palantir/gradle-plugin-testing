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
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import picocli.CommandLine.ParentCommand;

public abstract class TestClassesDiscoverer implements Runnable {

    @ParentCommand
    private TestClassesCommand testClassesCommand;

    protected abstract Filter<?> getFilter();

    private Set<String> getDiscoveredClassNames() {
        String testEngine = testClassesCommand.getTestEngine();
        if (ServiceLoader.load(TestEngine.class).stream()
                .map(ServiceLoader.Provider::get)
                .noneMatch(testEngineCandidate -> testEngineCandidate.getId().equals(testEngine))) {
            return Set.of();
        }

        String classPath = System.getProperty("java.class.path");
        LauncherDiscoveryRequest discoveryRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClasspathRoots(Arrays.stream(classPath.split(File.pathSeparator))
                        .map(Paths::get)
                        .collect(Collectors.toSet())))
                .filters(getFilter(), EngineFilter.includeEngines(testEngine))
                .build();

        LauncherConfig launcherConfig =
                LauncherConfig.builder().enableTestEngineAutoRegistration(true).build();
        try (LauncherSession launcherSession = LauncherFactory.openSession(launcherConfig)) {
            Launcher launcher = launcherSession.getLauncher();
            TestPlan testPlan = launcher.discover(discoveryRequest);
            return testPlan.getRoots().stream()
                    .flatMap(testIdentifier -> testPlan.getChildren(testIdentifier).stream()
                            .map(TestIdentifier::getSource)
                            .flatMap(test -> test.stream()
                                    .map(TestClassesDiscoverer::getTestClassFromSource)
                                    .map(clazz -> clazz.getName())))
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public final void run() {
        writeOutput(testClassesCommand.getDiscoverTestsCommand().getOutputPath(), getDiscoveredClassNames());
    }

    protected static Class<?> getTestClassFromSource(TestSource testSource) {
        if (testSource instanceof ClassSource classSource) {
            return classSource.getJavaClass();
        } else if (testSource instanceof MethodSource methodSource) {
            return methodSource.getJavaClass();
        }
        throw new IllegalStateException(String.format("Unexpected testSource type %s", testSource));
    }

    private static void writeOutput(Path output, Set<String> classes) {
        try {
            Files.write(
                    output,
                    classes.stream().sorted().toList(),
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    String.format("Failed to write test discovery output to file %s", output), e);
        }
    }
}
