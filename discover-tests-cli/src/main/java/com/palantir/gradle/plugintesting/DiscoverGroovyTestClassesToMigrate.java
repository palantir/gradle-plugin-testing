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
import java.util.logging.Logger;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.spockframework.runtime.SpockEngine;

public final class DiscoverGroovyTestClassesToMigrate extends TestClassesFilesDiscoverer {

    private static final Logger logger = Logger.getLogger("DiscoverGroovyTestClassesToMigrate");

    private static final SpockEngine SPOCK_ENGINE = new SpockEngine();

    @Override
    List<Filter<?>> getFilters() {
        return List.of(new PostDiscoveryFilter() {
            @Override
            public FilterResult apply(TestDescriptor testDescriptor) {
                // Check if the test class extends directly or indirectly from
                // "nebula.test.IntegrationSpec" or "nebula.test.IntegrationTestKitSpec"
                return testDescriptor
                        .getSource()
                        .map(DiscoverGroovyTestClassesToMigrate::getTestClassFromSource)
                        .map(testClass -> {
                            // Check if testClass extends IntegrationSpec or IntegrationTestKitSpec
                            if (isSubclassOf(testClass, "com.palantir.gradle.plugintesting.ConfigurationCacheSpec")) {
                                return FilterResult.excluded(
                                        String.format("%s shouldn't be migrated yet", testDescriptor));
                            } else if (isSubclassOf(testClass, "nebula.test.IntegrationSpec")
                                    || isSubclassOf(testClass, "nebula.test.IntegrationTestKitSpec")) {
                                logger.info(String.format("Included %s", testDescriptor));
                                return FilterResult.included(String.format("%s can be migrated", testDescriptor));
                            } else {
                                return FilterResult.excluded(
                                        String.format("%s shouldn't be migrated yet", testDescriptor));
                            }
                        })
                        .orElseGet(() -> FilterResult.excluded("Not a class source"));
            }
        });
    }

    @Override
    List<TestEngine> getTestEngines() {
        return List.of(SPOCK_ENGINE);
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
