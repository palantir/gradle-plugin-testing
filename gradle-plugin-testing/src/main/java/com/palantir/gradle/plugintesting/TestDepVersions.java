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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class to keep versions of dependencies referenced in test files up to date with the versions declared in
 * the project.  The class looks both at versions.lock and versions.props files to resolve dependencies.
 *
 * {@code
 *    import static com.palantir.test.TestDepVersions.resolve
 *    //...within a test case...
 *      buildFile << """
 *         buildscript {
 *             repositories {
 *                 maven { url 'https://artifactory.palantir.build/artifactory/release-jar' }
 *             }
 *             dependencies {
 *                 classpath '${resolve('com.palantir.gradle.conjure:gradle-conjure')}'
 *                 classpath '${resolve('com.palantir.sls-packaging:gradle-sls-packaging')}'
 *             }
 *         }
 *
 *         dependencies {
 *             implementation '${resolve('com.palantir.conjure.java:conjure-lib')}'
 *         }
 *         """.stripIndent(true)
 *  }
 */
public final class TestDepVersions {

    private static final String PROPS_FILE = "versions.props";
    private static final Pattern PROPS_FILE_LINE = Pattern.compile("(.*?)(:\\*)?\\s*=\\s*(.+)");
    private static final String LOCK_FILE = "versions.lock";
    private static final Pattern LOCK_FILE_LINE = Pattern.compile("([^:]+):([^:]+):([^ ]+) \\(.*");

    private static Path versionsDir = getRepositoryRoot();
    private static final Supplier<Map<String, String>> versionsSupplier =
            Suppliers.memoize(TestDepVersions::loadVersions);

    /**
     * Returns the version of the given dependency.  If a specific dependency isn't found, look for a general org
     * dependency that matches.  Throws exception if neither found.
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

    /**
     * Parses the versions.lock and versions.props files build a map of dependencies for the project.  Prioritizes
     * entries from versions.lock.  The versions.props entries are useful to get anything that is not specifically
     * referenced from java source sets like the conjure plugin.
     */
    private static Map<String, String> loadVersions() {
        Map<String, String> results;
        Path lockFile = versionsDir.resolve(Paths.get(LOCK_FILE));

        try (Stream<String> lines = Files.lines(lockFile, StandardCharsets.UTF_8)) {
            results = lines.map(String::trim)
                    .map(LOCK_FILE_LINE::matcher)
                    .filter(Matcher::matches)
                    .collect(Collectors.toMap(
                            matcher -> matcher.group(1) + ":" + matcher.group(2), matcher -> matcher.group(3)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Path propsFile = versionsDir.resolve(Paths.get(PROPS_FILE));
        try (Stream<String> lines = Files.lines(propsFile, StandardCharsets.UTF_8)) {
            Map<String, String> versionsFromProps = lines.map(String::trim)
                    .map(PROPS_FILE_LINE::matcher)
                    .filter(Matcher::matches)
                    // filter out keys that are already in the map from the lock file
                    .filter(matcher -> !results.containsKey(matcher.group(1)))
                    .collect(Collectors.toMap(matcher -> matcher.group(1), matcher -> matcher.group(3)));
            results.putAll(versionsFromProps);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ImmutableMap.copyOf(results);
    }

    /**
     * Return the repository root.  Know that the versions file resides there.
     */
    static Path getRepositoryRoot() {
        Path root = Paths.get(".").toAbsolutePath().normalize();
        while (root.getParent() != null && !Files.exists(root.resolve(LOCK_FILE))) {
            root = root.getParent();
        }
        return root;
    }

    @VisibleForTesting
    static void setVersionsDir(Path versionsDirOverride) {
        versionsDir = versionsDirOverride;
    }

    private TestDepVersions() {}
}
