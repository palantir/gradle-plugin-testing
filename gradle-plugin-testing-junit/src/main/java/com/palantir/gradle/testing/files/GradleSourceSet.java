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

import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import com.palantir.gradle.testing.files.arbitrary.ArbitraryDirectory;
import com.palantir.gradle.testing.files.java.JavaSrcDir;
import java.nio.file.Path;

public record GradleSourceSet(Path path) {
    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public GradleSourceSet {}

    public JavaSrcDir java() {
        return new JavaSrcDir(path.resolve("java"));
    }

    public Directory srcDir(String srcDirName) {
        return new ArbitraryDirectory(this.path.resolve(srcDirName));
    }
}
