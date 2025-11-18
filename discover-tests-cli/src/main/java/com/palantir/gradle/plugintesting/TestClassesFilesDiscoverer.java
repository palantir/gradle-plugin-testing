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

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

public abstract class TestClassesFilesDiscoverer extends Discoverer {

    @Override
    public Set<String> call() throws Exception {
        TestPlan testPlan = getTestPlan();
        return testPlan.getRoots().stream()
                .flatMap(testIdentifier -> testPlan.getChildren(testIdentifier).stream()
                        .map(TestIdentifier::getSource)
                        .flatMap(test -> test.stream()
                                .map(TestClassesFilesDiscoverer::getTestClassFromSource)
                                .map(clazz -> clazz.getName())))
                .collect(Collectors.toSet());
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
