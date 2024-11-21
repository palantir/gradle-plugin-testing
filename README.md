<p align=right>
   <a href="https://autorelease.general.dmz.palantir.tech/palantir/gradle-plugin-testing"><img src="https://img.shields.io/badge/Perform%20an-Autorelease-success.svg" alt="Autorelease"></a>
</p>

# About

Gradle plugin and helpers to assist with writing tests for other gradle plugins.

## Using the Plugin
    
 ```groovy
apply plugin: 'com.palantir.gradle-plugin-testing'
 ```

The plugin automatically adds a `testImplementation` dependency on the `com.palantir.gradle.plugintesting:plugin-testing-core` library so that the helper classes are available in tests.

# Primary Functionality

## Automatic suppression of gradle deprecation warnings
The nebula-test framework will automatically fail a test if it detects gradle deprecation messages in the output.  Unfortunately, this will fail if another plugin used in a test is using deprecated gradle components even if the actual plugin under test is clean.  The plugin-testing plugin will automatically set the `ignoreDeprecations` system property so that the nebula-test framework will ignore deprecation messages.  Use other mechanisms, such as java deprecation linting, to detect and fix deprecations in the plugin under test.

This behavior can be overridden using the `gradleTestUtils` extension.  e.g.

```groovy
gradleTestUtils {
    ignoreGradleDeprecations = false
}
```

## Resolution of dependencies in generated build files
Integration tests for Gradle plugins often write build files which include other plugins.  The version of those included plugins is hardcoded in some way, either directly in the test, e.g.

```groovy
//...within a nebula spec...
  buildFile << """
     buildscript {
         repositories {
             mavenCentral()
         }
         dependencies {
             classpath 'com.palantir.gradle.conjure:gradle-conjure:0.0.1'
         }
     }
     dependencies {
         implementation 'com.palantir.conjure.java:conjure-lib:0.0.1'
     }
     """
```

or in some "Versions" class that has a listing of all the dependencies the test uses.  e.g. 
```java
final class TestPluginVersions {
    static final String CONJURE_JAVA = "com.palantir.conjure.java:conjure-java:5.7.1";
    static final String CONJURE = "com.palantir.conjure:conjure:4.10.1";
```

Once these tests are written, the versions of the plugins are often not updated, even when the project under test keeps its dependencies of those plugins up-to-date.  This can cause tests to fail when the plugin is updated not because the plugin is bad, but because there is an incompatibility in the old versions of plugins used in the integration tests.  This can often happen with Gradle version bumps.

When applied to a project, the `gradle-plugin-testing` plugin scans the `testRuntimeClasspath` configuration for the project and passes all dependencies to the test task as a system property.  The version of the dependencies can then be resolved when tests are run and written into generated files.  e.g.

```groovy
import static com.palantir.gradle.plugintesting.TestDepVersions.resolve
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
                classpath '${resolve('com.palantir.gradle.conjure:gradle-conjure')}'
            }
        }
        dependencies {
            implementation '${resolve('com.palantir.conjure.java:conjure-lib')}'
        }
        """
   }
```
# Resolution of Gradle versions to test against
Similarly, tests may hardcode versions of Gradle that they need to stay compatible with. These versions also get stale and PRs start failing for the inverse reason of the above - the code in the plugin or a dependency of it is updated and is no longer compatible with an old version of Gradle. For example, attempting to update jackson libraries from `2.15.0` -> `2.17.0` would fail if a test tried to run on Gradle versiosn < `7.6.4` (when compatibility with jackson `2.17.0` was fixed).

The `GradleTestVersions` class can provide up-to-date versions of Gradle to test against.  
```groovy
import nebula.test.IntegrationSpec
import com.palantir.gradle.plugintesting.GradleTestVersions

class HelloWorldSpec extends IntegrationSpec {
    def 'runs on version of gradle: #version'() {
        when:
        gradleVersion = version

        then:
        def result = runTasks('someTask')
        result.success

        where:
        version << GradleTestVersions.gradleVersionsForTests
    }
}
```
The plugin sets default versions to test against, but these can be overridden using the `gradleTestUtils` extension.  e.g.

```groovy
gradleTestUtils {
    gradleVersions = ['7.6.4', '8.8']
}
```
