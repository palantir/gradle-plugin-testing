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
- Test names should be changed to a snake_case_english_sentence in all lower case.
- Keep the comments from the original Groovy tests when writing the new tests.
- Method name mappings from old to new framework:
    - `.build()` → `.buildsSuccessfully()`
    - `.buildAndFail()` → `.buildsWithFailure()`
    - `with('task').build()` → `gradle.withArgs('task').buildsSuccessfully()`
- When an external plugin is used ensure the correct `gradlePluginForTesting` configuration is added to the projects `build.gradle` file
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

# Final Instructions
- Make sure the migrated tests compile by running `./gradlew compileTestJava`.
- As you discover errors in your work, write out what the error was and what you did to find the information to fix it to a file called "test-migration-errors.md".
- Once the migration is complete and the new file compiles, relook at the testing-guide.md file and the above instructions to make sure you've followed all the best practices for use of the framework.  Update the converted test as necessary.
    - Add notes to the test-migration-errors.md file about the changes made on this second pass through.
    - If you made additional changes to the test, then review and update it one more time, also adding to test-migration-errors.md as necessary.
