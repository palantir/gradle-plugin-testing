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
import static com.palantir.gradle.plugintesting.TestDepVersions.resolve

class TestDepVersionsTests extends IntegrationSpec {
    def setup() {
        TestDepVersions.setVersionsDir(projectDir.toPath())
        //write versions.props - I am aware of the irony that I am using a hardcoded version within the test file
        file('versions.props') << """
            com.palantir.sls-packaging:* = 7.69.0
        """.stripIndent(true)
        file('versions.lock') << """
            com.palantir.sls-packaging:gradle-sls-packaging-api:7.69.0 (1 constraints: f2133970)
        """.stripIndent(true)

        System.setProperty('ignoreDeprecations', 'true')
        //language=gradle
        buildFile << """
            apply plugin: 'groovy'
            
            repositories {
                mavenCentral()
            }
            apply plugin: 'com.palantir.gradle.plugin-testing'

            dependencies {
                implementation gradleApi()

                testImplementation '${resolve("org.junit.jupiter:junit-jupiter")}'
                testImplementation '${resolve("com.netflix.nebula:nebula-test")}'
                //testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
            }
            tasks.withType(Test) {
                useJUnitPlatform()
            }
        """.stripIndent(true)

    }

    def 'test fully specified dep'() {
        when:
        String result = resolve('com.palantir.sls-packaging:gradle-sls-packaging')

        then:
        result == 'com.palantir.sls-packaging:gradle-sls-packaging:7.69.0'
    }

    def 'test only group'() {
        when:
        String result = resolve('com.palantir.sls-packaging:something-bogus')

        then:
        result == 'com.palantir.sls-packaging:something-bogus:7.69.0'
    }

    def 'test missing dep'() {
        when:
        resolve('does-not-exist:foo')

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains('No version found for does-not-exist:foo')
    }

}
