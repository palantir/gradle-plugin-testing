# Gradle File Block System Design

## Overview

This system provides a structured way to edit Gradle build files while maintaining proper block ordering and formatting. It replaces the previous template-based approach with a simpler, self-contained block architecture.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         GradleFile                              │
│                       (base interface)                          │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                ┌───────────┴───────────┐
                │                       │
    ┌───────────▼──────────┐  ┌────────▼─────────┐
    │  BuildGradleFile     │  │ SettingsGradleFile│
    │                      │  │                    │
    │  extends             │  │  extends           │
    │  StructuredGradleFile│  │  StructuredGradleFile│
    └───────────┬──────────┘  └────────┬───────────┘
                │                      │
                │   Defines blocks     │
                │   and their order    │
                │                      │
                └──────────┬───────────┘
                           │
              ┌────────────▼────────────┐
              │ StructuredGradleFile    │
              │                         │
              │ • parse(String)         │
              │ • render(ParsedContent) │
              │ • blocks()              │
              │ • blockOrder()          │
              └────────────┬────────────┘
                           │
                           │ uses
                           │
              ┌────────────▼────────────┐
              │        Block            │
              │      (interface)        │
              │                         │
              │ • name()                │
              │ • pattern()             │
              │ • render()              │
              │ • edit(editor)          │
              └────────────┬────────────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
    ┌────▼────┐   ┌────────▼────────┐  ┌────▼────────┐
    │Closure  │   │  NestedClosure  │  │  Property   │
    │ Block   │   │     Block       │  │   Block     │
    │         │   │                 │  │             │
    │ Simple  │   │   Composite     │  │  Non-block  │
    │ blocks  │   │   with child    │  │   patterns  │
    │         │   │   blocks        │  │             │
    └─────────┘   └─────────────────┘  └─────────────┘
         │                 │                 │
         │                 │                 │
    Examples:         Examples:          Examples:
    • plugins         • buildscript       • version
    • repositories    • configurations    • group
    • dependencies    • pluginManagement
    • allprojects

                           │
              ┌────────────▼────────────┐
              │      GradleBlock        │
              │    (typed wrapper)      │
              │                         │
              │ • append(text)          │
              │ • prepend(text)         │
              │ • overwrite(text)       │
              │ • edit(editor)          │
              └─────────────────────────┘
```

## Key Design Principles

### 1. Self-Contained Blocks

Each block type is a self-contained record that knows:
- Its name (e.g., "plugins", "repositories")
- Its regex pattern for parsing
- How to render itself
- How to edit its content

**Example:**
```java
public record ClosureBlock(String name, String content) implements Block {
    public static ClosureBlock empty(String name) {
        return new ClosureBlock(name, "");
    }

    @Override
    public Pattern pattern() {
        return Pattern.compile(
            "^\\s*" + Pattern.quote(name) + "\\s*\\{([^{}]*)\\}",
            Pattern.MULTILINE | Pattern.DOTALL);
    }
}
```

### 2. Intelligent Block Ordering

Blocks are always rendered in a predefined order, regardless of the order they're edited:

**BuildGradleFile order:**
1. `buildscript { ... }`
2. `plugins { ... }`
3. `allprojects { ... }`
4. `subprojects { ... }`
5. `repositories { ... }`
6. `dependencies { ... }`
7. `configurations { ... }`
8. Unrecognized content (preserved at end)

**Example:**
```java
// User edits in random order
buildGradle.dependencies().append("...");
buildGradle.plugins().append("...");
buildGradle.repositories().append("...");

// Output maintains correct order
plugins { ... }
repositories { ... }
dependencies { ... }
```

### 3. Parse → Edit → Render Cycle

Every edit operation follows this cycle:

```
1. Parse current file content
   ├─> Extract known blocks using regex patterns
   ├─> Capture unrecognized content
   └─> Build ParsedContent structure

2. Edit the target block
   ├─> Navigate to block (possibly nested)
   ├─> Apply editor function to block content
   └─> Create updated Block with new content

3. Update ParsedContent
   └─> Replace old block with updated block

4. Render back to string
   ├─> Render blocks in defined order
   ├─> Apply proper indentation
   ├─> Append unrecognized content
   └─> Write to file
```

### 4. Nested Block Support

Blocks can contain child blocks, enabling deep nesting:

```java
NestedClosureBlock buildscript = NestedClosureBlock.empty(
    "buildscript",
    ClosureBlock.empty("repositories"),
    ClosureBlock.empty("dependencies"),
    ClosureBlock.empty("plugins")
);

// Usage
buildGradle.buildscript().repositories().append("mavenCentral()");
```

**Rendering:**
```gradle
buildscript {
    repositories {
        mavenCentral()
    }
}
```

### 5. Merging Raw Appends

When raw content containing blocks is appended to the file, the system intelligently merges it:

```java
// Existing content
buildGradle.plugins().append("id 'java'");

// Raw append with plugins block
buildGradle.append("""
    plugins {
        id 'maven-publish'
    }
    repositories {
        mavenCentral()
    }
    """);

