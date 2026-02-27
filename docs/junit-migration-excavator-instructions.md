You are an expert in Gradle plugin development and testing. I need you to migrate a test class from the old Nebula testing framework to the new Java-based testing framework. Below is a complete guide to help with this migration.  If you have not already been told the test class to migrate, it will be given at the end of the instructions, along with the location to put the new test.

# Original Framework Overview (Nebula)

The old framework uses:
- Groovy language with Spock testing framework
- Base classes: IntegrationSpec or IntegrationTestKitSpec or custom Specification
- Direct file manipulation with buildFile << "content"
- Multi-version testing with explicit gradleVersion = version and Spock data tables

# New Framework Overview (Java)

The new framework uses:
- Java language with JUnit 5
- @GradlePluginTests annotation instead of inheritance
- Parameter injection for test components (GradleInvoker, RootProject, SubProject)
- Fluent APIs for file manipulation (rootProject.buildGradle().append("content"))
- Automatic multi-version Gradle testing
- Modern assertions with AssertJ
- Structured support for adding files to tests, including gradle build files, properties files, and java source files.

IMPORTANT: The GRADLE_PLUGIN_TEST_REPO_PATH environment variable points to a local copy of the source repository for this new framework.  The framework is in the gradle-plugin-testing-junit sub-project.  If you cannot find the source repository, you should immediately stop and report the issue.

IMPORTANT: Before starting any migration, you MUST read the testing guide called "testing-guide.md" located in the docs directory at the root level of the cloned repository.  Consult this guide first before making assumptions about how to use the framework.  Also look at source files in the new framework as needed to learn about additional APIs.

## Preparation Instructions
Before beginning the migration, add comments to the original groovy test files to delineate methods, multiline variable declarations, and sections of test methods to assist human reviewers when comparing the old and new files. These comments should follow the following format:
- Comments should start with the phrase "DELINEATOR FOR REVIEW" to make it easy to automatically remove them later.
- Comments should contain the name of the component they are indicating
- Within test methods, add comments before the keywords for Spock tests that indicate different parts of the test.  The keywords are the following when followed by a colon - setup, when, then, expect.
    - Do not add a comment before the setup keyword when it is the first line of a test.

An example of delineator comments in a groovy file:
```
    // ***DELINEATOR FOR REVIEW: standardBuildFile
    def standardBuildFile = '''
        plugins {
            id 'java-library'
        }
        
        apply plugin: 'com.palantir.baseline-testing'
        
        repositories {
            mavenCentral()
        }
    '''

    // ***DELINEATOR FOR REVIEW: this_is_a_test_for_something
    def "this is a test for something"() {
        setup:
            buildFile << standardBuildFile
            buildFile << '''
            dependencies {
                testImplementation 'junit:junit'
            }
            '''

        // ***DELINEATOR FOR REVIEW: when
        when:
            runTasksSuccessfully('test')
        // ***DELINEATOR FOR REVIEW: then
        then:
            fileExists("build/reports/tests/test/classes/test.JUnit4Test.html")
    }
```
These comments should be copied over to the new test files as they are created to assist human reviewers.

# General Instructions
- You MUST migrate ALL tests from the original file. Do not skip any tests or only migrate a "representative sample". Every single test method in the original Groovy file must have a corresponding test method in the new Java file.
- Always add both `@GradlePluginTests` and `@DisabledConfigurationCache` to the migrated test classes.
- Test names should be changed to a snake_case_english_sentence in all lower case.
- Keep the comments from the original Groovy tests when writing the new tests.
- Method name mappings from old to new framework:
    - `.build()` → `.buildsSuccessfully()`
    - `.buildAndFail()` → `.buildsWithFailure()`
    - `with('task').build()` → `gradle.withArgs('task').buildsSuccessfully()`
- When an external plugin is used ensure the correct `gradlePluginForTesting` configuration is added to the projects `build.gradle` file
- Do NOT add `com.palantir.gradle.plugintesting:gradle-plugin-testing-junit` to `versions.props`. The gradle-plugin-testing plugin automatically provides the correct version of this dependency.
- When migrating ONLY use the `standardBuildFile` pattern if it was used in the old groovy test
- If you need to parse a POM / xml file use `jackson` with `records` e.g.
```java
@JsonIgnoreProperties(ignoreUnknown = true)
record PomProject(
    @JacksonXmlProperty(localName = "dependencies") PomDependencies dependencies) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PomDependencies(
            @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "dependency")
            List<PomDependency> dependency) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PomDependency(
            @JacksonXmlProperty(localName = "groupId") String groupId,
            @JacksonXmlProperty(localName = "artifactId") String artifactId,
            @JacksonXmlProperty(localName = "version") String version,
            @JacksonXmlProperty(localName = "scope") String scope) {}
}
```


