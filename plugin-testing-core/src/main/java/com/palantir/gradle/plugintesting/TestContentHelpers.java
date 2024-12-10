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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Useful methods for writing common test content.
 */
public final class TestContentHelpers {

    private static final OpenOption[] WRITE_OPTIONS = {StandardOpenOption.CREATE, StandardOpenOption.APPEND};

    /**
     * Add version strings to the versions.props file for the given dependencies using the information from
     * TestDependencyVersions.
     */
    public static void addVersionsToPropsFile(File versionPropsFile, Collection<String> dependencies) {
        String versionsProps = dependencies.stream()
                .map(dep -> dep + " = " + TestDependencyVersions.version(dep))
                .collect(Collectors.joining("\n"));

        try {
            Files.writeString(versionPropsFile.toPath(), versionsProps, StandardCharsets.UTF_8, WRITE_OPTIONS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TestContentHelpers() {}
}
