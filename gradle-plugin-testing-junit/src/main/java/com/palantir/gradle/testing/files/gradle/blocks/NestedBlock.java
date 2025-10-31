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

package com.palantir.gradle.testing.files.gradle.blocks;

import com.palantir.gradle.testing.files.gradle.OrderedGradleFile;
import java.util.List;

/**
 * A block that can contain nested child blocks.
 * Subclasses must override {@link #childBlockOrder()} to define their children.
 */
public abstract class NestedBlock extends NamedBlock {
    public NestedBlock(OrderedGradleFile file, String blockName) {
        super(file, blockName);
    }

    public NestedBlock(NestedBlock parent, String blockName) {
        super(parent, blockName);
    }

    /**
     * Define the canonical ordering of child blocks for this block.
     * Must be overridden by subclasses to specify their nested children.
     */
    protected abstract List<String> childBlockOrder();

    /**
     * Access a nested child block by name.
     */
    protected final NamedBlock nested(String childName) {
        return new NamedBlock(this, childName);
    }
}
