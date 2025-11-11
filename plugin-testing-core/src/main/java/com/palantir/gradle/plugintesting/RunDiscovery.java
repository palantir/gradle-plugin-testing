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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.spockframework.runtime.SpockEngine;

public class RunDiscovery {
    private static final Logger logger = Logger.getLogger("RunDiscovery");
    private static final JupiterTestEngine JUPITER_TEST_ENGINE = new JupiterTestEngine();
    private static final SpockEngine SPOCK_ENGINE = new SpockEngine();

    private static final LauncherConfig LAUNCHER_CONFIG = LauncherConfig.builder()
            .enableTestEngineAutoRegistration(false)
            .addTestEngines(JUPITER_TEST_ENGINE, SPOCK_ENGINE)
            .build();

    public static void main(String[] args) {

        String classPath = System.getProperty("java.class.path");
        Set<Path> paths = Arrays.stream(classPath.split(File.pathSeparator))
                .map(Paths::get)
                .collect(Collectors.toSet());

        LauncherDiscoveryRequest discoveryRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClasspathRoots(paths))
                .build();

        try (LauncherSession launcherSession = LauncherFactory.openSession(LAUNCHER_CONFIG)) {
            Launcher launcher = launcherSession.getLauncher();
            TestPlan testDescriptor = launcher.discover(discoveryRequest);
            logger.info(String.format("testDescriptor %s", testDescriptor.getRoots()));
            testDescriptor
                    .getRoots()
                    .forEach(testIdentifier ->
                            logger.info(String.format("Found %s", testDescriptor.getChildren(testIdentifier))));
        }
    }
}
