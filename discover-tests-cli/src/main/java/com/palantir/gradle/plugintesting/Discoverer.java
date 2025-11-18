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
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

public abstract class Discoverer implements Callable<Set<String>> {

    private final Set<Path> classPaths;

    Discoverer() {
        String classPath = System.getProperty("java.class.path");
        this.classPaths = Arrays.stream(classPath.split(File.pathSeparator))
                .map(Paths::get)
                .collect(Collectors.toSet());
    }

    final TestPlan getTestPlan() {
        LauncherDiscoveryRequest discoveryRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClasspathRoots(classPaths))
                .filters(getFilters().toArray(Filter[]::new))
                .build();

        try (LauncherSession launcherSession = LauncherFactory.openSession(getLauncherSession())) {
            Launcher launcher = launcherSession.getLauncher();
            return launcher.discover(discoveryRequest);
        }
    }

    final LauncherConfig getLauncherSession() {
        return LauncherConfig.builder()
                .enableTestEngineAutoRegistration(false)
                .addTestEngines(getTestEngines().toArray(TestEngine[]::new))
                .build();
    }

    abstract List<Filter<?>> getFilters();

    abstract List<TestEngine> getTestEngines();
}
