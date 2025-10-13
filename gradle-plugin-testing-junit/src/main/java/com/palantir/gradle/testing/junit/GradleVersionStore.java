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

package com.palantir.gradle.testing.junit;

import com.palantir.gradle.testing.execution.GradleVersion;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

final class GradleVersionStore {
    private static final Namespace NAMESPACE = Namespace.create(GradleVersionStore.class);
    private static final String KEY = "gradleVersion";

    public static GradleVersion gradleVersion(ExtensionContext context) {
        return context.getStore(NAMESPACE).get(KEY, GradleVersion.class);
    }

    public static void setGradleVersion(ExtensionContext context, GradleVersion gradleVersion) {
        context.getStore(NAMESPACE).put(KEY, gradleVersion);
    }

    private GradleVersionStore() {}
}
