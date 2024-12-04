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
import static TestDependencyVersions.version

import java.util.stream.Collectors;
import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult

class PluginTestingPluginIntegrationSpec extends IntegrationSpec {

    private static final String DEPRECATION_ERROR_MESSAGE_FROM_NEBULA = 'Deprecation warnings were found (Set the ignoreDeprecations system property during the test to ignore)'

    File specUnderTest

    def setup() {
        //TODO(#xxx): once we have a published version of the plugin that works with resolved dependencies, remove this
        System.setProperty(TestDependencyVersions.TEST_DEPENDENCIES_SYSTEM_PROPERTY, 'org.junit.jupiter:junit-jupiter:5.11.3,com.netflix.nebula:nebula-test:10.6.1, com.palantir.baseline:gradle-baseline-java:6.4.0,com.google.guava:guava:33.3.1-jre,com.palantir.gradle.consistentversions:gradle-consistent-versions:2.31.0')

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
            
            repositories {
                mavenCentral()
                mavenLocal()
            }

            dependencies {
                implementation gradleApi()
                implementation 'com.google.guava:guava'

                testImplementation 'org.junit.jupiter:junit-jupiter'
                testImplementation 'com.netflix.nebula:nebula-test'
            }
            tasks.withType(Test) {
                useJUnitPlatform()
            }
        """.stripIndent(true)

        //Setup java source file that uses guava
        //language=java
        writeJavaSourceFile '''
            package com.testing;
            import com.google.common.collect.ImmutableMap;

            public class HelloWorld {
                static final ImmutableMap<String, String> HELLO_WORLD = ImmutableMap.of("hello", "world");
                public static void main(String[] args) {
                    System.out.println("Hello, World!");
                }
            }
        '''.stripIndent(true)

        //language=groovy
        specUnderTest = file('src/test/groovy/com/testing/HelloWorldSpec.groovy') << '''
            package com.testing

            //INSERT IMPORTS HERE
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
                                // DO NOT REPLACE THIS VERSION WITH A 'resolve' call
                                classpath 'com.palantir.gradle.consistentversions:gradle-consistent-versions:2.27.0'
                            }
                        }
                        apply plugin: 'java'
                        apply plugin: 'com.palantir.consistent-versions'
                        
                        //INSERT MORE HERE
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
                
                //INSERT MORE TESTS HERE
            }
        '''.stripIndent(true)

        writeVersionsPropsFile(file('versions.props'), ['org.junit.jupiter:junit-jupiter', 'com.netflix.nebula:nebula-test', 'com.google.guava:guava'])
        runTasksSuccessfully('writeVersionLocks')
    }

    /**
     * this is just a sanity check test to verify that nebula behaves as expected in the default case.  That is, it
     * will fail the test if there are gradle deprecation warnings.
     */
    def 'fails with gradle deprecation warnings when plugin not applied'() {
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

    def 'enable ignoreDeprecations'() {
        given:
        applyTestUtilsPlugin()
        buildFile << """
            gradleTestUtils {
                ignoreGradleDeprecations = false
            }
        """.stripIndent(true)

        when:
        def result = runTasks('test')

        then:
        result.standardOutput.contains('HelloWorldSpec > someTest FAILED')
        result.standardOutput.contains(DEPRECATION_ERROR_MESSAGE_FROM_NEBULA)
        !result.success
    }

    def 'works when applied before other plugins'() {
        given:
        prependToBuildFile('''
            apply plugin: 'com.palantir.gradle-plugin-testing'
        ''')

        when:
        def result = runTasks('test')

        then:
        result.success
    }

    def 'resolve dependencies'() {
        given:
        applyTestUtilsPlugin()
        specUnderTest.text = specUnderTest.text
            .replace('//INSERT IMPORTS HERE', '''
                import static com.palantir.gradle.plugintesting.TestDependencyVersions.resolve
            '''.stripIndent(true))
            .replace('//INSERT MORE HERE', '''
            dependencies {
                testImplementation '${resolve("org.junit.jupiter:junit-jupiter")}'
                testImplementation '${resolve("com.netflix.nebula:nebula-test")}'
                testImplementation '${resolve("com.google.guava:guava")}'
            }
        ''')

        when:
        def result = runTasks('test')

        then:
        result.success
        def generatedBuildFile = file('build/nebulatest/com.testing.HelloWorldSpec/someTest/build.gradle')
        generatedBuildFile.exists()
    }

    def 'override gradle testing versions'() {
        given:
        applyTestUtilsPlugin()
        buildFile << """
            gradleTestUtils {
                gradleVersions = ['7.6.4', '8.10.1']
            }
        """.stripIndent(true)

        //add a test case to the spec
        specUnderTest.text = specUnderTest.text
            .replace('//INSERT IMPORTS HERE', '''
                import com.palantir.gradle.plugintesting.GradleTestVersions
            '''.stripIndent(true))

            //language=groovy
            .replace('//INSERT MORE TESTS HERE', '''
               def 'test with version: #version'() {
                    when:
                    gradleVersion = version
            
                    then:
                    def result = runTasks('test')
                    println "============std error from test with version============"
                    println result.standardError
                    println "============std error from test with version============"
                    println result.standardOutput
                    result.success
            
                    where:
                    version << GradleTestVersions.gradleVersionsForTests
                }
            '''.stripIndent())

        when:
        def result = runTasks('test')

        then:
        result.standardOutput.contains('test with version: #version > test with version: 7.6.4')
        result.standardOutput.contains('test with version: #version > test with version: 8.10.1')
    }

    def 'checkUnusedDependencies ignores the plugin'() {
        given:
        //language=gradle
        prependToBuildFile("""
            buildscript {
                repositories {
                    mavenCentral() { metadataSources { mavenPom(); ignoreGradleMetadataRedirection() } }
                    gradlePluginPortal() { metadataSources { mavenPom(); ignoreGradleMetadataRedirection() } }
                }
                dependencies {
                    classpath '${resolve('com.palantir.baseline:gradle-baseline-java')}'
                }
            }
            apply plugin: 'com.palantir.baseline-exact-dependencies'
        """)
        applyTestUtilsPlugin()

        when:
        def result = runTasks('checkUnusedDependencies')

        then:
        !result.standardOutput.contains('Found 1 dependencies unused during compilation')
        result.success
    }

    void applyTestUtilsPlugin() {
        //language=gradle
        buildFile << """
            apply plugin: 'com.palantir.gradle-plugin-testing'
        """.stripIndent(true)
    }

    File prependToBuildFile(String content) {
        buildFile.text = content.stripIndent(true) + buildFile.text
        return buildFile
    }

    @Override
    ExecutionResult runTasks(String... tasks) {
        def projectVersion = Optional.ofNullable(System.getProperty('projectVersion')).orElseThrow()
        String[] strings = tasks + ["-P${PluginTestingPlugin.PLUGIN_VERSION_PROPERTY_NAME}=${projectVersion}".toString()]
        return super.runTasks(strings)
    }

    //TODO: Maybe make this a utility method in TestDependencyVersions or other utility class
    void writeVersionsPropsFile(File versionPropsFile, Collection<String> dependencies) {
        String versionsProps = dependencies.stream()
            .map { dependency -> dependency + "=" + version(dependency)}
            .collect(Collectors.joining("\n"))
        versionPropsFile << versionsProps
    }
}
