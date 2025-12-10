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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentHashMap;

public class RealTimeThreadAwareOutputStream extends OutputStream {
    private final PrintStream original;
    private final ConcurrentHashMap<Thread, TestContext> threadContexts;
    private final boolean isStderr;

    public RealTimeThreadAwareOutputStream(PrintStream original, boolean isStderr) {
        this.original = original;
        this.threadContexts = new ConcurrentHashMap<>();
        this.isStderr = isStderr;
    }

    public void registerThread(Thread thread, String testName) {
        threadContexts.put(thread, new TestContext(testName));
    }

    public void clearThread(Thread thread) {
        threadContexts.remove(thread);
    }

    @Override
    public void write(int b) throws IOException {
        Thread current = Thread.currentThread();
        TestContext context = threadContexts.get(current);

        if (context != null) {
            context.buffer.write(b);

            // Flush on newline
            if (b == '\n') {
                flush(context);
            }
        } else {
            original.write(b);
        }
    }

    @Override
    public void flush() throws IOException {
        Thread current = Thread.currentThread();
        TestContext context = threadContexts.get(current);

        if (context != null) {
            flush(context);
        }
        original.flush();
    }

    private void flush(TestContext context) {
        String line = context.buffer.toString();
        if (!line.isEmpty()) {
            synchronized (original) {
                String messageType = isStderr ? "testStdErr" : "testStdOut";
                original.println("##teamcity[" + messageType + " name='" +
                        escape(context.testName) + "' out='" + escape(line) + "']");
                original.flush();
            }
            context.buffer.reset();
        }
    }

    private String escape(String text) {
        return text
                .replace("|", "||")
                .replace("'", "|'")
                .replace("\n", "|n")
                .replace("\r", "|r")
                .replace("[", "|[")
                .replace("]", "|]");
    }

    private static class TestContext {
        final String testName;
        final ByteArrayOutputStream buffer;

        TestContext(String testName) {
            this.testName = testName;
            this.buffer = new ByteArrayOutputStream();
        }
    }
}
