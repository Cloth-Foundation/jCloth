# Cloth — Language Design Notes

> This document describes the *intent* and *direction* of Cloth.
> It is not a frozen specification; it exists to guide decisions as the language evolves.

---

## 1. Overview

Cloth is a **low-level, compiled, object-oriented systems programming language**.

It is designed to combine:

* The **clarity and structure of Java**
* The **control and performance of C**
* Without the complexity, inconsistency, and historical baggage of C++

Cloth is intended to produce **fast native binaries** capable of running close to bare metal, while maintaining a readable and maintainable codebase suitable for long-lived systems.

---

## 2. Primary Goals

Cloth aims to make it easy to build:

* Operating system components
* Game engines and rendering systems
* Infrastructure services
* Embedded and robotics software
* High-performance backend systems

Cloth is **not focused on**:

* Web application frameworks
* Scripting environments
* Rapid prototyping languages

It is a systems language first.

---

## 3. Core Philosophy

### 3.1 Readability is a Systems Feature

Cloth assumes code will be maintained for years by teams, not individuals.

The language prioritizes:

* Explicit structure
* Clear ownership
* Predictable behavior
* Minimal stylistic variance

There should be **one obvious way** to write common constructs.

---

### 3.2 Structure Over Cleverness

Cloth intentionally enforces structure:

* Code lives in classes.
* Entry points are explicit.
* Overrides must be declared.
* Visibility is clearly scoped.
* Memory ownership is visible in types.

The goal is to reduce architectural drift and make large systems understandable.

---

### 3.3 Native Compilation

Cloth compiles directly to native code.

There is no mandatory virtual machine.

The language must be capable of:

* Running without a runtime
* Interfacing with hardware
* Being used in kernels or embedded targets

A runtime may exist, but it is optional.

---

### 3.4 Manual Memory by Default

Cloth uses **manual memory management** as its primary model.

This avoids:

* Hidden allocations
* Runtime unpredictability
* Mandatory garbage collection

Example:

```cloth
var dog: Dog = new Dog();
delete dog;
```

This behavior is explicit and deterministic.

---

### 3.5 Optional Managed Runtime

Cloth may support an optional runtime with garbage collection, but this is a *mode*, not a requirement.

The language must function fully without it.

The runtime should be removable for low-level targets.

---

## 4. Ownership Model

Cloth distinguishes between **owning objects** and **non-owning references** without introducing borrow-checking complexity.

| Form            | Meaning                               |
| --------------- | ------------------------------------- |
| `Dog`           | Owns memory; responsible for deletion |
| `ref Dog`       | Non-owning alias                      |
| `const ref Dog` | Read-only alias                       |

References exist to express intent, not to enforce lifetimes.

Cloth does **not** attempt Rust-style compile-time memory proofs.

---

## 5. Object-Oriented Model

Cloth is class-based and supports:

* Single inheritance
* Explicit overrides
* Final methods and classes
* Nested static classes
* Primary constructors declared in class headers

Example:

```cloth
class Animal(food: string) {
    func eat(): void {
        println("Eating " + this.food);
    }
}
```

Constructor parameters become private immutable fields automatically.

---

## 6. Visibility Model

Cloth supports both inline visibility modifiers and visibility blocks.

```cloth
public {
    func run(): void { }
}

private func helper(): void { }
```

Visibility is explicit and never inferred from context.

---

## 7. Entry Point Model

Programs begin execution at a `static func main` defined inside a class.

The class name is not significant.

```cloth
class Bootstrap {
    public {
        static func main(args: string[]): i32 {
            return 0;
        }
    }
}
```

This avoids special-case "magic" classes.

---

## 8. Design Constraints

To remain consistent with Cloth’s goals, new features should:

* Improve clarity of large systems
* Reduce ambiguity in behavior
* Avoid introducing multiple competing paradigms
* Remain analyzable by tooling
* Avoid hidden runtime behavior

Features that primarily increase expressiveness without improving maintainability are discouraged.

---

## 9. What Cloth Is Not Trying To Be

Cloth is not:

* A functional language
* A metaprogramming-heavy language
* A replacement for scripting environments
* A language designed for maximum abstraction flexibility

It is intentionally opinionated.

---

## 10. Guiding Principle

If a feature makes code easier to understand five years later, it belongs in Cloth.

If it only makes code shorter today, it probably does not.