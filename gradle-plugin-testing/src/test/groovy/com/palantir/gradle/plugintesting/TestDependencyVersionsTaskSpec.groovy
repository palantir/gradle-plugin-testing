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

import static TestDependencyVersions.resolve

class TestDependencyVersionsTaskSpec extends AbstractTestingPluginSpec {

    File outputFile

    def setup() {
        //TODO(#xxx): once we have a published version of the plugin that works with resolved dependencies, remove this
        System.setProperty(TestDependencyVersions.TEST_DEPENDENCIES_SYSTEM_PROPERTY, 'org.junit.jupiter:junit-jupiter:5.11.3,com.netflix.nebula:nebula-test:10.6.1, com.palantir.baseline:gradle-baseline-java:6.4.0,com.google.guava:guava:33.3.1-jre,com.palantir.gradle.consistentversions:gradle-consistent-versions:2.31.0')

        outputFile = new File(projectDir,'build/plugin-testing/dependency-versions.properties')

        //language=gradle
        buildFile << """
            buildscript {
                repositories {
                    mavenCentral()
                }
                dependencies {
                    // remember - this invocation of resolve is using the information passed from the version of the plugin
                    // applied to this project itself, _not_ the current version under test.  So the resolve code itself is
                    // the current version, but the information it is working with is from the last published version of
                    // the plugin.
                    classpath '${resolve("com.palantir.gradle.consistentversions:gradle-consistent-versions")}'
                }
            }
            apply plugin: 'com.palantir.consistent-versions'
            apply plugin: 'groovy'
            apply plugin: 'com.palantir.gradle-plugin-testing'
            
            repositories {
                mavenCentral()
                mavenLocal()
            }

            dependencies {
                implementation 'com.google.guava:guava'

                testImplementation 'org.junit.jupiter:junit-jupiter'
                testImplementation 'com.netflix.nebula:nebula-test'

                testRuntimeOnly 'com.palantir.gradle.consistentversions:gradle-consistent-versions'
            }
        """.stripIndent(true)

        TestContentHelpers.addVersionsToPropsFile(file('versions.props'), ['org.junit.jupiter:junit-jupiter', 'com.netflix.nebula:nebula-test', 'com.google.guava:guava', 'com.palantir.gradle.consistentversions:gradle-consistent-versions'])
        runTasksSuccessfully('writeVersionLocks')
    }

    def 'write versions'() {
        when:
        def result = runTasksSuccessfully('writeTestDependencyVersions')

        then:
        outputFile.exists()
        outputFile.text.contains('org.junit.jupiter:junit-jupiter')
    }
}
