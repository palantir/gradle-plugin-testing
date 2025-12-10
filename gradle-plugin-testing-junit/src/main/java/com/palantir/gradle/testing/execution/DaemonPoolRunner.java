/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.gradle.testing.execution;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import one.util.streamex.IntStreamEx;
import org.gradle.internal.SystemProperties;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.testkit.runner.internal.GradleProvider;
import org.gradle.testkit.runner.internal.TestKitDirProvider;

public class DaemonPoolRunner extends AbstractGradleRunner {
    private final List<NamedToolingApiGradleExecutor> executorPool;
    private static int currExecutor = 0;

    private GradleProvider gradleProvider;
    private TestKitDirProvider testKitDirProvider;
    private File projectDirectory;
    private List<String> arguments = Collections.emptyList();
    private List<String> jvmArguments = Collections.emptyList();
    private ClassPath classpath = ClassPath.EMPTY;
    private boolean debug;
    private OutputStream standardOutput;
    private OutputStream standardError;
    private InputStream standardInput;
    private boolean forwardingSystemStreams;
    private Map<String, String> environment;

    /**
     * Constructor for pool-based approach (creates its own pool and round-robins).
     */
    public DaemonPoolRunner(int daemonPoolSize) {
        executorPool = IntStreamEx.range(daemonPoolSize)
                .mapToObj(i -> new NamedToolingApiGradleExecutor("test-kit-daemon-" + i))
                .toList();
        this.testKitDirProvider = calculateTestKitDirProvider(SystemProperties.getInstance());
        this.debug = Boolean.getBoolean(DEBUG_SYS_PROP);
    }

    protected NamedToolingApiGradleExecutor getNextExecutor() {
        // Use round-robin from pool
        NamedToolingApiGradleExecutor executor = executorPool.get(currExecutor);
        System.err.println("Running with executor (round-robin): " + executor);
        currExecutor = (currExecutor + 1) % executorPool.size();
        return executor;
    }
}
