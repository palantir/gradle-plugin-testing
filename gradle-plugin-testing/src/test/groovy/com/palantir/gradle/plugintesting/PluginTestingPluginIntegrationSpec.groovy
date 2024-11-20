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
import spock.lang.IgnoreIf
import spock.lang.Unroll

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.stream.Stream

//import static com.palantir.gradle.plugintesting.TestDepVersions.resolve

class PluginTestingPluginIntegrationSpec extends IntegrationSpec {

    public static final String DEPRECATION_ERROR_MESSAGE_FROM_NEBULA = 'Deprecation warnings were found (Set the ignoreDeprecations system property during the test to ignore)'

    def setup() {
        writeHelloWorld('com.testing')
        System.setProperty('ignoreDeprecations', 'true')
        //language=gradle
        buildFile << """
            apply plugin: 'groovy'
            
            repositories {
                mavenCentral()
            }

            dependencies {
                implementation gradleApi()

                //TODO(#xxx): once we have a published version of the plugin, apply it to the project so we can use resolve
                testImplementation 'org.junit.jupiter:junit-jupiter:5.11.3'
                testImplementation 'com.netflix.nebula:nebula-test:10.6.1'
                //testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
            }
            tasks.withType(Test) {
                useJUnitPlatform()
            }
        """.stripIndent(true)

        //language=groovy
        file('src/test/groovy/com/testing/HelloWorldSpec.groovy') << '''
            package com.testing

            import nebula.test.IntegrationSpec

            class HelloWorldSpec extends IntegrationSpec {
                def setup() {
                    //language=gradle
                    buildFile << """
                        buildscript {
                            repositories {
                                mavenCentral()
                            }
                            dependencies {
                                // This version causes deprecation warnings in gradle 8 for gradle 9
                                classpath 'com.palantir.gradle.consistentversions:gradle-consistent-versions:2.27.0'
                            }
                        }
                        apply plugin: 'java'
                        apply plugin: 'com.palantir.consistent-versions'
                    """.stripIndent(true)
                    
                    file('versions.lock') << ''
                }

                def 'someTest'() {
                    when:
                    def result = runTasks('test')

                    then:
                    println "============std error follows============"
                    println result.standardError
                    println "============std out follows============"
                    println result.standardOutput
                    result.success
                }
            }
        '''.stripIndent(true)
    }

    /**
     * this is just a sanity check test to verify that nebula behaves as expected in the default case.  That is, it
     * will fail the test if there are gradle deprecation warnings.
     */
    def 'fails when plugin not applied'() {
        when:
        def result = runTasks('test')

        then:
        result.standardOutput.contains('HelloWorldSpec > someTest FAILED')
        result.standardOutput.contains(DEPRECATION_ERROR_MESSAGE_FROM_NEBULA)
        !result.success
    }

    def 'ignoreDeprecations automatically set when plugin applied'() {
        given:
        applyTestUtilsPlugin()

        when:
        def result = runTasks('test')

        then:
        result.success
        !result.standardOutput.contains(DEPRECATION_ERROR_MESSAGE_FROM_NEBULA)
    }

    def 'override gradle testing versions'() {
        given:
        applyTestUtilsPlugin()
        buildFile << """
            gradleTestUtils {
                gradleVersions = ['7.6.4', '8.10.1']
            }
        """.stripIndent(true)

        when:
        def result = runTasks('test')

        then:
        //TODO: verify test run with versions
        result.standardOutput.contains('8.10.1')
    }

    void applyTestUtilsPlugin() {
        //language=gradle
        buildFile << """
            apply plugin: 'com.palantir.gradle.plugin-testing'
        """.stripIndent(true)
    }

    @Override
    ExecutionResult runTasks(String... tasks) {
        def projectVersion = Optional.ofNullable(System.getProperty('projectVersion')).orElseThrow()
        String[] strings = tasks + ["-P${PluginTestingPlugin.PLUGIN_VERSION_PROPERTY_NAME}=${projectVersion}".toString()]
        return super.runTasks(strings)
    }
}
