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

import java.util.List;
import org.immutables.value.Value;

/**
 * Represents a Maven artifact that can be published to a test repository.
 * Contains a coordinate and optional dependencies.
 */
@Value.Immutable
public interface MavenArtifact {
    MavenCoordinate coordinate();

    List<MavenCoordinate> dependencies();

    /**
     * Creates a builder for constructing a Maven artifact.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an artifact from a coordinate string with no dependencies.
     */
    static MavenArtifact of(String coordinate) {
        return builder().coordinate(coordinate).build();
    }

    class Builder extends ImmutableMavenArtifact.Builder {

        /**
         * Adds a dependency from a coordinate string in the format {@code group:artifact:version}.
         */
        public Builder addDependency(String coordinate) {
            return addDependencies(MavenCoordinate.of(coordinate));
        }

        /**
         * Sets the Maven coordinate from a coordinate string in the format {@code group:artifact:version}.
         */
        public Builder coordinate(String coordinate) {
            return coordinate(MavenCoordinate.of(coordinate));
        }
    }
}
