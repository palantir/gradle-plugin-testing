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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableGradleTestVersionsConfig.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface GradleTestVersionsConfig {
    @JsonProperty("major-versions")
    @Value.Default
    default SortedMap<Integer, String> majorVersions() {
        return new TreeMap<>();
    }

    @JsonProperty("extra-versions")
    @Value.Default
    default SortedSet<String> extraVersions() {
        return new TreeSet<>();
    }

    final class Builder extends ImmutableGradleTestVersionsConfig.Builder {}

    static Builder builder() {
        return new Builder();
    }

    static GradleTestVersionsConfig deserialize(Path pathToResource) throws IOException {
        String content = Files.readString(pathToResource);
        if (content.trim().isEmpty()) {
            return builder().build();
        }

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return objectMapper.readValue(content, new TypeReference<>() {});
    }

    default GradleTestVersionsConfig withMajorVersion(String newMajorVersion) {
        int majorVersion = getMajorVersion(newMajorVersion);

        SortedMap<Integer, String> newMajorVersions = new TreeMap<>();
        newMajorVersions.putAll(this.majorVersions());
        newMajorVersions.put(majorVersion, newMajorVersion);

        return builder().from(this).majorVersions(newMajorVersions).build();
    }

    default GradleTestVersionsConfig withoutMajorVersion(int majorVersion) {
        SortedMap<Integer, String> newMajorVersions = EntryStream.of(majorVersions())
                .filterKeys(maj -> maj != majorVersion)
                .toSortedMap();
        SortedSet<String> newExtraVersions = StreamEx.of(extraVersions())
                .filter(version -> {
                    int versionMajor = getMajorVersion(version);
                    return versionMajor != majorVersion;
                })
                .toCollection(TreeSet::new);
        return builder()
                .from(this)
                .majorVersions(newMajorVersions)
                .extraVersions(newExtraVersions)
                .build();
    }

    private static int getMajorVersion(String newMajorVersion) {
        return Integer.parseInt(newMajorVersion.split("\\.")[0]);
    }
}
