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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.launcher.PostDiscoveryFilter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "subClassesOf", description = "Test classes options")
public final class SubClassesOfCommand extends TestClassesDiscoverer {

    @Option(names = "--include", split = ",", description = "List of sub-class names to include", defaultValue = "")
    private List<String> classNames;

    @Option(names = "--exclude", split = ",", description = "List of sub-class names to exclude", defaultValue = "")
    private List<String> excludedClassNames;

    @Override
    protected Filter<?> getFilter() {
        List<Class<?>> excludedClassesOnClasspath = getClassesFrom(excludedClassNames);
        List<Class<?>> includedClassesOnClasspath = getClassesFrom(classNames);

        return new PostDiscoveryFilter() {
            @Override
            public FilterResult apply(TestDescriptor testDescriptor) {
                return testDescriptor
                        .getSource()
                        .map(TestClassesDiscoverer::getTestClassFromSource)
                        .map(testClass -> {
                            if (isAssignableFrom(testClass, excludedClassesOnClasspath)) {
                                return FilterResult.excluded(
                                        String.format("%s subclasses an excluded class", testDescriptor));
                            } else if (isAssignableFrom(testClass, includedClassesOnClasspath)) {
                                return FilterResult.included(
                                        String.format("%s subclasses an allowlisted class", testDescriptor));
                            } else {
                                return FilterResult.excluded(
                                        String.format("%s does not subclass an allowlisted class", testDescriptor));
                            }
                        })
                        .orElseGet(() -> FilterResult.excluded("Not a class source"));
            }
        };
    }

    static List<Class<?>> getClassesFrom(List<String> classNames) {
        return classNames.stream()
                .map(className -> {
                    try {
                        return Optional.of(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        return Optional.<Class<?>>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    static boolean isAssignableFrom(Class<?> originalClass, List<Class<?>> targetClassNames) {
        return targetClassNames.stream().anyMatch(targetClass -> targetClass.isAssignableFrom(originalClass));
    }
}
