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
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.launcher.PostDiscoveryFilter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "subClassesOf", description = "Test classes options")
public final class SubClassesOfCommand extends TestClassesDiscoverer {

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
                                        String.format("%s subclasses an excluded class", testDescriptor));
                            } else if (isSubclassOfAny(testClass, subClasses)) {
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
