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

package com.palantir.gradle.testing.maven;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.util.List;
import org.gradle.api.JavaVersion;
import org.immutables.value.Value;

/**
 * Represents a Maven module that can be published to a test repository.
 * Use the builder pattern via {@link #of(String)} to create modules with dependencies.
 */
@Value.Immutable
public interface Module {
    Splitter COORDINATE_SPLITTER = Splitter.on(':').trimResults();

    String group();

    String artifact();

    String version();

    List<String> dependencies();

    @Value.Default
    default JavaVersion targetCompatibility() {
        return JavaVersion.VERSION_1_8;
    }

    /**
     * Creates a module builder from a coordinate string in the format "group:artifact:version".
     */
    static ImmutableModule.Builder of(String coordinate) {
        List<String> parts = parseCoordinate(coordinate);
        return ImmutableModule.builder()
                .group(parts.get(0))
                .artifact(parts.get(1))
                .version(parts.get(2));
    }

    static Module parseModule(String moduleString) {
        return moduleString.contains("->") ? parseWithDependencies(moduleString) : parseSimple(moduleString);
    }

    static Module parseSimple(String coordinate) {
        return of(coordinate).build();
    }

    static Module parseWithDependencies(String graphString) {
        List<String> parts = Splitter.on("->").splitToList(graphString);
        Preconditions.checkArgument(
                parts.size() == 2, "Graph string must contain '->' separator, got: %s", graphString);

        ImmutableModule.Builder builder = of(parts.get(0).trim());

        Splitter.on('|')
                .trimResults()
                .omitEmptyStrings()
                .splitToStream(parts.get(1))
                .forEach(dep -> builder.addDependencies(validateCoordinate(dep)));

        return builder.build();
    }

    private static List<String> parseCoordinate(String coordinate) {
        List<String> parts = COORDINATE_SPLITTER.splitToList(coordinate);
        Preconditions.checkArgument(
                parts.size() == 3, "Coordinate must be in format 'group:artifact:version', got: %s", coordinate);
        return parts;
    }

    private static String validateCoordinate(String coordinate) {
        parseCoordinate(coordinate);
        return coordinate;
    }
}
