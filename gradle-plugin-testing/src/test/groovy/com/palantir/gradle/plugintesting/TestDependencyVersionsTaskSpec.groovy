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
        outputFile = new File(projectDir,'build/plugin-testing/dependency-versions.properties')
    }

    def 'write versions without GCV'() {
        given:
        //language=gradle
        buildFile << """
            apply plugin: 'groovy'
            apply plugin: 'com.palantir.gradle-plugin-testing'
            
            repositories {
                mavenCentral()
                mavenLocal()
            }

            dependencies {
                implementation 'com.google.guava:guava:33.3.1-jre'

                testImplementation 'org.junit.jupiter:junit-jupiter:5.11.3'
                testImplementation 'com.netflix.nebula:nebula-test:10.6.1'

                testRuntimeOnly 'com.palantir.gradle.consistentversions:gradle-consistent-versions:2.31.0'
            }
        """.stripIndent(true)

        when:
        def result = runTasksSuccessfully('writeTestDependencyVersions')

        then:
        outputFile.exists()
        !outputFile.text.contains('null')
        outputFile.text.contains('com.google.guava:guava=33.3.1-jre')
        outputFile.text.contains('org.junit.jupiter:junit-jupiter=5.11.3')
        outputFile.text.contains('com.netflix.nebula:nebula-test=10.6.1')
        outputFile.text.contains('com.palantir.gradle.consistentversions:gradle-consistent-versions=2.31.0')
        outputFile.text.contains('com.palantir.gradle.plugintesting:plugin-testing-core')
    }

    def 'write versions with GCV'() {
        given:
        writeDependenciesVersionsFile()
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

        TestContentHelpers.addVersionsToPropsFile(file('versions.props'), [
                'org.junit.jupiter:junit-jupiter',
                'com.netflix.nebula:nebula-test',
                'com.google.guava:guava',
                'com.palantir.gradle.consistentversions:gradle-consistent-versions'])
        runTasksSuccessfully('writeVersionLocks')

        when:
        def result = runTasksSuccessfully('writeTestDependencyVersions')

        then:
        outputFile.exists()
        !outputFile.text.contains('null')
        outputFile.text.contains('org.junit.jupiter:junit-jupiter')
    }
}
