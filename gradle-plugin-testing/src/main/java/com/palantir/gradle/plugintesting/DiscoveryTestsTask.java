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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.OutputFile;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spockframework.runtime.SpockEngine;

public abstract class DiscoveryTestsTask extends JavaExec {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryTestsTask.class);

    @InputFiles
    public abstract ConfigurableFileCollection getTestClasspath();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @Inject
    protected abstract ProjectLayout getProjectLayout();

    private static final JupiterTestEngine JUPITER_TEST_ENGINE = new JupiterTestEngine();
    private static final SpockEngine SPOCK_ENGINE = new SpockEngine();

    private static final LauncherConfig LAUNCHER_CONFIG = LauncherConfig.builder()
            .enableTestEngineAutoRegistration(false)
            .addTestEngines(JUPITER_TEST_ENGINE, SPOCK_ENGINE)
            .build();

    public DiscoveryTestsTask() {
        setClasspath(getTestClasspath());
        getOutputFile().convention(getProjectLayout().getBuildDirectory().file("nebula-tests.txt"));
    }

    @Override
    public final void exec() {
        List<String> testClasses = new ArrayList<>();
        log.info("Retrieved {} {}", getTestClasspath().getFiles(), System.getProperty("java.class.path"));

        LauncherDiscoveryRequest discoveryRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClasspathRoots(
                        getTestClasspath().getFiles().stream().map(File::toPath).collect(Collectors.toSet())))
                .build();

        try (LauncherSession launcherSession = LauncherFactory.openSession(LAUNCHER_CONFIG)) {
            Launcher launcher = launcherSession.getLauncher();
            TestPlan testDescriptor = launcher.discover(discoveryRequest);
            log.info("testDescriptor {}", testDescriptor.getRoots());
            testDescriptor
                    .getRoots()
                    .forEach(testIdentifier -> log.info("Found {}", testDescriptor.getChildren(testIdentifier)));
        }

        // TODO: whole hierarchy
        /*testDescriptor.get().forEach(rootId -> {
            testPlan.getChildren(rootId).forEach(testId -> {
                String className = testId.getDisplayName();
                log.info("Classname = {}", className);
                // if (isNebulaIntegrationTest(className, classLoader)) {
                //    testClasses.add(className);
                //    getLogger().info("Discovered Nebula test: {}", className);
                // }
            });
        });*/

        try {
            Files.writeString(
                    getOutputFile().get().getAsFile().toPath(), String.join(System.lineSeparator(), testClasses));
            getLogger().lifecycle("Discovered {} Nebula tests", testClasses.size());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private URLClassLoader createClassLoader(Set<File> classpath) {
        List<URL> urls = new ArrayList<>();
        for (File file : classpath) {
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                getLogger().warn("Could not convert file to URL: {}", file, e);
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
    }

    private boolean isNebulaIntegrationTest(String className, ClassLoader classLoader) {
        try {
            Class<?> clazz = Class.forName(className, false, classLoader);
            Class<?> superClass = clazz.getSuperclass();
            if (superClass == null) {
                return false;
            }
            String superClassName = superClass.getName();
            return "nebula.test.IntegrationSpec".equals(superClassName)
                    || "nebula.test.IntegrationTestKitSpec".equals(superClassName);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }
}
