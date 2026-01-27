## GradleParameter design

The goal of `GradleParameter` to allow users to pass certain variables to a test dependent on the gradle version.

It should work similarly to `ValueSource` in junit (maybe building on it?)

Example: 

```java
@GradlePluginTests
class GradleParameterEqualToFixtureTest {

    @Test
    @GradleParameter(
            name = "behavior",
            otherwiseStrings = "otherwise",
            value = {
                @ForVersion(
                        lessThan = "9.3.0",
                        strings = {"lessThan1", "lessThan2"}),
                @ForVersion(equalTo = "8.14.3", strings = "equal")
            })
    void test_one(GradleInvoker gradleInvoker, RootProject rootProject, String behavior) {
        rootProject.buildGradle().append("""
            println "Behavior: %s"
            """, behavior);
    }

    @Test
    @GradleParameter(
            name = "behavior",
            otherwiseInt = 1,
            value = {
                    @ForVersion(
                            lessThan = "9.3.0",
                            ints = 2),
            })
    void test_two(GradleInvoker gradleInvoker, RootProject rootProject, String behavior) {
        rootProject.buildGradle().append("""
                println "Behavior: %s"
                """, behavior);
    }

    @Test
    void other_test(GradleInvoker gradleInvoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            println "Behavior: %s"
            """, "behavior");
    }
}
```

Given we run with gradle version 9.3.0, 8.14.3 and 7.6.4 we would expect the following tests to run:

```
Gradle 7.6.4:
    test_one:
        behavior = lessThan1
        behavior = lessThan2
    test_two:
        behavior = 2
    other_test
Gradle 8.14.3:
    test_one:
        behavior = lessThan1
        behavior = lessThan2
        behavior = equals
    test_two:
        behavior = 2
    other_test
Gradle 9.3.0:
    test_one:
        behavior = otherwise
    test_two:
        behavior = 1
    other_test
```

Critical note - we do not create test we do not run i.e. no skipped tests (e.g. for 8.14.3 test_two there is no skipped behavior = 1)

Critical note - type checking is performed so all ForVersion's have the same type and don't use multiple types and these types match the otherwise type

A user should be able to stack multiple `GradleParameter` and it should behave as expected:

```java
@GradlePluginTests
class GradleParameterEqualToFixtureTest {

    @Test
    @GradleParameter(
            name = "behavior",
            otherwiseStrings = "otherwise",
            value = {
                @ForVersion(
                        lessThan = "9.3.0",
                        strings = {"lessThan1", "lessThan2"}),
                @ForVersion(equalTo = "8.14.3", strings = "equal")
            })
    @GradleParameter(
            name = "maxInt",
            otherwiseInt = {3, 4},
            value = {
                    @ForVersion(
                            lessThan = "9.3.0",
                            ints = {1, 2}),
            })
    void test_one(GradleInvoker gradleInvoker, RootProject rootProject, String behavior) {
        rootProject.buildGradle().append("""
            println "Behavior: %s"
            """, behavior);
    }

    @Test
    void other_test(GradleInvoker gradleInvoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            println "Behavior: %s"
            """, "behavior");
    }
}
```

We would expect:
```
Gradle 7.6.4:
    test_one:
        behavior = lessThan1, maxInt = 1
        behavior = lessThan2, maxInt = 1
        behavior = lessThan1, maxInt = 2
        behavior = lessThan2, maxInt = 2
    other_test
Gradle 8.14.3:
    test_one:
        behavior = lessThan1, maxInt = 1
        behavior = lessThan2, maxInt = 1
        behavior = equals, maxInt = 1
        behavior = lessThan1, maxInt = 2
        behavior = lessThan2, maxInt = 2
        behavior = equals, maxInt = 2
    other_test
Gradle 9.3.0:
    test_one:
        behavior = otherwise, maxInt = 3
        behavior = otherwise, maxInt = 4
    other_test
```

Again no skipped tests created

Overlapping is supported, we only have lessThan or Equals, newer versions are covered by otherwise.
If no matching case then we should throw an error
If empty strings = {} then we don't creat invocations for that condition
Within a single `GradleParameter` all types must be the same for all `ForVersion` and `otherwise` - if not throw