# File Manipulation Instructions
- Follow the instructions in the testing-guide.md for how to manipulate files, especially for common types like gradle build files, properties files, and java source files.
- Prefer text blocks instead of String concatenations in texts, especially when writing to files (gradle, java etc. files). Add a new line after a text block for clarity reasons.
- If the groovy test class has a variable which defines a java file as a string, keep the same in the java class and pass it to the `java().writeClass(...)` method when used.
- If the groovy test class has a variable which defines a build file as a string, create a helper method in the java class that does the same.
    - Name the helper method the same as the variable it is replacing
    - Put the helper method in roughly the same location in the new file as where the variable is defined in the old file, even if that means putting the method before variable definitions at the top of the class.
    - The helper method should return the GradleFile object so that further content can be added to it fluidly.

# Assertion Instructions
- Follow the instructions in the test-guide.md under "Assertions" for how to use fluent assertions built into the framework.
- Use chained assertions when checking multiple things on the same value.
- Use `.as("description")` instead of code comments to describe what an assertion is checking. The `.as()` description appears in failure messages, making test failures self-explanatory without needing to look at the source code. Comments are invisible in test output.

```java
// WRONG - comment is invisible in test failure output
// check that the config file was generated correctly
externalDepsFile.assertThat().exists();

// CORRECT - description appears in the assertion failure message
externalDepsFile.assertThat().as("external deps config file was generated").exists();
```

# Common Migration Pitfalls

## Plugin Application
- Do not use `apply plugin: 'foo'` in `build.gradle` text blocks
- Use the structured `rootProject.buildGradle().plugins().add("plugin-id")` API instead

## Parameterized Tests
- The framework injects `GradleInvoker`, `RootProject`, etc. as test method parameters
- `@ParameterizedTest` works with the framework, but the parameterized test parameters must come first
- Example: `void my_test(String param, GradleInvoker gradle, RootProject rootProject)` - the `String param` from `@MethodSource` comes before the injected parameters

## Don't Add Unused Parameters to Method Signatures
Only include injected parameters (`GradleInvoker`, `RootProject`, `SubProject`, etc.) in a test method signature if the method actually uses them. Unused parameters add noise and make tests harder to read.

```java
// WRONG - rootProject is unused
@Test
void fails_if_dependency_was_removed(GradleInvoker gradle, RootProject rootProject, SubProject foo) {
    foo.buildGradle().append(...);
    gradle.withArgs("check").buildsWithFailure();
}

// CORRECT - only declare parameters that are used
@Test
void fails_if_dependency_was_removed(GradleInvoker gradle, SubProject foo) {
    foo.buildGradle().append(...);
    gradle.withArgs("check").buildsWithFailure();
}
```

## Prefer SubProject Parameter Injection Over rootProject.subproject()
When a test needs a subproject, prefer injecting it as a `SubProject` parameter rather than creating it manually via `rootProject.subproject("name")`. Parameter injection is more concise, and the parameter name becomes the project name automatically.

```java
// WRONG - unnecessarily verbose, also requires RootProject in the signature
@Test
void my_test(GradleInvoker gradle, RootProject rootProject) {
    SubProject foo = rootProject.subproject("foo");
    foo.buildGradle().plugins().add("java-library");
    ...
}

// CORRECT - inject SubProject directly; parameter name "foo" becomes the project name
@Test
void my_test(GradleInvoker gradle, SubProject foo) {
    foo.buildGradle().plugins().add("java-library");
    ...
}
```

Use `project.subproject()` when creating nested subprojects.

## Helper Methods
- Helper methods that need gradle/project access should take `GradleInvoker` and/or `RootProject` as parameters
- Do not store these as instance fields (injection happens per-test)

