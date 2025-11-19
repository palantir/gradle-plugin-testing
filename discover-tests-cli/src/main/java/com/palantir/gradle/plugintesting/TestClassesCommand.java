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

import java.util.concurrent.Callable;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.TestEngine;
import org.spockframework.runtime.SpockEngine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(
        name = "testClasses",
        subcommands = {SubClassesOfCommand.class, WithAnnotationsCommand.class})
public final class TestClassesCommand implements Callable<Integer> {

    @ParentCommand
    private DiscoverTestsMain discoverTestsCommand;

    @Option(names = "--test-engine", description = "Test engine - spock or junit-jupiter")
    private String testEngine;

    DiscoverTestsMain getDiscoverTestsCommand() {
        return discoverTestsCommand;
    }

    TestEngine getTestEngine() {
        if (testEngine.equals("junit-jupiter")) {
            return new JupiterTestEngine();
        } else if (testEngine.equals("spock")) {
            return new SpockEngine();
        }
        throw new IllegalArgumentException(String.format(
                "testEngine should be either `junit-jupiter` or `spock`, %s is not a supported test engine",
                testEngine));
    }

    @Override
    public Integer call() throws Exception {
        return 0;
    }
}
