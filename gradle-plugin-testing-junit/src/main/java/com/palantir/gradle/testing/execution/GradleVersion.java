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

package com.palantir.gradle.testing.execution;

public record GradleVersion(String version) implements Comparable<GradleVersion> {
    @Override
    public String toString() {
        return version;
    }

    @Override
    public int compareTo(GradleVersion other) {
        return org.gradle.util.GradleVersion.version(this.version)
                .compareTo(org.gradle.util.GradleVersion.version(other.version));
    }
}
