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
                //WARNING: Do not include any dependencies here that the plugin-testing-core project uses in it's
                // api or implementation configurations.  Because the plugin adds the plugin-testing-core artifact as
                // a testImplementation dependency, we get the transitive dependencies of it (e.g. guava).  Since those
                // versions will update over time this test could start erroneously failing.  So just list some random
                // dependencies here that are unlikely to be added to the plugin-test-core project.
                //implementation 'com.google.guava:guava:33.3.1-jre'
                implementation 'org.ow2.asm:asm:9.7.1'

                testImplementation 'org.apache.httpcomponents.client5:httpclient5:5.3.1'

                testRuntimeOnly 'com.palantir.gradle.consistentversions:gradle-consistent-versions:2.31.0'
            }
        """.stripIndent(true)

        when:
        def result = runTasksSuccessfully('writeTestDependencyVersions')

        then:
        outputFile.exists()
        !outputFile.text.contains('null')
        outputFile.text.contains('org.ow2.asm:asm=9.7.1')
        outputFile.text.contains('org.apache.httpcomponents.client5:httpclient5=5.3.1')
        outputFile.text.contains('com.palantir.gradle.consistentversions:gradle-consistent-versions=2.31.0')
        outputFile.text.contains('com.palantir.gradle.plugintesting:plugin-testing-core')
    }

    def 'write versions with GCV'() {
        given:
        // remember - the resolved version for this dependency is using the information passed from the version of the plugin
        // applied to the gradle-plugin-test project itself, _not_ the current version under test.  So the
        // addBuildScriptDependencies code and the "resolve" logic it calls is the current version, but the information
        // it is working with is from the last published version of the plugin (assuming that's the one applied to the
        // root build.gradle file of this project.
        buildFile << TestContentHelpers.addBuildScriptBlock('mavenCentral()', 'com.palantir.gradle.consistentversions:gradle-consistent-versions')
        //language=gradle
        buildFile << """
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
