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

package com.palantir.gradle.testing.junit.output;

import java.io.PrintStream;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ThreadAwareOutputExtension
        implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(ThreadAwareOutputExtension.class);

    private static ThreadAwareOutputStream stdoutCapture;
    private static ThreadAwareOutputStream stderrCapture;
    private static PrintStream originalOut;
    private static PrintStream originalErr;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (stdoutCapture == null) {
            originalOut = System.out;
            originalErr = System.err;

            stdoutCapture = new ThreadAwareOutputStream(originalOut);
            stderrCapture = new ThreadAwareOutputStream(originalErr);

            System.setOut(new PrintStream(stdoutCapture, true));
            System.setErr(new PrintStream(stderrCapture, true));
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Thread currentThread = Thread.currentThread();
        stdoutCapture.registerThread(currentThread);
        stderrCapture.registerThread(currentThread);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        Thread currentThread = Thread.currentThread();

        String stdout = stdoutCapture.getOutput(currentThread);
        String stderr = stderrCapture.getOutput(currentThread);

        // Store in context for potential later retrieval
        context.getStore(NAMESPACE).put("stdout", stdout);
        context.getStore(NAMESPACE).put("stderr", stderr);

        // Print with clear test association
        synchronized (originalOut) {
            if (!stdout.isEmpty()) {
                originalOut.println("\n╔══ " + context.getDisplayName() + " [STDOUT] ══");
                originalOut.print(stdout);
                if (!stdout.endsWith("\n")) originalOut.println();
                originalOut.println("╚" + "═".repeat(context.getDisplayName().length() + 20));
            }
        }

        synchronized (originalErr) {
            if (!stderr.isEmpty()) {
                originalErr.println("\n╔══ " + context.getDisplayName() + " [STDERR] ══");
                originalErr.print(stderr);
                if (!stderr.endsWith("\n")) originalErr.println();
                originalErr.println("╚" + "═".repeat(context.getDisplayName().length() + 20));
            }
        }

        stdoutCapture.clearThread(currentThread);
        stderrCapture.clearThread(currentThread);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        // Optional: restore original streams
        // System.setOut(originalOut);
        // System.setErr(originalErr);
    }
}
