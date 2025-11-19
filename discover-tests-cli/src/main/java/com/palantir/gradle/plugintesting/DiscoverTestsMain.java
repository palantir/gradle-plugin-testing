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

import java.nio.file.Path;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "discover", subcommands = TestClassesCommand.class)
public final class DiscoverTestsMain {

    @Option(names = "--output", description = "Output")
    private String output;

    Path getOutputPath() {
        return Path.of(output);
    }

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new DiscoverTestsMain()).execute(args);
        System.exit(exitCode);
    }
}
