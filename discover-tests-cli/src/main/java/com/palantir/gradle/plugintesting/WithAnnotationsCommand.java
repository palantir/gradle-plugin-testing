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

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.launcher.PostDiscoveryFilter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "withAnnotations", description = "Test classes options")
public final class WithAnnotationsCommand extends TestClassesDiscoverer {

    @Option(names = "--include", split = ",", description = "List of testClasses with annotations to include")
    private List<String> includedAnnotations;

    @Override
    protected Filter<?> getFilter() {
        List<? extends Class<? extends Annotation>> annotationClasses = includedAnnotations.stream()
                .map(WithAnnotationsCommand::getClassAnnotation)
                .mapMulti(Optional<Class<? extends Annotation>>::ifPresent)
                .toList();
        return new PostDiscoveryFilter() {
            @Override
            public FilterResult apply(TestDescriptor testDescriptor) {
                return testDescriptor
                        .getSource()
                        .map(TestClassesDiscoverer::getTestClassFromSource)
                        .map(testClass -> {
                            if (hasAnyClassAnnotations(testClass, annotationClasses)) {
                                return FilterResult.included(
                                        String.format("%s has an allowlisted annotation", testDescriptor));
                            }
                            return FilterResult.excluded(
                                    String.format("%s does not have an allowlisted annotation", testDescriptor));
                        })
                        .orElseGet(() -> FilterResult.excluded("Not a class source"));
            }
        };
    }

    private static boolean hasAnyClassAnnotations(
            Class<?> clazz, List<? extends Class<? extends Annotation>> annotations) {
        return annotations.stream().anyMatch(annotation -> AnnotationSupport.isAnnotated(clazz, annotation));
    }

    @SuppressWarnings("BanSystemOut")
    private static Optional<Class<? extends Annotation>> getClassAnnotation(String annotationName) {
        try {
            return Optional.of((Class<? extends java.lang.annotation.Annotation>) Class.forName(annotationName));
        } catch (ClassNotFoundException e) {
            System.out.format(
                    "Failed to retrieve the annotation class from the string name: %s, Skipping", annotationName);
            return Optional.empty();
        }
    }
}
