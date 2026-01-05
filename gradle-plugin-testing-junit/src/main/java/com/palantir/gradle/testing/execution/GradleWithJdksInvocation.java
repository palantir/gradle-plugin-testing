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

package com.palantir.gradle.testing.execution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public record GradleWithJdksInvocation(
        GradleInvocation setupInvocation,
        ProcessBuilder generateToolchainsInvocation,
        Callable<GradleInvocation> tasksInvocation)
        implements GradleInvocation {

    private static final Logger logger = Logging.getLogger(GradleWithJdksInvocation.class);

    @Override
    public InvocationResult buildsSuccessfully() {
        setupJdkAutomanagement();
        try {
            return tasksInvocation.call().buildsSuccessfully();
        } catch (Exception e) {
            throw new RuntimeException("Failed to run the gradle invoker", e);
        }
    }

    @Override
    public InvocationResult buildsWithFailure() {
        setupJdkAutomanagement();
        try {
            return tasksInvocation.call().buildsWithFailure();
        } catch (Exception e) {
            throw new RuntimeException("Failed to run the gradle invoker", e);
        }
    }

    public void setupJdkAutomanagement() {
        try {
            setupInvocation.buildsSuccessfully();
            runWithLogger(generateToolchainsInvocation);
        } catch (Exception e) {
            throw new GradleWithJdksInvocationFailure(e);
        }
    }

    public static void runWithLogger(ProcessBuilder processBuilder) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            Process process = processBuilder.start();
            CompletableFuture<Void> stdOutputFuture = CompletableFuture.runAsync(
                    () -> processStream(process.getInputStream(), logger::lifecycle), executorService);
            CompletableFuture<Void> stdErrFuture = CompletableFuture.runAsync(
                    () -> processStream(process.getErrorStream(), logger::error), executorService);
            stdOutputFuture.get();
            stdErrFuture.get();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException(String.format(
                        "Command '%s' failed with exit code %d.",
                        String.join(" ", processBuilder.command()), exitCode));
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(
                    String.format("Failed to run command '%s'. ", String.join(" ", processBuilder.command())), e);
        } finally {
            executorService.shutdown();
        }
    }

    public static void processStream(InputStream inputStream, Consumer<String> logFunction) {
        try (BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                logFunction.accept(line);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write inputStream", e);
        }
    }
}
