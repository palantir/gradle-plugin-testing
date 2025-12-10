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

public class ThreadAwareOutputStream extends OutputStream {
    private final PrintStream original;
    private final ConcurrentHashMap<Thread, ByteArrayOutputStream> threadBuffers;

    public ThreadAwareOutputStream(PrintStream original) {
        this.original = original;
        this.threadBuffers = new ConcurrentHashMap<>();
    }

    public void registerThread(Thread thread) {
        threadBuffers.put(thread, new ByteArrayOutputStream());
    }

    public String getOutput(Thread thread) {
        ByteArrayOutputStream buffer = threadBuffers.get(thread);
        return buffer != null ? buffer.toString() : "";
    }

    public void clearThread(Thread thread) {
        threadBuffers.remove(thread);
    }

    @Override
    public void write(int b) throws IOException {
        Thread current = Thread.currentThread();
        ByteArrayOutputStream buffer = threadBuffers.get(current);

        if (buffer != null) {
            buffer.write(b);
        } else {
            // Fallback to original for non-test threads
            original.write(b);
        }
    }

    @Override
    public void flush() throws IOException {
        original.flush();
    }
}
