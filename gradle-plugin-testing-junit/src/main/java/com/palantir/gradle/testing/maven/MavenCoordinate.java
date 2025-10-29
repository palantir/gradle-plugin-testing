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
 * Represents a Maven coordinate that can be published to a test repository.
 * Use the builder pattern via {@link #from(String)} to create coordinates with dependencies.
 */
@Value.Immutable
public interface MavenCoordinate {
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
     * Creates a coordinate builder from a coordinate string in the format "group:artifact:version".
     */
    static Builder from(String coordinate) {
        List<String> parts = parseCoordinate(coordinate);
        return new Builder().group(parts.get(0)).artifact(parts.get(1)).version(parts.get(2));
    }

    class Builder extends ImmutableMavenCoordinate.Builder {}

    static MavenCoordinate parse(String moduleString) {
        return moduleString.contains("->") ? parseWithDependencies(moduleString) : parseSimple(moduleString);
    }

    private static MavenCoordinate parseSimple(String coordinate) {
        return MavenCoordinate.from(coordinate.trim()).build();
    }

    private static MavenCoordinate parseWithDependencies(String graphString) {
        List<String> parts = Splitter.on("->").trimResults().omitEmptyStrings().splitToList(graphString);

        ImmutableMavenCoordinate.Builder builder = MavenCoordinate.from(parts.get(0));

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
