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

package com.palantir.gradle.testing.files.java;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.intellij.lang.annotations.Language;

public record JavaSrcDir(Path srcDirPath) {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([^;]+);");
    private static final Pattern CLASS_PATTERN =
            Pattern.compile("(?:class|interface|record|enum|@interface)\\s+(\\w+)");

    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public JavaSrcDir {}

    public JavaFile writeClass(@Language("Java") String javaSource) {
        Optional<String> possiblePackagePath = possiblyExtractGroup(PACKAGE_PATTERN, javaSource);

        String className = possiblyExtractGroup(CLASS_PATTERN, javaSource)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Could not find the class name from the source: \n\n" + javaSource));

        String canonicalClassName =
                possiblePackagePath.map(packagePath -> packagePath + ".").orElse("") + className;

        return fileByClassName(canonicalClassName).overwrite(javaSource);
    }

    @FormatMethod
    public JavaFile writeClass(@Language("Java") String javaSource, Object... args) {
        return writeClass(javaSource.formatted(args));
    }

    public JavaFile fileByClassName(String canonicalClassName) {
        return fileByPath(canonicalClassName.replace('.', '/') + ".java");
    }

    public JavaFile fileByPath(String path) {
        return new JavaFile(srcDirPath.resolve(path));
    }

    private static Optional<String> possiblyExtractGroup(Pattern pattern, String source) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
