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

package com.palantir.gradle.testing.files.gradle;

import java.util.List;
import java.util.Map;

/**
 * Result of parsing blocks from content - contains parsed blocks and remaining unmatched content.
 *
 * @param blocks parsed blocks indexed by name, with each name potentially having multiple blocks
 * @param remaining content that didn't match any block patterns
 */
record ParseResult(Map<String, List<Block>> blocks, String remaining) {}
