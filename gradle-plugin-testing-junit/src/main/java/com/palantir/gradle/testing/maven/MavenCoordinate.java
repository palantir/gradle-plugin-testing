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
import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import java.util.List;

/**
 * Represents a Maven coordinate ({@code group:artifact:version}).
 */
public record MavenCoordinate(String group, String artifact, String version) {
    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public MavenCoordinate {}

    /**
     * Parses a coordinate string in the format {@code group:artifact:version}.
     */
    public static MavenCoordinate of(String coordinate) {
        List<String> parts = parseCoordinate(coordinate);
        return new MavenCoordinate(parts.get(0), parts.get(1), parts.get(2));
    }

    private static List<String> parseCoordinate(String coordinate) {
        List<String> parts = Splitter.on(':').trimResults().splitToList(coordinate);
        Preconditions.checkArgument(
                parts.size() == 3, "Coordinate must be in format 'group:artifact:version', got: %s", coordinate);
        return parts;
    }
}
