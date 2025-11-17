# Testing Gradle Plugins

This guide covers how to write tests for Gradle plugins using the `gradle-plugin-testing` framework.

## Contents

- [Quick Start](#quick-start)
    - [Write your first test](#write-your-first-test)
    - [IDE Setup - Syntax Highlighting](#ide-setup---syntax-highlighting)
- [Core Concepts](#core-concepts)
    - [The @GradlePluginTests Annotation](#the-gradleplugintests-annotation)
    - [Parameter Injection](#parameter-injection)
    - [Multi-Version Testing](#multi-version-testing)
- [File Operations](#file-operations)
    - [Working with Files](#working-with-files)
    - [Build Gradle](#build-gradle)
    - [Settings Gradle](#settings-gradle)
    - [Plugin Management](#plugin-management)
      - [Testing with External Plugins](#testing-with-external-plugins)
    - [Gradle Files](#gradle-files)
    - [Java Source Files](#java-source-files)
    - [Properties Files](#properties-files)
    - [YAML Files](#yaml-files)
    - [Arbitrary Files](#arbitrary-files)
    - [Directories](#directories)
    - [Build Directory](#build-directory)
- [Maven Repository Testing](#maven-repository-testing)
- [Executing Gradle Builds](#executing-gradle-builds)
    - [Basic Execution](#basic-execution)
    - [Running builds with Configuration Cache enabled](#running-builds-with-configuration-cache-enabled)
    - [Environment Variables](#environment-variables)
- [Assertions](#assertions)
    - [Fluent Assertion API](#fluent-assertion-api)
    - [Task Outcome Assertions](#task-outcome-assertions)
    - [Output Assertions](#output-assertions)
    - [File Assertions](#file-assertions)
- [Error Prone Checks](#error-prone-checks)
    - [Why We Ship Error Prone Checks](#why-we-ship-error-prone-checks)
    - [Configuring Error Prone](#configuring-error-prone)

## Quick Start

### Write your first test

```java
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.palantir.gradle.testing.assertion.GradlePluginTestAssertions.assertThat;

@GradlePluginTests
class MyPluginTest {
    @BeforeEach
    void setup(RootProject project) {
        project.buildGradle().plugins().add("my-plugin");
        project.buildGradle().append("""
                myPlugin {
                    enabled = true
                }
                """);
    }

    @Test
    void plugin_applies_successfully(GradleInvoker gradle, RootProject project) {
        assertThat(gradle.withArgs("tasks").buildsSuccessfully())
            .output()
            .contains("myTask");
    }
}
```


### IDE Setup - Syntax Highlighting

Add the [gradle-idea-language-injector](https://github.com/palantir/gradle-idea-language-injector) plugin to your root `build.gradle`:

```gradle
plugins {
    id 'com.palantir.idea-language-injector' version '<version>'
}
```

This plugin automatically generates IntelliJ IDEA language injection configurations, enabling syntax highlighting for string literals inside IntelliJ.

## Core Concepts

### The `@GradlePluginTests` Annotation

Mark test classes with `@GradlePluginTests` to enable Gradle plugin testing.

Each test gets an isolated project directory under `build/gradle-plugin-testing` for debugging purposes.

### Parameter Injection

Tests can request these parameters in any order:

- **`GradleInvoker`** - Executes Gradle builds
- **`RootProject`** - Gradle root project that will always be named "root". To use a different project name, call `rootProject.settingsGradle().rootProjectName("custom-name")`
- **`SubProject`** - Gradle subproject for the root project. The parameter name will be used as the project name exactly. For example, `SubProject apiService` creates a subproject named `apiService`
- **`MavenRepo`** - Maven repository for publishing test artifacts. See [Maven Repository Testing](#maven-repository-testing)

These parameters can be used in the constructor for a test class or in `@Test`, `@BeforeEach`, `@AfterEach`, `@BeforeAll`, or `@AfterAll` methods.

**Example with subprojects:**
```java
@Test
void multi_project_build(SubProject api, SubProject server) {
    // Projects "api" and "server" are automatically created and included as sub-projects to the "root" project.
    api.buildGradle().plugins().add("java-library");

    server.buildGradle().plugins().add("application");
    server.buildGradle().append("""
            dependencies {
                implementation project(':api')
            }
            """);
}
```

**Creating subprojects programmatically:**
```java
@Test
void create_nested_subprojects_manually(SubProject api) {
    // Nested subprojects
    SubProject nestedProject = api.subproject("nested");
}
```

### Multi-Version Testing

The framework automatically runs each test against multiple Gradle versions.

Configure which Gradle versions to test against using the `gradleTestUtils` extension:

```gradle
gradleTestUtils {
    gradleVersions = ['8.10', '8.14.3']
}
```

If not specified, tests run against the default version (currently `8.14.3`).

See [Resolution of Gradle versions to test against](../README.md#resolution-of-gradle-versions-to-test-against) for more details.

## File Operations

### Working with Files

All project files inherit from `ProjectFile<T>` with these methods:

- **`overwrite(String text)`** - Replace entire file content
- **`append(String text)`** - Add text to end of file
- **`prepend(String text)`** - Add text to start of file
- **`appendLine(String line)`** - Add line with newline
- **`prependLine(String line)`** - Add line at start
- **`edit(FileEditor editor)`** - Transform file content with callback
- **`text()`** - Read file content
- **`createEmpty()`** - Create an empty file
- **`assertThat()`** - AssertJ path assertions

All methods support `String.format()` syntax:
```java
project.buildGradle().append("version = '%s'", "1.0.0");
```

**Reading file contents:**
```java
String content = project.buildGradle().text();
```

**Creating empty files:**
```java
project.file("versions.lock").createEmpty();
```

### Build Gradle

Configure `build.gradle` for your projects:

```java
@Test
void configure_build_file(RootProject project) {
    project.buildGradle().append("""
        group = 'com.example'
        version = '1.0.0'

        repositories {
            mavenCentral()
        }

        dependencies {
            implementation 'com.google.guava:guava:32.1.0-jre'
        }
        """);
}

@Test
void configure_subproject_build(SubProject api) {
    api.buildGradle().append("""
        dependencies {
            api 'org.slf4j:slf4j-api:2.0.9'
        }
        """);
}
```

### Settings Gradle

Configure `settings.gradle`, the root project name defaults to `root` and can only be changed using the `rootProjectName` method on the settings file.

```java
@Test
void configure_settings(RootProject project) {
    project.settingsGradle()
        .rootProjectName("my-service")
        .include("api")
        .include("impl");
}
```

### Plugin Management

Use the structured `plugins()` API to add plugins:

```java
@Test
void add_plugins(RootProject project) {
    // Add plugins individually
    project.buildGradle()
        .plugins()
        .add("java")
        .add("application");

    // Add without applying
    project.buildGradle()
        .plugins()
        .addWithoutApply("com.palantir.baseline");
}
```

**Important:** Always use the `plugins()` API instead of manually writing plugin blocks in `append()` or `overwrite()` calls. The `plugins()` API ensures correct positioning after `buildscript {}` blocks and prevents duplicate plugin entries.

#### Testing with External Plugins

To test your plugin alongside external Gradle plugins, use the `gradlePluginForTesting` configuration in your projects `build.gradle`. This makes external plugins available in your test's Gradle runtime:

```gradle
dependencies {
    gradlePluginForTesting 'com.palantir.sls-packaging:gradle-sls-packaging:<version>'
}
```

The plugin is then available in your tests:

```java
@Test
void test_with_external_plugin(GradleInvoker gradle, RootProject project) {
    project.buildGradle().plugins().add("com.palantir.sls-asset-distribution");

    gradle.withArgs("tasks").buildsSuccessfully();
}
```

**Note:** If you forget to add a plugin to `gradlePluginForTesting`, your test will fail with an error like:
```
Plugin [id: 'com.palantir.sls-asset-distribution'] was not found in any of the following sources:
- Gradle Core Plugins (plugin is not in 'org.gradle' namespace)
- Gradle TestKit (classpath: ...)
- Plugin Repositories (plugin dependency must include a version number for this source)
```

When using [com.palantir.consistent-versions](https://github.com/palantir/gradle-consistent-versions), dependencies in `gradlePluginForTesting` are automatically included in the `[Test dependencies]` section of `versions.lock`.


### Gradle Files

Create custom Gradle files beyond `build.gradle` and `settings.gradle`:

```java
@Test
void create_custom_gradle_files(RootProject project) {
    // Create custom Gradle files
    project.gradleFile("dependencies.gradle").overwrite("""
        dependencies {
            implementation 'com.google.guava:guava:32.1.0-jre'
        }
        """);

    // Reference from build.gradle
    project.buildGradle().append("apply from: 'dependencies.gradle'");
}
```

### Java Source Files

Write and manipulate Java source:

```java
@Test
void create_java_class(RootProject project) {
    // Write a complete class
    project.mainSourceSet().java().writeClass("""
        package com.example;

        public class Calculator {
            public int add(int a, int b) {
                return a + b;
            }
        }
        """);

    // Access by class name
    project.mainSourceSet()
        .java()
        .fileByClassName("com.example.Calculator")
        .edit(text -> text.replace("add", "sum"));

    // Write to test sources
    project.testSourceSet().java().writeClass("""
        package com.example;

        import org.junit.jupiter.api.Test;

        class CalculatorTest {
            @Test
            void adds_numbers() {}
        }
        """);

    // Access custom source sets
    project.sourceSet("integrationTest")
        .java()
        .writeClass("""
            package com.example;

            class IntegrationTest {}
            """);
}
```

### Properties Files

Manage `gradle.properties` and other properties files:

```java
@Test
void set_gradle_properties(RootProject project) {
    // Use appendProperty for key-value pairs
    project.gradlePropertiesFile()
        .appendProperty("org.gradle.parallel", "true")
        .appendProperty("org.gradle.caching", "true");
}

@Test
void create_versions_props(RootProject project) {
    // Use propertiesFile() for arbitrary properties files
    project.propertiesFile("versions.props")
        .appendProperty("com.google.guava:*", "32.1.0-jre")
        .appendProperty("org.slf4j:*", "2.0.9");
}
```

### YAML Files

Create and manipulate YAML files:

```java
@Test
void create_yaml_files(RootProject project) {
    project
        .yamlFile("application.yml")
        .overwrite("""
            server:
              port: 8080
            database:
              url: jdbc:postgresql://localhost:5432/mydb
            """);
}
```

### Arbitrary Files

Create any file in the project:

```java
@Test
void create_arbitrary_files(RootProject project) {
    project.file("README.md").overwrite("# My Project");
    project.file(".gitignore").append("*.log\n");
    project.file("config.json").overwrite("""
        {
          "version": "1.0.0"
        }
        """);
}
```

### Directories

Create and work with directories:

```java
@Test
void create_directories(RootProject project) {
    // Create arbitrary directory structures
    project.directory("scripts")
        .file("deploy.sh")
        .overwrite("#!/bin/bash\necho 'Deploying...'");

    // Create nested directories explicitly
    project.directory("docs/api/v1/README.md")
        .createDirectories();
}
```

### Build Directory

Access Gradle's build output directory:

```java
@Test
void access_build_output(GradleInvoker gradle, RootProject project) {
    project.buildGradle().plugins().add("java");
    project.mainSourceSet().java().writeClass("""
        package com.example;
        public class Main {}
        """);

    gradle.withArgs("build").buildsSuccessfully();

    // Access compiled classes
    project.buildDir()
        .file("classes/java/main/com/example/Main.class")
        .assertThat()
        .exists();

    // Access JAR files
    project.buildDir()
        .file("libs/root.jar")
        .assertThat()
        .exists();
}
```

**Note:** `buildDir()` returns a `Directory`, so you can use all directory methods like `file()`, `directory()`, etc.

## Maven Repository Testing

Use `MavenRepo` to publish test artifacts to a local Maven repository for testing plugins that resolve external dependencies.

```java
@Test
void publish_and_resolve_artifacts(MavenRepo repo, RootProject root, GradleInvoker gradle) {
    // Publish simple artifacts
    repo.publish(MavenArtifact.of("com.example:library:1.0.0"));

    // Publish artifacts with dependencies
    repo.publish(
        MavenArtifact.of("com.example:base:1.0.0"),
        MavenArtifact.builder()
            .coordinate("com.example:advanced:2.0.0")
            .addDependency("com.example:base:1.0.0")
            .addDependency("com.google.guava:guava:32.1.0-jre")
            .build()
    );

    root.buildGradle().plugins().add("java-library");
    
    // Configure build to use the repository
    root.buildGradle().withMavenRepo(repo);
    root.buildGradle().append("""
        dependencies {
            implementation 'com.example:advanced:2.0.0'
        }
        """);

    gradle.withArgs("build").buildsSuccessfully();
}
```

The `MavenRepo` instance is shared across all test methods in a class. Artifacts published in lifecycle methods (eg. `@BeforeEach`) remain available for subsequent tests.

## Executing Gradle Builds

### Basic Execution

Use `GradleInvoker` to run builds:

```java
@Test
void successful_build(GradleInvoker gradle, RootProject project) {
    project.buildGradle().plugins().add("java");

    // Run and expect success
    InvocationResult result = gradle.withArgs("build").buildsSuccessfully();

    // Run and expect failure
    InvocationResult failure = gradle.withArgs("brokenTask").buildsWithFailure();
}
```

### Running builds with Configuration Cache enabled

When [`configuration cache is enabled (getConfigurationCacheEnabled() == true)`](../gradle-plugin-testing/src/main/java/com/palantir/gradle/plugintesting/PluginTestingExtension.java), each GradleInvoker invocation will automatically run twice:

1. The first run uses `--configuration-cache` and verifies that the configuration cache is properly stored.
2. The second run uses `--configuration-cache --dry-run` and verifies that the configuration cache entry is successfully reused.

If configuration cache issues are detected, the build will fail with an [UnexpectedConfigurationCacheFailure](../gradle-plugin-testing-junit/src/main/java/com/palantir/gradle/testing/execution/UnexpectedConfigurationCacheFailure.java).

For tests or test classes that are incompatible with configuration cache, use the `@DisabledConfigurationCache` annotation:

```java
// Disable configuration cache for a specific test method
@Test
@DisabledConfigurationCache(reason="task abc is incompatible with configuration cache")
void incompatible_configuration_cache_build(GradleInvoker gradle, RootProject project) {
```
Or
```java
// Disable for an entire test class
@GradlePluginTests
@DisabledConfigurationCache(reason="tasks abc, xyz are incompatible with configuration cache")
class PluginIncompatibleWithConfigCache {
```

### Environment Variables

Use [`gradle-utils:environment-variables`](https://github.com/palantir/gradle-utils) which provides a testing-friendly way to access environment variables via Gradle properties.

In your plugin, use `EnvironmentVariables` to read environment variables:

```java
// In your plugin code
@Nested
abstract EnvironmentVariables getEnvironmentVariables();

public void someMethod() {
    String value = getEnvironmentVariables().envVarOrFromTestingProperty("FOO").get();
}
```

In your tests, pass values via Gradle properties:

```java
@Test
void plugin_reads_environment_variable(GradleInvoker gradle, RootProject project) {
    project.buildGradle().plugins().add("my-plugin");

    gradle.withArgs("myTask", "-P__TESTING=true", "-P__TESTING_FOO=TEST_VALUE")
        .buildsSuccessfully();
}
```

## Assertions

### Fluent Assertion API

Import the assertion entry point:
```java
import static com.palantir.gradle.testing.assertion.GradlePluginTestAssertions.assertThat;
```

### Task Outcome Assertions

```java
@Test
void task_assertions(GradleInvoker gradle, RootProject project) {
    project.buildGradle().plugins().add("java");

    InvocationResult result = gradle.withArgs("build").buildsSuccessfully();

    // Check task succeeded
    assertThat(result).task(":compileJava").succeeded();

    // Check task was up-to-date
    InvocationResult secondRun = gradle.withArgs("build").buildsSuccessfully();
    assertThat(secondRun).task(":compileJava").upToDate();

    // Check task failed
    InvocationResult failure = gradle.withArgs("failingTask").buildsWithFailure();
    assertThat(failure).task(":failingTask").failed();

    // Check task was not executed
    assertThat(result).task(":nonExistentTask").notOnTaskGraph();

    // Check specific outcome
    assertThat(result).task(":compileJava").outcome().isEqualTo(TaskOutcome.FROM_CACHE);
}
```

### Output Assertions

```java
@Test
void output_assertions(GradleInvoker gradle, RootProject project) {
    project.buildGradle().append("println 'Configuration phase'");

    InvocationResult result = gradle.withArgs("tasks").buildsSuccessfully();

    // Check output contains text
    assertThat(result).output().contains("Configuration phase");

    // Use full AssertJ string assertions
    assertThat(result).output()
        .containsIgnoringCase("BUILD SUCCESSFUL")
        .doesNotContain("deprecated");
}
```

### File Assertions

```java
@Test
void file_assertions(GradleInvoker gradle, RootProject project) {
    project.buildGradle().plugins().add("java");
    project.mainSourceSet().java().writeClass("""
        package com.example;
        public class Main {}
        """);

    gradle.withArgs("build").buildsSuccessfully();

    // Assert files exist
    project.buildDir().file("classes/java/main/com/example/Main.class")
        .assertThat().exists();

    // Assert exact file contents
    project.buildGradle().assertThat().hasContent("""
        plugins {
            id 'java'
        }
        """);

    // Use AssertJ string assertions on file content
    project.buildGradle().assertThat().content()
        .contains("plugins")
        .doesNotContain("application");
}
```

## Error Prone Checks

The `gradle-plugin-testing` plugin ships with custom Error Prone checks that are automatically enabled when the `net.ltgt.errorprone` plugin is applied to your project.

### Why We Ship Error Prone Checks

These checks serve two purposes:

1. **Enforcing Best Practices** - The checks guide developers toward using the framework's structured APIs correctly, preventing common mistakes.

2. **Enabling Automated Migrations** - Many of the checks are patchable, meaning Error Prone can automatically fix violations. This allows the framework to evolve over time while automatically migrating existing test code to new APIs or patterns. When upgrading the testing framework, your tests can be automatically updated without manual intervention.

### Configuring Error Prone

To enable these checks, apply the Error Prone plugin to your project:

```gradle
plugins {
    id 'net.ltgt.errorprone' version '<version>'
}
```

The framework's Error Prone checks will be automatically registered and applied to your test code, catching issues at compile time and offering automated fixes where possible.
