/*
 * (c) Copyright 2024 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.gradle.plugintesting

import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult

class AbstractTestingPluginSpec extends IntegrationSpec{

    @Override
    ExecutionResult runTasks(String... tasks) {
        def projectVersion = Optional.ofNullable(System.getProperty('projectVersion')).orElseThrow()
        String[] strings = tasks + ["-P${PluginTestingPlugin.PLUGIN_VERSION_PROPERTY_NAME}=${projectVersion}".toString()]
        return super.runTasks(strings)
    }

    //TODO(#xxx): once we have a published version of the plugin that generates the file, can remove this
    void writeDependenciesVersionsFile() {
        File versionsFile = file('hardcoded-dependency-versions.properties')
        versionsFile << """
            org.junit.jupiter:junit-jupiter=5.11.3
            com.netflix.nebula:nebula-test=10.6.1
            com.google.guava:guava=33.3.1-jre
            com.palantir.gradle.consistentversions:gradle-consistent-versions=2.31.0
            com.palantir.baseline:gradle-baseline-java=6.4.0
        """.stripIndent(true)
        System.setProperty(TestDependencyVersions.TEST_DEPENDENCIES_FILE_SYSTEM_PROPERTY, versionsFile.absolutePath);
    }
}