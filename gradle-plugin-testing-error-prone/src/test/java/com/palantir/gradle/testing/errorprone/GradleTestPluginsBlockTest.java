/*
 * (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.gradle.testing.errorprone;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

class GradleTestPluginsBlockTest {
    @Test
    void catch_all_plugin_patterns() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    // BUG: Diagnostic contains: GradleTestPluginsBlock
                    file.append("apply plugin: 'java'");

                    // BUG: Diagnostic contains: GradleTestPluginsBlock
                    file.append("plugins.apply('java')");

                    // BUG: Diagnostic contains: GradleTestPluginsBlock
                    file.append("pluginManagement.apply('java')");

                    // BUG: Diagnostic contains: GradleTestPluginsBlock
                    file.append("plugins { id 'java' }");

                    // BUG: Diagnostic contains: GradleTestPluginsBlock
                    file.append("plugins { id 'java' apply false }");
                }
            }
            """);
    }

    @Test
    void catch_quote_variations() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    // BUG: Diagnostic contains: GradleTestPluginsBlock
                    file.append("apply plugin: \\"java\\"");

                    // BUG: Diagnostic contains: GradleTestPluginsBlock
                    file.append(\"""
                        plugins {
                            id "java"
                            id 'application'
                        }
                        \""");
                }
            }
            """);
    }

    @Test
    void catch_variable_reference_no_autofix() {
        testUnchanged("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    String content = "apply plugin: 'java'";
                    // BUG: Diagnostic contains: GradleTestPluginsBlock
                    file.append(content);
                }
            }
            """);
    }

    @Test
    void allow_all_valid_cases() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    // Correct API usage
                    file.plugins().add("java");

                    // No plugins block context
                    file.append("id 'java'");

                    // Non-plugin content
                    file.append("repositories { mavenCentral() }");

                    // Comments should be ignored
                    file.append("// apply plugin: 'java'");
                    file.append("/* plugins.apply('java') */");
                    file.append("\""
                                /*
                                  plugins.apply('java')
                                */
                                "\"");

                    // Word 'id' in other contexts
                    file.append("description = 'This is a valid project id string'");
                    file.append("tasks.register('myTask') { id 'something' }");
                }
            }
            """);
    }

    @Test
    void allow_outside_gradle_plugin_tests() {
        test("""
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            class TestClass {
                void test(GradleFile file) {
                    file.append("apply plugin: 'java'");
                }
            }
            """);
    }

    @Test
    void autofix_simple_patterns() {
        testFix("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.append("apply plugin: 'java'");
                    // other code
                    file.append("plugins.apply('application')");
                }
            }
            """, """
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.plugins().add("java");
                    // other code
                    file.plugins().add("application");
                }
            }
            """);
    }

    @Test
    void autofix_with_content() {
        testFix("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.append(\"""
                        repositories { mavenCentral() }
                        apply plugin: 'java'
                        dependencies { }
                        \""");
                }
            }
            """, """
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.append(\"""
                        repositories { mavenCentral() }

                        dependencies { }
                        \""");
                    file.plugins().add("java");
                }
            }
            """);
    }

    @Test
    void autofix_multiple_plugins() {
        testFix("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.append(\"""
                        apply plugin: 'java'
                        repositories { }
                        apply plugin: 'application'
                        tasks { }
                        \""");
                }
            }
            """, """
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.append(\"""
                        repositories { }

                        tasks { }
                        \""");
                    file.plugins().add("java").add("application");
                }
            }
            """);
    }

    @Test
    void autofix_plugins_block() {
        testFix("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.append("plugins { id 'java' }");

                    file.append(\"""
                        plugins {
                            id 'application'
                            id 'maven-publish'
                        }
                        \""");
                }
            }
            """, """
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.plugins().add("java");

                    file.plugins().add("application").add("maven-publish");
                }
            }
            """);
    }

    @Test
    void autofix_apply_false() {
        testFix("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.append(\"""
                        plugins {
                            id 'java'
                            id 'application' apply false
                            id 'maven-publish'
                        }
                        \""");
                }
            }
            """, """
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.plugins().add("java").addWithoutApply("application").add("maven-publish");
                }
            }
            """);
    }

    @Test
    void autofix_mixed_quote_styles() {
        testFix("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.append(\"""
                        plugins {
                            id "java"
                            id 'application'
                        }
                        \""");
                }
            }
            """, """
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.plugins().add("java").add("application");
                }
            }
            """);
    }

    @Test
    void autofix_all_content_methods() {
        testFix("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.overwrite("apply plugin: 'java'");
                    file.prepend("apply plugin: 'application'");
                    file.append("apply plugin: 'maven-publish'");
                    file.appendLine("apply plugin: 'idea'");
                    file.prependLine("apply plugin: 'eclipse'");
                    file.edit(text -> text + "apply plugin: 'java'");
                }
            }
            """, """
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.plugins().add("java");
                    file.plugins().add("application");
                    file.plugins().add("maven-publish");
                    file.plugins().add("idea");
                    file.plugins().add("eclipse");
                    file.plugins().add("java");
                }
            }
            """);
    }

    @Test
    void autofix_multiple_plugins_calls() {
        testFix("""
            import com.palantir.gradle.testing.files.gradle.GradleFile;
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.append(\"""
                    plugins {
                        id 'java'
                        id 'application'
                    }

                    // some other code

                    plugins {
                        id 'java'
                        id 'application'
                    }
                    \""");
                }
            }
            """, """
            import com.palantir.gradle.testing.files.gradle.GradleFile;
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.append(\"""
                    // some other code
                    \""");
                    file.plugins().add("java").add("application");
                }
            }
            """);
    }

    @Test
    void catch_chained_append_methods() {
        testUnchanged("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.append("repositories { mavenCentral() }")
                    // BUG: Diagnostic contains: GradleTestPluginsBlock
                        .append("apply plugin: 'java'");

                    // BUG: Diagnostic contains: GradleTestPluginsBlock
                    file.append("apply plugin: 'application'")
                        .append("dependencies { }");
                }
            }
            """);
    }

    @Test
    void catch_varargs_overloads_no_autofix() {
        testUnchanged("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    // BUG: Diagnostic contains: Plugins must be added using .plugins().add()
                    file.overwrite("//other content \\n apply plugin: '%s'", "arg1");
                }
            }
            """);
    }

    @Test
    void autofix_overwrite_with_text_block() {
        testFix("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.overwrite(""\"
                        dependencies {}
                        apply plugin: 'java'
                        ""\");
                }
            }
            """, """
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.overwrite(""\"
                        dependencies {}
                        ""\");
                    file.plugins().add("java");
                }
            }
            """);
    }

    @Test
    void autofix_block_lambda() {
        testFix("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.edit(text -> {
                        return text + "apply plugin: 'java'";
                    });
                }
            }
            """, """
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.gradle.GradleFile;

            @GradlePluginTests
            class TestClass {
                void test(GradleFile file) {
                    file.plugins().add("java");
                }
            }
            """);
    }

    private void test(@Language("Java") String javaCode) {
        CompilationTestHelper compilationTestHelper =
                CompilationTestHelper.newInstance(GradleTestPluginsBlock.class, getClass());
        compilationTestHelper.addSourceLines("TestClass.java", javaCode).doTest();
    }

    private void testFix(@Language("Java") String input, @Language("Java") String expected) {
        BugCheckerRefactoringTestHelper refactoringTestHelper =
                BugCheckerRefactoringTestHelper.newInstance(GradleTestPluginsBlock.class, getClass());
        refactoringTestHelper
                .addInputLines("TestClass.java", input)
                .addOutputLines("TestClass.java", expected)
                .doTest();
    }

    private void testUnchanged(@Language("Java") String javaCode) {
        BugCheckerRefactoringTestHelper refactoringTestHelper =
                BugCheckerRefactoringTestHelper.newInstance(GradleTestPluginsBlock.class, getClass());
        refactoringTestHelper
                .addInputLines("TestClass.java", javaCode)
                .expectUnchanged()
                .doTest();
    }
}
