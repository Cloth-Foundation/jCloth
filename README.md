# Cloth

**A low-level, compiled, object-oriented systems programming language**

Cloth is designed to combine the **clarity and structure of Java** with the **control and performance of C**, without the complexity, inconsistency, and historical baggage of C++.

---
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/Cloth-Foundation/jCloth/gradle.yml?style=for-the-badge)
![GitHub License](https://img.shields.io/github/license/Cloth-Foundation/jCloth?style=for-the-badge)
![GitHub Repo stars](https://img.shields.io/github/stars/Cloth-Foundation/jCloth?style=for-the-badge)
![GitHub forks](https://img.shields.io/github/forks/Cloth-Foundation/jCloth?style=for-the-badge)
![GitHub contributors](https://img.shields.io/github/contributors/Cloth-Foundation/jCloth?style=for-the-badge)
![GitHub repo size](https://img.shields.io/github/repo-size/Cloth-Foundation/jCloth?style=for-the-badge)

---

## What is Cloth?

Cloth is a systems programming language intended to produce fast native binaries capable of running close to bare metal, while maintaining a readable and maintainable codebase suitable for long-lived systems.

The language prioritizes **predictability, explicitness, and maintainability** over terseness and convenience. It's built for teams working on large-scale systems that need to remain comprehensible and reliable over years of development.

---

## Core Philosophy

### Structure Over Cleverness

Cloth intentionally enforces structure to reduce architectural drift and make large systems understandable:

- Code lives in classes
- Entry points are explicit
- Overrides must be declared
- Visibility is clearly scoped
- Memory ownership is visible in types

### Readability as a Systems Feature

Cloth assumes code will be maintained for years by teams, not individuals. The language is designed so there's **one obvious way** to write common constructs, minimizing stylistic variance and maximizing clarity.

### Explicit Over Implicit

Allocation, initialization, and visibility are directly visible in source code. Features that require hidden allocations, hidden threads, or hidden global initialization are not part of the language.

### Native Compilation

Cloth compiles directly to native code. There is no mandatory virtual machine or runtime. The language must be capable of running without a runtime, interfacing with hardware, and being used in kernels or embedded targets.

---

## Design Principles

### Manual Memory by Default

Cloth uses **manual memory management** as its primary model, avoiding hidden allocations, runtime unpredictability, and mandatory garbage collection:

```cloth
var dog: Dog = new Dog();
delete dog;
```

An optional managed runtime with garbage collection may be supported as a *mode*, but the language functions fully without it.

### Ownership Without Borrow Checking

Cloth distinguishes between **owning objects** and **non-owning references** without introducing borrow-checking complexity:

```cloth
var pet: Dog           // Owns memory; responsible for deletion
var alias: ref Dog     // Non-owning alias
```

References exist to express intent, not to enforce compile-time lifetime proofs.

### Class-Based Object Model

Cloth is class-based and supports:

- Single inheritance
- Explicit overrides (`override` keyword required)
- Final methods and classes
- Nested static classes
- Primary constructors declared in class headers

```cloth
class Animal(food: string) {
    func eat(): void {
        println("Eating " + this.food);
    }
}
```

Constructor parameters become private immutable fields automatically.

### Explicit Visibility

Visibility is never inferred from context. Cloth supports both inline modifiers and visibility blocks:

```cloth
public {
    func run(): void { }
}

private func helper(): void { }
```

### Clear Entry Points

Programs begin execution at a `static func main` defined inside a class—no magic class names required:

```cloth
class Bootstrap {
    public {
        static func main(args: string[]): i32 {
            return 0;
        }
    }
}
```

---

## What Cloth Is For

Cloth aims to make it easy to build:

- Operating system components
- Game engines and rendering systems
- Infrastructure services
- Embedded and robotics software
- High-performance backend systems

Cloth is **not focused on**:

- Web application frameworks
- Scripting environments
- Rapid prototyping
- Functional programming paradigms
- Heavy metaprogramming

It is a systems language first.

---

## Language Features

Cloth supports (or will support) the following features:

### Type System
- Built-in primitive types (`i8`, `i16`, `i32`, `i64`, `u8`, `u16`, `u32`, `u64`, `f32`, `f64`, `bool`, `char`, `string`)
- Nullable types with explicit `?` syntax
- Arrays and user-defined types (classes, interfaces)
- Type qualifiers (e.g., `atomic`)

### Memory Management
- Explicit allocation with `new`
- Explicit deallocation with `delete`
- Ownership tracking with `ref` for non-owning references
- Optional garbage collection mode (future)

### Object-Oriented Features
- Single inheritance
- Abstract classes and interfaces
- Method overriding with explicit `override` keyword
- `final` methods and classes
- Primary constructors in class headers
- Static and instance members
- Nested types

### Module System
- Explicit module declarations
- Import statements for dependency management
- Visibility control (`public`, `internal`, `private`)

### Meta Programming
- Meta keywords (`SIZEOF`, `TYPEOF`, `LENGTH`, `MAX`, `MIN`, etc.)
- Compile-time introspection capabilities

---

## Guiding Principle

> **If a feature makes code easier to understand five years later, it belongs in Cloth.**
>
> **If it only makes code shorter today, it probably does not.**

New features are evaluated against whether they improve clarity of large systems, reduce ambiguity in behavior, and remain analyzable by tooling—not just whether they increase expressiveness.

---

## Project Status

**Cloth is currently in early development**

The project is actively being built and is not yet ready for production use. Current focus areas include:

The language specification is evolving, and breaking changes should be expected.

---

## Building

Cloth is implemented in Java and uses Gradle as its build system.

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test
```

---

## Contributing

As the language is in early development, contribution guidelines are still being established. If you're interested in contributing or following the project's progress, please check back as the project matures.

---

## Learn More

For detailed information about the language design and specification, see the documentation in the `docs/` directory:

- [Design.md](docs/Design.md) — Language design philosophy and goals
- [Core Model.md](docs/Core%20Model.md) — Program structure and object model
- [Memory Model.md](docs/Memory%20Model.md) — Memory management semantics
- [spec/Outline.md](docs/spec/Outline.md) — Formal language specification (draft)