// Result: merged into existing blocks in correct order
plugins {
    id 'java'
    id 'maven-publish'
}

repositories {
    mavenCentral()
}
```

## Data Flow Example

### Scenario: Adding a plugin to an existing file

```
1. Initial State:
   File: "repositories {\n    mavenCentral()\n}\n"

2. User Action:
   buildGradle.plugins().append("id 'java'")

3. Parse Phase:
   ParsedContent {
     blocks: {
       "repositories": ClosureBlock("repositories", "mavenCentral()")
     }
     unrecognized: ""
   }

4. Edit Phase:
   - Navigate to "plugins" block
   - Block not found in parsed content
   - Get empty template: ClosureBlock("plugins", "")
   - Apply edit: ClosureBlock("plugins", "id 'java'")

5. Update Phase:
   ParsedContent {
     blocks: {
       "plugins": ClosureBlock("plugins", "id 'java'"),
       "repositories": ClosureBlock("repositories", "mavenCentral()")
     }
     unrecognized: ""
   }

6. Render Phase:
   - blockOrder = ["plugins", "repositories", ...]
   - Render "plugins": "plugins {\n    id 'java'\n}"
   - Render "repositories": "repositories {\n    mavenCentral()\n}"
   - Join with blank lines

7. Final Output:
   plugins {
       id 'java'
   }

   repositories {
       mavenCentral()
   }
```

## Implementation Details

### Block Types

#### ClosureBlock (Simple)
- Used for blocks without child blocks
- Pattern matches: `name { content }`
- Examples: `plugins`, `repositories`, `dependencies`

#### NestedClosureBlock (Composite)
- Contains ordered child blocks
- Recursively parses and renders children
- Examples: `buildscript`, `configurations`, `pluginManagement`

#### PropertyBlock
- For non-closure patterns
- Examples: version assignments, group declarations
- Currently unused but available for extension

### Unrecognized Content

Content that doesn't match any block pattern is preserved:
- Tasks configurations
- Custom methods
- Comments outside blocks
- Property assignments

This content is always rendered at the end after all structured blocks.

### Indentation

Blocks are indented using 4 spaces per level:
```gradle
plugins {
    id 'java'
}

configurations {
    all {
        resolutionStrategy {
            force 'com.google:guava:32.0'
        }
    }
}
```

## Benefits Over Previous Design

### Before (Template-based)
- 16+ files: BlockType interfaces, Template builders, BlockHandles, typed wrappers
- Complex builder pattern for defining structure
- Separation between block definition and implementation
- Difficult to add new block types

### After (Self-contained)
- 6 files: Block interface, 3 implementations, ParsedContent, GradleBlock wrapper
- Blocks know their own patterns and behavior
- Single unified wrapper for all block types
- Easy to add new blocks: create a `Block` constant

### Lines of Code Reduction
- Deleted: ~800 lines
- Simplified: ~400 lines
- Net reduction: ~60% less code

## Usage Examples

### Simple Block Editing
```java
buildGradle.plugins().append("id 'java'");
buildGradle.repositories().append("mavenCentral()");
```

### Nested Block Editing
```java
buildGradle.buildscript().repositories().append("gradlePluginPortal()");
buildGradle.configurations().all().resolutionStrategy().append("force 'x:y:1.0'");
```

### Raw Content with Blocks
```java
buildGradle.append("""
    plugins {
        id 'java'
    }

    version = '1.0.0'
    """);
// plugins block goes to correct position, version goes to unrecognized
```

### Block Operations
```java
// Append
buildGradle.dependencies().append("implementation 'junit:junit:4.13'");

// Prepend
buildGradle.repositories().prepend("mavenLocal()");

// Overwrite
buildGradle.plugins().overwrite("id 'application'");

// Edit with function
buildGradle.plugins().edit(content ->
    content.replace("java", "java-library"));
```

## Extension Points

### Adding a New Block Type

1. Add block constant to BuildGradleFile/SettingsGradleFile:
```java
private static final Block PUBLISHING = ClosureBlock.empty("publishing");
```

2. Add to blocks() list and blockOrder():
```java
@Override
protected List<Block> blocks() {
    return List.of(..., PUBLISHING);
}

@Override
protected List<String> blockOrder() {
    return List.of(..., "publishing");
}
```

3. Add accessor method:
```java
public GradleBlock publishing() {
    return new GradleBlock(this, "publishing");
}
```

### Adding a New Block Implementation

Implement the `Block` interface:
```java
public record CustomBlock(String name, String content) implements Block {
    @Override
    public Pattern pattern() {
        // Your pattern
    }

    @Override
    public String render() {
        // Your rendering logic
    }
}
```

## Testing Strategy

Tests verify:
1. **Block ordering** - blocks appear in correct order regardless of edit order
2. **Merging** - multiple edits to same block accumulate properly
3. **Nesting** - deeply nested blocks work correctly
4. **Preservation** - unrecognized content is preserved
5. **Raw appends** - raw content with blocks is parsed and placed correctly
6. **Operations** - append/prepend/overwrite/edit all work as expected

See `BuildGradleFileTest.java` for comprehensive test suite.
