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

import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;

public final class DiscoverTestsMain {

    public enum Type {
        GROOVY_TEST_CLASSES_TO_MIGRATE("groovyTestClassesToMigrate"),
        GRADLE_PLUGIN_TEST_CLASSES("gradlePluginTestClasses");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }

        public static Type fromLabel(String label) {
            for (Type e : values()) {
                if (e.label.equals(label)) {
                    return e;
                }
            }
            throw new RuntimeException(String.format("Cannot convert %s to a Type", label));
        }
    }

    private static final Logger log = Logger.getLogger("DiscoverTestsMain");

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected one argument: " + Arrays.toString(Type.values()));
        }
        printOutputs(
                switch (Type.fromLabel(args[0])) {
                    case GRADLE_PLUGIN_TEST_CLASSES -> new DiscoverGradlePluginTestingTestClasses().call();
                    case GROOVY_TEST_CLASSES_TO_MIGRATE -> new DiscoverGroovyTestClassesToMigrate().call();
                });
        System.exit(0);
    }

    private static void printOutputs(Set<String> classes) {
        classes.forEach(System.out::println);
        System.out.flush();
    }
}
