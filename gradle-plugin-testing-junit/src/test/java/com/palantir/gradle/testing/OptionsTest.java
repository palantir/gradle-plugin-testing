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

package com.palantir.gradle.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.gradle.testing.execution.Options;
import java.util.List;
import org.junit.jupiter.api.Test;

public class OptionsTest {

    @Test
    void can_build_options() {
        Options options = Options.builder().args(List.of("a", "b", "c")).build();
        assertThat(Options.from(options).args(List.of("d", "e")).build())
                .isEqualTo(Options.builder().args(List.of("d", "e")).build());
    }
}
