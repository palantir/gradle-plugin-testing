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
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class IntelliJRealTimeOutputExtension implements
        BeforeEachCallback, AfterEachCallback, BeforeAllCallback {

    private static RealTimeThreadAwareOutputStream stdoutCapture;
    private static RealTimeThreadAwareOutputStream stderrCapture;
    private static PrintStream originalOut;
    private static PrintStream originalErr;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (stdoutCapture == null) {
            originalOut = System.out;
            originalErr = System.err;

            stdoutCapture = new RealTimeThreadAwareOutputStream(originalOut, false);
            stderrCapture = new RealTimeThreadAwareOutputStream(originalErr, true);

            System.setOut(new PrintStream(stdoutCapture, true));
            System.setErr(new PrintStream(stderrCapture, true));
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Thread currentThread = Thread.currentThread();
        String testName = getTestName(context);

        stdoutCapture.registerThread(currentThread, testName);
        stderrCapture.registerThread(currentThread, testName);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        Thread currentThread = Thread.currentThread();

        // Flush any remaining output
        stdoutCapture.flush();
        stderrCapture.flush();

        stdoutCapture.clearThread(currentThread);
        stderrCapture.clearThread(currentThread);
    }

    private String getTestName(ExtensionContext context) {
        return context.getTestClass().map(Class::getName).orElse("") +
                "." + context.getTestMethod().map(m -> m.getName()).orElse("");
    }
}
