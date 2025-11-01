# Gradle Block System

A structured editing system for Gradle build files that maintains proper block ordering and formatting.

## Architecture

```
GradleFile (interface)
    ├── BuildGradleFile extends StructuredGradleFile
    └── SettingsGradleFile extends StructuredGradleFile

StructuredGradleFile
    • Defines blocks and their order
    • Parse: text → ParsedContent
    • Render: ParsedContent → text

Block (sealed interface)
    ├── ClosureBlock - simple blocks (plugins, repositories, dependencies)
    ├── NestedClosureBlock - blocks with children (buildscript, configurations)
    └── PropertyBlock - property assignments (version = '1.0')

GradleBlock - wrapper enabling chained access to nested blocks

ParsedContent - parsing result with blocks map and unstructured content
```

## Core Concepts

### Self-Contained Blocks

Each block knows its:
- Name (e.g., "plugins", "buildscript")
- Regex pattern for parsing
- How to render itself
- How to merge with other blocks

```java
public record ClosureBlock(String name, String content) implements Block {
    @Override
    public Pattern pattern() {
        return Pattern.compile("^\\s*" + Pattern.quote(name) + "\\s*\\{([^{}]*)\\}");
    }
}
```

### Block Ordering

Blocks always render in predefined order, regardless of edit order:

**BuildGradleFile:** buildscript → plugins → allprojects → subprojects → repositories → dependencies → configurations

**SettingsGradleFile:** pluginManagement → plugins → dependencyResolutionManagement

```java
// Edit in any order
buildGradle.dependencies().append("...");
buildGradle.plugins().append("...");

// Always renders in correct order
plugins { ... }
dependencies { ... }
```

### Parse → Edit → Render Cycle

1. **Parse** - Extract blocks using regex patterns, preserve unrecognized content
2. **Edit** - Navigate to target block, apply transformation
3. **Update** - Replace block in ParsedContent
4. **Render** - Write blocks in order with proper indentation

### Nested Blocks

Blocks can contain ordered child blocks:

```java
buildGradle.buildscript().repositories().append("mavenCentral()");

// Renders as:
buildscript {
    repositories {
        mavenCentral()
    }
}
```

### Intelligent Merging

Raw content with blocks is parsed and merged correctly:

```java
buildGradle.plugins().append("id 'java'");
buildGradle.append("""
    plugins {
        id 'maven-publish'
    }
    repositories {
        mavenCentral()
    }
    """);

// Result:
plugins {
    id 'java'
    id 'maven-publish'
}

repositories {
    mavenCentral()
}
```

## Key Classes

### Block Interface
Core abstraction with parse(), render(), merge(), edit() methods.

### ClosureBlock
Simple blocks with unstructured text content. Indentation normalized during parse, applied during render.

### NestedClosureBlock
Composite blocks with ordered children and optional unstructured content. Recursively parses and renders children.

### PropertyBlock
Property assignments like `version = '1.0'`. Subclasses define pattern.

### ParsedContent
Immutable parsing result containing:
- `blocks` - Map of block name to Block
- `unstructuredContent` - Content not matching any pattern

Static utilities for parsing, rendering, navigation, and updates.

### GradleBlock
Wrapper for block-scoped operations. Enables chaining:
```java
buildGradle.buildscript().repositories().append("mavenCentral()")
```

### StructuredGradleFile
Base class defining block structure. Subclasses specify blocks() and blockOrder().

## Usage Examples

```java
// Simple block editing
buildGradle.plugins().append("id 'java'");
buildGradle.repositories().append("mavenCentral()");

// Nested block editing
buildGradle.buildscript().repositories().append("gradlePluginPortal()");
buildGradle.configurations().all().resolutionStrategy().append("force 'x:y:1.0'");

// Block operations
buildGradle.dependencies().append("implementation 'junit:junit:4.13'");
buildGradle.repositories().prepend("mavenLocal()");
buildGradle.plugins().overwrite("id 'application'");
buildGradle.plugins().edit(content -> content.replace("java", "java-library"));

// Raw content merging
buildGradle.append("""
    plugins {
        id 'java'
    }
    version = '1.0.0'
    """);
// Plugins block merged, version preserved as unstructured
```

## Extension

### Add a New Block Type

1. Define block constant:
```java
private static final Block PUBLISHING = new ClosureBlock("publishing", "");
```

2. Add to blocks() and blockOrder():
```java
@Override
protected List<Block> blocks() {
    return List.of(..., PUBLISHING);
}
```

3. Add accessor:
```java
public GradleBlock publishing() {
    return new GradleBlock(this, "publishing");
}
```

## Implementation Notes

- **Indentation:** 4 spaces per level
- **Unrecognized content:** Preserved at end after structured blocks
- **Pattern matching:** Blocks extracted in defined order during parsing
- **Immutability:** All operations return new instances, no mutation
- **Optional usage:** No null checks - Optional patterns throughout
