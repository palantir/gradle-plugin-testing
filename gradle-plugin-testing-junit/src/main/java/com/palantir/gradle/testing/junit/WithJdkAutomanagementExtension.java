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

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class WithJdkAutomanagementExtension implements BeforeAllCallback, BeforeEachCallback {

    private static final Logger log = Logging.getLogger(WithJdkAutomanagementExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        setUpJdkManagement(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        setUpJdkManagement(context);
    }

    private void setUpJdkManagement(ExtensionContext context) {
        log.info("Enabling the jdk management.");
        JdkAutomanagementStore.enableJdkAutomanagement(context);
    }
}
