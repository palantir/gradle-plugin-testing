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

package com.palantir.gradle.testing.files;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.stream.Stream;
import org.assertj.core.api.AbstractPathAssert;
import org.assertj.core.api.Assertions;

public interface ProjectFile<T extends ProjectFile<T>> {
    Path path();

    @FormatMethod
    default T overwrite(@FormatString String text, Object... args) {
        return overwrite(args.length > 0 ? text.formatted(args) : text);
    }

    default T overwrite(String text) {
        writeString(path(), text, StandardOpenOption.TRUNCATE_EXISTING);
        return (T) this;
    }

    default T append(String text) {
        writeString(path(), text, StandardOpenOption.APPEND);
        return (T) this;
    }

    @FormatMethod
    default T append(@FormatString String text, Object... args) {
        return append(format(text, args));
    }

    default T appendLine(String line) {
        return append(line + "\n");
    }

    @FormatMethod
    default T appendLine(@FormatString String line, Object... args) {
        return appendLine(format(line, args));
    }

    default T prepend(String text) {
        edit(existingText -> text + existingText);
        return (T) this;
    }

    @FormatMethod
    default T prepend(@FormatString String text, Object... args) {
        return prepend(format(text, args));
    }

    default T prependLine(String line) {
        return prepend(line + "\n");
    }

    @FormatMethod
    default T prependLine(@FormatString String line, Object... args) {
        return prependLine(format(line, args));
    }

    @FormatMethod
    private static String format(String text, Object... args) {
        return args.length > 0 ? text.formatted(args) : text;
    }

    interface FileEditor {
        String edit(String text);
    }

    default T edit(FileEditor editor) {
        String text = Files.exists(path()) ? text() : "";
        return overwrite(editor.edit(text));
    }

    default T createEmpty() {
        return overwrite("");
    }

    default AbstractPathAssert<?> assertThat() {
        return Assertions.assertThat(path());
    }

    default String text() {
        try {
            return Files.readString(path());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeString(Path path, String text, StandardOpenOption... options) {
        try {
            Files.createDirectories(path.getParent());

            StandardOpenOption[] allOptions = Stream.concat(
                            Arrays.stream(options), Stream.of(StandardOpenOption.CREATE))
                    .toArray(StandardOpenOption[]::new);

            Files.writeString(path, text, StandardCharsets.UTF_8, allOptions);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
