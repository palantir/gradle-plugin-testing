/*
 * (c) Copyright 2024 Palantir Technologies Inc. All rights reserved.
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

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Utility class to keep versions of dependencies referenced in test files up to date with the versions declared in
 * the project.
 */
public final class TestDependencyVersions {
    static final String TEST_DEPENDENCIES_FILE_SYSTEM_PROPERTY = "TEST_DEPENDENCIES_FILE";

    private static final Supplier<Map<String, String>> versionsSupplier =
            Suppliers.memoize(TestDependencyVersions::loadVersions);

    /**
     * Returns the version of the given dependency. Throws exception if not found.
     */
    public static String version(String depName) {
        Map<String, String> versionsMap = versionsSupplier.get();
        String result = versionsMap.get(depName);
        if (result != null) {
            return result;
        }

        if (depName.contains(":")) {
            String org = depName.substring(0, depName.indexOf(':'));
            result = versionsMap.get(org);
            if (result != null) {
                return result;
            }
            throw new IllegalArgumentException("No version found for " + depName + " or " + org);
        }
        throw new IllegalArgumentException("No version found for " + depName);
    }

    /**
     * Returns a resolved dependency string for the given dependency name.
     */
    public static String resolve(String depName) {
        return depName + ":" + version(depName);
    }

    @SuppressWarnings("for-rollout:PreferSafeLoggableExceptions")
    private static Map<String, String> loadVersions() {
        String fileName = System.getProperty(TEST_DEPENDENCIES_FILE_SYSTEM_PROPERTY);
        if (fileName == null) {
            throw new IllegalStateException("No test dependencies file name found.  Use the PluginTestingPlugin to set "
                    + TEST_DEPENDENCIES_FILE_SYSTEM_PROPERTY + " system property.");
        }
        File depsFile = new File(fileName);
        if (!depsFile.exists()) {
            throw new IllegalStateException("Test dependencies file does not exist: " + depsFile);
        }

        try {
            Map<String, String> results = Files.readAllLines(depsFile.toPath()).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(dep -> dep.split("="))
                    .collect(Collectors.toMap(dep -> dep[0], dep -> dep[1]));
            return ImmutableMap.copyOf(results);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TestDependencyVersions() {}
}
