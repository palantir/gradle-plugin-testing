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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableSortedSet;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableGradleTestVersionsConfig.class)
@JsonDeserialize(as = ImmutableGradleTestVersionsConfig.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface GradleTestVersionsConfig {
    @JsonProperty("major-versions")
    @Value.NaturalOrder
    SortedMap<Integer, String> majorVersions();

    @JsonProperty("extra-versions")
    @Value.NaturalOrder
    SortedSet<String> extraVersions();

    final class Builder extends ImmutableGradleTestVersionsConfig.Builder {}

    static Builder builder() {
        return new Builder();
    }

    default GradleTestVersionsConfig withoutMajorVersion(int majorVersion) {
        SortedMap<Integer, String> newMajorVersions = EntryStream.of(majorVersions())
                .filterKeys(maj -> maj != majorVersion)
                .toSortedMap();
        SortedSet<String> newExtraVersions = StreamEx.of(extraVersions())
                .filter(version -> getMajorVersion(version) != majorVersion)
                .toCollection(TreeSet::new);
        return builder()
                .from(this)
                .majorVersions(newMajorVersions)
                .extraVersions(newExtraVersions)
                .build();
    }

    default SortedSet<String> allVersions() {
        return ImmutableSortedSet.<String>naturalOrder()
                .addAll(majorVersions().values())
                .addAll(extraVersions())
                .build();
    }

    private static int getMajorVersion(String newMajorVersion) {
        int dotIndex = newMajorVersion.indexOf('.');
        String majorVersionStr = dotIndex == -1 ? newMajorVersion : newMajorVersion.substring(0, dotIndex);
        return Integer.parseInt(majorVersionStr);
    }
}
