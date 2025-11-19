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

import com.palantir.gradle.plugintesting.DiscoverTestsMain.TestClasses;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.spockframework.runtime.SpockEngine;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(
        name = "discover",
        subcommands = {TestClasses.class})
public final class DiscoverTestsMain implements Callable<Integer> {

    @Option(names = "--output", description = "Output")
    private String output;

    Path getOutputPath() {
        return Path.of(output);
    }

    @Command(
            name = "testClasses",
            subcommands = {SubClassesOf.class, WithAnnotations.class})
    public static final class TestClasses implements Callable<Integer> {

        @ParentCommand
        private DiscoverTestsMain discoverTestsCommand;

        @Option(names = "--test-engine", description = "Test engine - spock or junit-jupiter")
        private String testEngine;

        DiscoverTestsMain getDiscoverTestsCommand() {
            return discoverTestsCommand;
        }

        TestEngine getTestEngine() {
            if (testEngine.equals("junit-jupiter")) {
                return new JupiterTestEngine();
            } else if (testEngine.equals("spock")) {
                return new SpockEngine();
            }
            throw new IllegalArgumentException(String.format(
                    "testEngine should be either `junit-jupiter` or `spock`, %s is not a supported test engine",
                    testEngine));
        }

        @Override
        public Integer call() throws Exception {
            return 0;
        }
    }

    @Command(name = "subClassesOf", description = "Test classes options")
    public static final class SubClassesOf extends TestClassesDiscoverer {

        @Option(names = "--include", split = ",", description = "List of subClasses to include")
        private List<String> subClasses;

        @Option(names = "--exclude", split = ",", description = "List of subClasses to exclude")
        private List<String> excludeSubClasses;

        @Override
        Filter<?> getFilter() {
            return new PostDiscoveryFilter() {
                @Override
                public FilterResult apply(TestDescriptor testDescriptor) {
                    return testDescriptor
                            .getSource()
                            .map(TestClassesDiscoverer::getTestClassFromSource)
                            .map(testClass -> {
                                if (isSubclassOfAny(testClass, excludeSubClasses)) {
                                    return FilterResult.excluded(
                                            String.format("%s is subclassing an ignored class", testDescriptor));
                                } else if (isSubclassOfAny(testClass, subClasses)) {
                                    return FilterResult.included(
                                            String.format("%s subclasses an allowlisted class", testDescriptor));
                                } else {
                                    return FilterResult.excluded(
                                            String.format("%s doesn't subclass an allowlisted class", testDescriptor));
                                }
                            })
                            .orElseGet(() -> FilterResult.excluded("Not a class source"));
                }
            };
        }

        static boolean isSubclassOfAny(Class<?> originalClass, List<String> targetClassNames) {
            return targetClassNames.stream().anyMatch(targetClassName -> isSubclassOf(originalClass, targetClassName));
        }

        static boolean isSubclassOf(Class<?> originalClass, String targetClassName) {
            Class<?> clazz = originalClass;
            while (clazz != null && !clazz.equals(Object.class)) {
                if (clazz.getName().equals(targetClassName)) {
                    return true;
                }
                clazz = clazz.getSuperclass();
            }
            return false;
        }
    }

    @Command(name = "withAnnotations", description = "Test classes options")
    public static final class WithAnnotations extends TestClassesDiscoverer {

        @Option(names = "--include", split = ",", description = "List of testClasses with annotations to include")
        private List<String> includedAnnotations;

        @Option(names = "--exclude", split = ",", description = "List of testClasses with annotations to exclude")
        private List<String> excludedAnnotations;

        @Override
        Filter<?> getFilter() {
            return new PostDiscoveryFilter() {
                @Override
                public FilterResult apply(TestDescriptor testDescriptor) {
                    return testDescriptor
                            .getSource()
                            .map(TestClassesDiscoverer::getTestClassFromSource)
                            .map(testClass -> {
                                if (hasAnyClassAnnotations(testClass, excludedAnnotations)) {
                                    return FilterResult.excluded(
                                            String.format("%s has an allowlisted annotation", testDescriptor));
                                } else if (hasAnyClassAnnotations(testClass, includedAnnotations)) {
                                    return FilterResult.included(
                                            String.format("%s has an excluded annotation", testDescriptor));
                                }
                                return FilterResult.excluded(
                                        String.format("%s does not have an allowlisted annotation", testDescriptor));
                            })
                            .orElseGet(() -> FilterResult.excluded("Not a class source"));
                }
            };
        }

        private static boolean hasAnyClassAnnotations(Class<?> clazz, List<String> annotations) {
            return annotations.stream().anyMatch(annotation -> hasClassAnnotation(clazz, annotation));
        }

        private static boolean hasClassAnnotation(Class<?> clazz, String annotationName) {
            try {
                Class<? extends java.lang.annotation.Annotation> annotationClass =
                        (Class<? extends java.lang.annotation.Annotation>) Class.forName(annotationName);
                return clazz.isAnnotationPresent(annotationClass);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(
                        String.format("Failed to check if the class %s has annotation %s", clazz, annotationName));
            }
        }
    }

    @Override
    public Integer call() throws Exception {
        return 0;
    }

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new DiscoverTestsMain()).execute(args);
        System.exit(exitCode);
    }
}
