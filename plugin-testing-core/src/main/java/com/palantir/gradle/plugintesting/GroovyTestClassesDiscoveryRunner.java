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
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.spockframework.runtime.SpockEngine;

public class GroovyTestClassesDiscoveryRunner {
    private static final Logger logger = Logger.getLogger("GroovyTestClassesDiscoveryRunner");

    private static final SpockEngine SPOCK_ENGINE = new SpockEngine();

    private static final LauncherConfig LAUNCHER_CONFIG = LauncherConfig.builder()
            .enableTestEngineAutoRegistration(false)
            .addTestEngines(SPOCK_ENGINE)
            .build();

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new RuntimeException(String.format(
                    "Invalid number of arguments, expected 1 argument got %s arguments: %s",
                    args.length, Arrays.stream(args).toList()));
        }

        String classPath = System.getProperty("java.class.path");
        Set<Path> paths = Arrays.stream(classPath.split(File.pathSeparator))
                .map(Paths::get)
                .collect(Collectors.toSet());

        LauncherDiscoveryRequest discoveryRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClasspathRoots(paths))
                .filters(new PostDiscoveryFilter() {
                    @Override
                    public FilterResult apply(TestDescriptor testDescriptor) {
                        // Check if the test class extends directly or indirectly from
                        // "nebula.test.IntegrationSpec" or "nebula.test.IntegrationTestKitSpec"
                        return testDescriptor
                                .getSource()
                                .map(GroovyTestClassesDiscoveryRunner::getTestClassFromSource)
                                .map(testClass -> {
                                    // Check if testClass extends IntegrationSpec or IntegrationTestKitSpec
                                    if (isSubclassOf(
                                            testClass, "com.palantir.gradle.plugintesting.ConfigurationCacheSpec")) {
                                        return FilterResult.excluded(
                                                String.format("%s shouldn't be migrated yet", testDescriptor));
                                    } else if (isSubclassOf(testClass, "nebula.test.IntegrationSpec")
                                            || isSubclassOf(testClass, "nebula.test.IntegrationTestKitSpec")) {
                                        logger.info(String.format("Included %s", testDescriptor));
                                        return FilterResult.included(
                                                String.format("%s can be migrated", testDescriptor));
                                    } else {
                                        return FilterResult.excluded(
                                                String.format("%s shouldn't be migrated yet", testDescriptor));
                                    }
                                })
                                .orElseGet(() -> FilterResult.excluded("Not a class source"));
                    }
                })
                .build();

        try (LauncherSession launcherSession = LauncherFactory.openSession(LAUNCHER_CONFIG)) {
            Launcher launcher = launcherSession.getLauncher();
            TestPlan testPlan = launcher.discover(discoveryRequest);
            Set<String> testClassNames = testPlan.getRoots().stream()
                    .flatMap(testIdentifier -> testPlan.getChildren(testIdentifier).stream()
                            .map(TestIdentifier::getSource)
                            .flatMap(test -> test.stream()
                                    .map(GroovyTestClassesDiscoveryRunner::getTestClassFromSource)
                                    .map(clazz -> clazz.getName())))
                    .collect(Collectors.toSet());

            Path outputPath = Path.of(args[0]);
            try {
                Files.writeString(
                        outputPath,
                        String.join("\n", testClassNames),
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new UncheckedIOException(String.format("Failed to write to file %s", outputPath), e);
            }
        }
    }

    static boolean isSubclassOf(Class<?> clazz, String targetClassName) {
        while (clazz != null && !clazz.equals(Object.class)) {
            if (clazz.getName().equals(targetClassName)) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    static Class<?> getTestClassFromSource(TestSource testSource) {
        if (testSource instanceof ClassSource classSource) {
            return classSource.getJavaClass();
        } else if (testSource instanceof MethodSource methodSource) {
            return methodSource.getJavaClass();
        }
        return null;
    }
}
