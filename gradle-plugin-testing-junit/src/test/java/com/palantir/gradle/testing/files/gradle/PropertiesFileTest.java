/*
 * (c) Copyright 2026 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.gradle.testing.files.gradle;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.palantir.gradle.testing.files.properties.PropertiesFile;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PropertiesFileTest {
    PropertiesFile propertiesFile;

    @BeforeEach
    void beforeEach(@TempDir Path tempDir) {
        propertiesFile = new PropertiesFile(tempDir.resolve("gradle.properties")).createEmpty();
    }

    @Test
    void can_append_properties() {
        propertiesFile.appendProperty("some.value", "true");
        propertiesFile.appendProperty("other.value", "true");
        propertiesFile.assertThat().hasContent("""
            some.value=true
            other.value=true
            """);

        assertThatThrownBy(() -> propertiesFile.appendProperty("other.value", "false"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Property 'other.value' already exists with a different value: 'true'");

        propertiesFile.appendProperty("some.value", "true");
        propertiesFile.assertThat().hasContent("""
            some.value=true
            other.value=true
            """);

        assertThatThrownBy(() -> propertiesFile.appendProperty("some.value", "false"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Property 'some.value' already exists with a different value: 'true'");
    }
}
