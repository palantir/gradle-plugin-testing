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

package com.palantir.gradle.testing.junit;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Represents a Maven module that can be published to a test repository.
 * Use the builder pattern via {@link #of(String)} to create modules with dependencies.
 */
public record Module(
        String group, String artifact, String version, ImmutableList<String> dependencies, String targetCompatibility) {
    private static final Splitter COORDINATE_SPLITTER = Splitter.on(':').trimResults();

    /**
     * Creates a module builder from a coordinate string in the format "group:artifact:version".
     */
    public static Builder of(String coordinate) {
        List<String> parts = COORDINATE_SPLITTER.splitToList(coordinate);
        Preconditions.checkArgument(
                parts.size() == 3, "Coordinate must be in format 'group:artifact:version', got: %s", coordinate);
        return new Builder(parts.get(0), parts.get(1), parts.get(2));
    }

    /**
     * Parses a coordinate string in the format "group:artifact:version" into a Module with no dependencies.
     */
    static Module parseSimple(String coordinate) {
        return of(coordinate).build();
    }

    /**
     * Parses a dependency graph string in the format "group:artifact:version -> dep1:dep2:dep3|dep4:dep5:dep6".
     */
    static Module parseWithDependencies(String graphString) {
        List<String> parts = Splitter.on("->").splitToList(graphString);
        Preconditions.checkArgument(
                parts.size() == 2, "Graph string must contain '->' separator, got: %s", graphString);

        String coordinate = parts.get(0).trim();
        String dependenciesString = parts.get(1).trim();

        Builder builder = of(coordinate);

        Splitter.on('|')
                .trimResults()
                .splitToStream(dependenciesString)
                .filter(dep -> !dep.isEmpty())
                .forEach(builder::dependsOn);

        return builder.build();
    }

    public String coordinate() {
        return group + ":" + artifact + ":" + version;
    }

    @Override
    public String toString() {
        return coordinate();
    }

    public static final class Builder {
        private final String group;
        private final String artifact;
        private final String version;
        private final ImmutableList.Builder<String> dependencies = ImmutableList.builder();
        private String targetCompatibility = "1.8";

        private Builder(String group, String artifact, String version) {
            this.group = Preconditions.checkNotNull(group, "group");
            this.artifact = Preconditions.checkNotNull(artifact, "artifact");
            this.version = Preconditions.checkNotNull(version, "version");
        }

        /**
         * Adds a dependency to this module in the format "group:artifact:version".
         */
        public Builder dependsOn(String dependency) {
            Preconditions.checkArgument(
                    COORDINATE_SPLITTER.splitToList(dependency).size() == 3,
                    "Dependency must be in format 'group:artifact:version', got: %s",
                    dependency);
            this.dependencies.add(dependency);
            return this;
        }

        /**
         * Sets the target Java compatibility version. Defaults to "1.8".
         */
        public Builder targetCompatibility(String targetCompatibility) {
            this.targetCompatibility = Preconditions.checkNotNull(targetCompatibility, "targetCompatibility");
            return this;
        }

        public Module build() {
            return new Module(group, artifact, version, dependencies.build(), targetCompatibility);
        }
    }
}
