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

import javax.inject.Inject;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.OutputFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DiscoveryTestsTask extends JavaExec {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryTestsTask.class);

    @InputFiles
    public abstract ConfigurableFileCollection getTestClasspath();

    @InputFiles
    public abstract ConfigurableFileCollection getRuntimeClasspath();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @Inject
    protected abstract ProjectLayout getProjectLayout();

    public DiscoveryTestsTask() {
        setClasspath(getProject().files(getRuntimeClasspath(), getTestClasspath()));
        getOutputFile().convention(getProjectLayout().getBuildDirectory().file("nebula-tests.txt"));
        getMainClass().set("com.palantir.gradle.plugintesting.RunDiscovery");
    }
}
