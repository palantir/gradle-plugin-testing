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
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.launcher.PostDiscoveryFilter;

public final class DiscoverGradlePluginTestingTestClasses extends TestClassesFilesDiscoverer {

    private static final JupiterTestEngine JUPITER_TEST_ENGINE = new JupiterTestEngine();

    @Override
    List<Filter<?>> getFilters() {
        return List.of(new PostDiscoveryFilter() {
            @Override
            public FilterResult apply(TestDescriptor testDescriptor) {
                return testDescriptor
                        .getSource()
                        .map(TestClassesFilesDiscoverer::getTestClassFromSource)
                        .map(testClass -> {
                            if (hasClassAnnotation(testClass, "com.palantir.gradle.testing.junit.GradlePluginTests")) {
                                return FilterResult.included(
                                        String.format("%s has GradlePluginTests annotation", testDescriptor));
                            }
                            return FilterResult.excluded(
                                    String.format("%s does not have GradlePluginTests annotation", testDescriptor));
                        })
                        .orElseGet(() -> FilterResult.excluded("Not a class source"));
            }
        });
    }

    public static boolean hasClassAnnotation(Class<?> clazz, String annotationName) {
        try {
            Class<? extends java.lang.annotation.Annotation> annotationClass =
                    (Class<? extends java.lang.annotation.Annotation>) Class.forName(annotationName);
            return clazz.isAnnotationPresent(annotationClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    List<TestEngine> getTestEngines() {
        return List.of(JUPITER_TEST_ENGINE);
    }
}
