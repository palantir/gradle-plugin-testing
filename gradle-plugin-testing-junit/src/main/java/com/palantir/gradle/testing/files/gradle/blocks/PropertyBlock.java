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

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Block implementation for property assignments like rootProject.name = 'value'.
 * Unlike closure blocks, this doesn't have nested structure.
 */
public non-sealed class PropertyBlock implements Block {
    private final String name;
    private final String value;

    public PropertyBlock(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    @Override
    public Pattern pattern() {
        throw new UnsupportedOperationException("Pattern must be overridden in subclass");
    }

    @Override
    public Block parse(String content) {
        return new PropertyBlock(name, content.trim());
    }

    @Override
    public String render() {
        return value.isEmpty() ? "" : name + " = '" + value + "'";
    }

    @Override
    public Block merge(Block other) {
        return other instanceof PropertyBlock o && !o.value.isEmpty() ? o : this;
    }

    @Override
    public Block edit(Function<String, String> editor) {
        return parse(editor.apply(render()));
    }
}
