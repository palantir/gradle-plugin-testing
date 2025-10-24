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

package com.palantir.gradle.testing.files;

import com.palantir.gradle.testing.files.arbitrary.ArbitraryFile;
import com.palantir.gradle.testing.files.arbitrary.ArbitrarySrcDir;
import com.palantir.gradle.testing.files.gradle.GradleFile;
import com.palantir.gradle.testing.files.gradle.RegularGradleFile;
import com.palantir.gradle.testing.files.properties.PropertiesFile;
import com.palantir.gradle.testing.files.yaml.YamlFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public interface FileFactory {
    Path path();

    default FileFactory ensureExists() {
        try {
            Files.createDirectories(path());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to ensure directory at %s exists".formatted(path()), e);
        }
        return this;
    }

    default ArbitraryFile file(String path) {
        return new ArbitraryFile(resolvePath(path));
    }

    default ArbitrarySrcDir directory(String path) {
        return new ArbitrarySrcDir(resolvePath(path));
    }

    default GradleFile gradleFile(String path) {
        return new RegularGradleFile(resolvePath(path));
    }

    default YamlFile yamlFile(String path) {
        return new YamlFile(resolvePath(path));
    }

    default PropertiesFile propertiesFile(String path) {
        return new PropertiesFile(resolvePath(path));
    }

    private Path resolvePath(String path) {
        return path().resolve(path);
    }
}
