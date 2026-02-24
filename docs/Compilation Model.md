# Cloth — Compilation Model

> This document describes how Cloth code is expected to compile, link, and run.
>
> Cloth is a **compiled, object-oriented systems language** intended to produce native binaries capable of running close to bare metal. A runtime/VM may exist, but it must be **optional** and must not be required for all targets.

---

## 1. Goals

The Cloth compilation model is designed to provide:

- **Native code generation** (machine code / assembly output)
- **Predictable builds** suitable for enterprise CI/CD
- **Tooling-friendly** intermediate representations and diagnostics
- The ability to target:
  - Desktop/server platforms
  - Embedded environments
  - Bare-metal kernels and bootable images (future scope)

Cloth emphasizes deterministic compilation and clear boundaries.

---

## 2. Non-Goals

Cloth is not designed around:

- Mandatory VM execution (not JVM-first)
- Just-in-time compilation as the primary path
- Heavy runtime reflection requirements
- Build systems that require complex global state or “magic” conventions

---

## 3. Compilation Pipeline

A typical Cloth compilation pipeline:

1. **Lexing**
2. **Parsing**
3. **Semantic analysis**
   - name resolution
   - type checking
   - visibility checks
   - override/final checks
4. **IR generation**
5. **Optimization (optional)**
6. **Code generation**
   - assembly output (optional artifact)
   - object files
7. **Linking**
   - native executable / library
8. **Packaging (optional)**
   - static binary
   - shared library
   - embedded image / kernel artifact

This pipeline is intended to work both with and without an optional runtime.

---

## 4. Compilation Units and Modules

### 4.1 Modules

Cloth source files declare a module:

```cloth
module cloth.math;
````

A module is a *logical namespace* and a *visibility boundary* (for `internal`).

### 4.2 Compilation Units

A compilation unit is typically:

* a single `.cloth` file, or
* a group of source files compiled together as one target

The build system determines the target boundary.

### 4.3 Imports

Imports reference modules:

```cloth
import std.io;
```

Imports affect name resolution only; they do not implicitly link binaries unless specified by the build configuration.

---

## 5. Targets and Artifacts

Cloth supports multiple output artifact types:

* **Executable**
* **Static library**
* **Shared library**
* (future) **Kernel / bare metal image**

Each build target specifies:

* target triple (platform/arch/abi)
* optimization level
* runtime mode (manual/GC)
* debug vs release instrumentation
* link mode (static vs dynamic)

---

## 6. Entrypoint Selection

### 6.1 Required Entrypoint Form

A Cloth program begins execution at a method named `main`.

Rules:

1. `main` must be declared inside a class.
2. The class name is not significant.
3. `main` must be `static`.
4. Exactly one valid `main` must exist per executable target.

Allowed signatures:

```cloth
static func main(): void
static func main(): i32
static func main(args: string[]): void
static func main(args: string[]): i32
```

If multiple valid entrypoints exist, compilation fails with an error.

If no valid entrypoint exists for an executable target, compilation fails with an error.

### 6.2 Libraries

Library targets do not require an entrypoint.

---

## 7. Separate Compilation and Linking

Cloth is intended to support conventional separate compilation:

* Each compilation unit produces an **object file** (or equivalent).
* A linker combines object files into the final artifact.

### 7.1 ABI Stability

To support clean interop and consistent builds, Cloth should define:

* calling convention rules for functions/methods
* object layout rules (especially for inheritance)
* vtable/interface table layout (if used)
* name mangling conventions
* symbol visibility rules

ABI rules must be stable across compiler versions where possible, or versioned explicitly.

---

## 8. Optional Runtime / VM Integration

Cloth may optionally include a runtime/VM that provides:

* garbage collection (GC)
* managed heap
* runtime metadata / reflection support (optional)
* debugging hooks

However:

* The runtime must be **optional**
* The compiler must be able to generate binaries that:

    * statically link runtime components, or
    * omit runtime entirely (manual memory mode)
* Code generation must not assume the runtime exists unless enabled

Runtime mode is a build configuration decision.

---

## 9. Build Modes

Cloth should support build modes that affect compilation and diagnostics.

### 9.1 Debug Mode

* full debug symbols
* runtime checks if enabled (bounds checks, use-after-free checks, etc.)
* minimal optimization for easier stepping

### 9.2 Release Mode

* aggressive optimization
* optional LTO
* fewer runtime checks by default
* deterministic behavior maintained

---

## 10. Diagnostics Model

Cloth compiler diagnostics must be:

* precise and location-aware
* stable and scriptable (machine-readable option)
* structured (category, severity, code, message)

Diagnostics must support enterprise-grade CI.

---

## 11. FFI and Interoperability (Design Direction)

As a systems language, Cloth must support FFI.

The compilation model should allow:

* calling C functions
* exporting Cloth functions with a stable ABI
* linking against external libraries
* controlling calling conventions and symbol visibility

Exact syntax for FFI is not defined here, but the compilation model assumes FFI will be a first-class capability.

---

## 12. Summary

Cloth is designed to compile through a conventional systems pipeline:

* source → AST → semantics → IR → native code → link

It supports:

* modules and imports
* native executables and libraries
* predictable entrypoint discovery (`static func main`)
* optional runtime integration
* enterprise-grade diagnostics and tooling

The compilation model is intended to enable Cloth to scale from embedded/bare metal to large server systems without requiring a mandatory VM
