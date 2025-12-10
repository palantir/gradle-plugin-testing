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

public class SimpleThreadAwareOutputStream extends OutputStream {
    private final PrintStream original;
    private final ConcurrentHashMap<Thread, ByteArrayOutputStream> threadBuffers;

    public SimpleThreadAwareOutputStream(PrintStream original) {
        this.original = original;
        this.threadBuffers = new ConcurrentHashMap<>();
    }

    public void registerThread(Thread thread) {
        threadBuffers.put(thread, new ByteArrayOutputStream());
    }

    public void flushThreadOutput(Thread thread) {
        ByteArrayOutputStream buffer = threadBuffers.get(thread);
        if (buffer != null && buffer.size() > 0) {
            synchronized (original) {
                try {
                    buffer.writeTo(original);
                    original.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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

            // Flush on newline for real-time output
            if (b == '\n') {
                flushThreadOutput(current);
                buffer.reset();
            }
        } else {
            original.write(b);
        }
    }
}
