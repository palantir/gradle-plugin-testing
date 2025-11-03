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

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A block representing property assignments (non-closure syntax).
 * <p>
 * Used for simple key-value assignments like {@code version = '1.0'} or {@code rootProject.name = 'myapp'}.
 * Subclasses must override {@link #pattern()} to define the specific property pattern to match.
 *
 * @see Block
 * @see ClosureBlock
 */
non-sealed class PropertyBlock implements Block {
    private final String name;
    private final String value;

    PropertyBlock(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public final String name() {
        return name;
    }

    public final String value() {
        return value;
    }

    /**
     * Must be overridden by subclasses to define the property's parsing pattern.
     */
    public Pattern pattern() {
        throw new UnsupportedOperationException("Pattern must be overridden in subclass");
    }

    @Override
    public final Optional<ExtractionResult> extract(String content) {
        Matcher matcher = pattern().matcher(content);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new ExtractionResult(matcher.group(1), matcher.start(), matcher.end()));
    }

    @Override
    public final Block parse(String content) {
        return new PropertyBlock(name, content.trim());
    }

    @Override
    public final String renderContent() {
        return value.isEmpty() ? "" : name + " = '" + value + "'";
    }

    @Override
    public final String renderBlock() {
        return renderContent();
    }

    @Override
    public final Block merge(Block other) {
        return other instanceof PropertyBlock o && o.name.equals(this.name) && !o.value.isEmpty() ? o : this;
    }

    @Override
    public final Optional<Block> getChild(String childName) {
        return Optional.empty();
    }

    @Override
    public final Block withChild(String childName, Block child) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support children");
    }
}
