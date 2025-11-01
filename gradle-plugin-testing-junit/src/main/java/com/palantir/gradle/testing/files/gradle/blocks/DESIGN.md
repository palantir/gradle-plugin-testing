# Gradle Block System

Structured editing for Gradle files with automatic block ordering and formatting.

## Architecture

```
GradleFile (interface)
    ├── BuildGradleFile extends StructuredGradleFile
    └── SettingsGradleFile extends StructuredGradleFile

StructuredGradleFile
    • Defines blocks and order
    • Parse: text → ParsedContent
    • Render: ParsedContent → text

Block (sealed interface)
    ├── ClosureBlock - all closure blocks (simple & nested)
    └── PropertyBlock - property assignments

GradleBlock - view onto a specific block enabling chained operations

ParsedContent - parsed result with blocks map and unstructured content
```

## Core Concepts

### Self-Contained Blocks

Each block knows its name, regex pattern, rendering, and merging logic:

```java
public record ClosureBlock(String name, Map<String, Block> children, String unstructuredContent) {
    // Simple block: empty children, content in unstructuredContent
    // Nested block: children in LinkedHashMap, content parsed recursively
}
```

### Block Ordering

Blocks always render in predefined order:

**BuildGradleFile:** buildscript → plugins → allprojects → subprojects → repositories → dependencies → configurations

**SettingsGradleFile:** pluginManagement → plugins → dependencyResolutionManagement

### Parse → Edit → Render Cycle

1. **Parse** - Extract blocks via regex, preserve unmatched content
2. **Edit** - Navigate to block, apply transformation
3. **Update** - Replace block in ParsedContent (immutable)
4. **Render** - Write blocks in order with indentation

### Unified Block Type

ClosureBlock handles both simple and nested blocks:
- **Simple** (empty children): plugins { id 'java' }
- **Nested** (LinkedHashMap children): buildscript { repositories { } }

### Polymorphic Navigation

No type checking - uses `getChild()` and `withChild()` polymorphically:

```java
Block current = parsed.getBlockAt(templates, "buildscript", "repositories");
// Works for any nesting depth without instanceof checks
```

## Key Classes

### Block
Core interface: parse(), renderContent(), renderBlock(), merge(), edit(), getChild(), withChild()

### ClosureBlock
Unified block for all closures. Stores children in LinkedHashMap for order preservation.
Empty children = simple block, non-empty = nested.

### PropertyBlock
Property assignments like `version = '1.0'`. Subclasses define pattern.

### ParsedContent
Immutable parse result. Provides package-private utilities for parsing, rendering, navigation, updates.

### GradleBlock
View onto a block within a file. Enables chaining: `buildGradle.buildscript().repositories().append(...)`

### StructuredGradleFile
Base class defining block structure. Subclasses implement blocks() and blockOrder().

## Usage

```java
// Simple blocks
buildGradle.plugins().append("id 'java'");
buildGradle.repositories().append("mavenCentral()");

// Nested blocks
buildGradle.buildscript().repositories().append("gradlePluginPortal()");

// Operations
buildGradle.dependencies().append("implementation 'x:y:1.0'");
buildGradle.repositories().prepend("mavenLocal()");
buildGradle.plugins().overwrite("id 'application'");
buildGradle.plugins().edit(content -> content.replace("java", "java-library"));

// Raw content merging
buildGradle.append("""
    plugins { id 'java' }
    version = '1.0.0'
    """);
// Plugins block merged, version preserved as unstructured
```

## Extension

Add new block type:

```java
// 1. Define block
protected static Block closure(String name) {
    return new ClosureBlock(name, Map.of(), "");
}

// 2. Add to blocks()
@Override
protected List<Block> blocks() {
    return List.of(..., closure("publishing"));
}

// 3. Add accessor
public GradleBlock publishing() {
    return new GradleBlock(this, "publishing");
}
```

## Design Principles

- **Self-contained** - blocks know their own behavior
- **Immutable** - all operations return new instances
- **Optional-based** - no nulls anywhere
- **Polymorphic** - no instanceof checks
- **Order-preserving** - LinkedHashMap for children
- **Minimal visibility** - package-private utilities where possible