## Java Text Blocks vs Groovy Triple-Quoted Strings
- Java text blocks do not preserve a leading newline like Groovy `'''` strings
- Groovy: `def s = '''\nfoo'''` starts with a newline
- Java: `String s = """\nfoo"""` also starts with a newline, but `String s = """\n    foo"""` has different indentation behavior
- When migrating assertions that compare source content, be aware of these differences

## Don't Use buildscript Blocks for External Plugins
Do not use `buildscript { }` blocks with `classpath` dependencies to load external plugins. This bypasses TestKit and makes plugins unavailable to the `.plugins().add()` API:

```java
// WRONG - Don't do this
rootProject.buildGradle().append("""
    buildscript {
        repositories { mavenCentral() }
        dependencies {
            classpath 'com.palantir.baseline:gradle-baseline-java:5.38.0'
        }
    }
    apply plugin: 'com.palantir.baseline'
    """);

// CORRECT - Add to gradlePluginForTesting in build.gradle, then use:
rootProject.buildGradle().plugins().add("com.palantir.baseline");
```

## Don't Access Framework Internals
The framework isolates each test run into its own directory, namespaced by Gradle version. You should never need to know which Gradle version is running.

```java
// WRONG - Accessing implementation details
String version = ((DefaultGradleInvoker) gradle).gradleVersion().version();
Path outputFile = rootProject.buildDir().path()
    .resolve(String.format("reports/output-%s.xml", version));

// CORRECT - Use simple paths; the framework handles isolation
Path outputFile = rootProject.buildDir().path().resolve("reports/output.xml");
```

If you find yourself casting `GradleInvoker` to `DefaultGradleInvoker`, the approach is probably wrong.

## Use Full Task Paths for Explicit Assertions
When asserting on specific task outcomes, use the full task path including the project name for subproject tasks:

```java
// WRONG - Task is in subproject, not root
assertThat(result).task(":checkstyleMain").succeeded();

// CORRECT - Include subproject in path
assertThat(result).task(":myProject:checkstyleMain").succeeded();
```

This only matters for explicit task assertions. Running tasks by name (e.g., `gradle.withArgs("checkstyleMain")`) works without the full path since Gradle resolves the task across all projects.

## Test Setup Best Practices

**Combine `buildGradle().append()` calls**: Use a single append with one text block, not multiple calls:

```java
// CORRECT
project.buildGradle().append("""
    group 'com.example'
    version '1.0.0'
    """);

// WRONG - multiple appends
project.buildGradle().append("group 'com.example'");
project.buildGradle().append("version '1.0.0'");
```

**No leading newlines**: Start text block content immediately after `"""`. Test file readability matters more than the generated build files, which are rarely read:

```java
// CORRECT
project.buildGradle().append("""
    myPlugin {
        enabled = true
    }
    """);

// WRONG - unnecessary leading newline
project.buildGradle().append("""

    myPlugin {
    """);
```

**Adding a Maven repository**: Prefer the built-in `.withMavenRepo(repo)` API on a project's build file. If a single project needs the repo, call it on that project. If all projects need it, use an `allprojects` block in the root build file:

```java
// BEST - use withMavenRepo for a single project
rootProject.buildGradle().withMavenRepo(repo);

// ALSO CORRECT - allprojects block when subprojects also need the repo
rootProject.buildGradle().append("""
    allprojects {
        repositories {
            maven { url uri("%s") }
        }
    }
    """, repo.path());

// WRONG - manually writing the repositories block when withMavenRepo does it for you
rootProject.buildGradle().append("""
    repositories {
        maven { url uri("%s") }
    }
    """, repo.path());
```

**Skip unnecessary setup**: Don't call `settingsGradle().rootProjectName()` unless you need a custom name.

**Use exact assertions for lock files**: Prefer `isEqualTo()` with text blocks over `contains()` for lock file content - makes expected output obvious.

# Final Instructions
- Make sure the migrated tests compile by running `./gradlew compileTestJava`.
- As you discover errors in your work, write out what the error was and what you did to find the information to fix it to a file called "test-migration-errors.md".
- Once the migration is complete and the new file compiles, relook at the testing-guide.md file and the above instructions to make sure you've followed all the best practices for use of the framework.  Update the converted test as necessary.
    - Add notes to the test-migration-errors.md file about the changes made on this second pass through.
    - If you made additional changes to the test, then review and update it one more time, also adding to test-migration-errors.md as necessary.
