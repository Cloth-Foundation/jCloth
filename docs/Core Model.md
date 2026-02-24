# Cloth Language Design — Core Model

## 1. Philosophy

Cloth is a **formal, enterprise-oriented systems language** designed for:

* Clear, readable structure suitable for large teams
* Predictable object-oriented design (Java-like class model)
* Minimal syntactic noise
* Explicit lifecycle and visibility rules
* Strong compile-time determinism (no implicit runtime magic)

Cloth favors **clarity over cleverness** and **structure over brevity**.

---

## 2. Program Structure

Every Cloth source file belongs to a module.

```cloth
module cloth;

import std.io;
```

* `module` defines the compilation namespace.
* `import` brings other modules into scope.

---

## 3. Classes as the Primary Organizational Unit

All executable logic must live inside a class.

Cloth follows a class-centric design similar to Java, but without forcing a specific class name or inheritance model.

```cloth
public class Example {
    public {
        func doWork(): void {
            println("Working");
        }
    }
}
```

---

## 4. Visibility

Cloth supports defining visibility using **visibility blocks** as well as **inline visibility modifiers**.

Both forms are semantically equivalent.
Visibility blocks are preferred when declaring multiple members with the same access level, while inline modifiers are useful for single declarations or overrides.

---

### 4.1 Visibility Levels

| Keyword    | Meaning                                    |
| ---------- | ------------------------------------------ |
| `public`   | Accessible outside the module              |
| `internal` | Accessible within the module               |
| `private`  | Accessible only within the declaring class |

---

### 4.2 Visibility Blocks (Recommended for Grouping)

Visibility blocks apply a visibility label to all enclosed declarations.

```cloth
public {
    func api(): void { }
    var version: string = "1.0";
}

private {
    func helper(): void { }
}
```

**Properties**

* Visibility blocks **do not create scope**.
* They cannot be nested.
* They may only appear at class level.
* They improve readability by avoiding repeated modifiers.

---

### 4.3 Inline Visibility Modifiers

Visibility may also be declared inline for individual members.

```cloth
public func api(): void { }

private var cache: Map<string, string>;

internal static func bootstrap(): void { }
```

Inline modifiers are especially useful when:

* A single declaration differs from its surrounding block
* Defining small utility classes
* Overriding visibility intentionally

---

### 4.4 Mixing Both Forms

Inline modifiers override the surrounding block when both are used.

```cloth
public {
    func visible(): void { }

    private func hidden(): void { }  // Explicit override
}
```

---

### 4.5 Default Visibility

If no visibility is specified, the default is:

> `internal`

```cloth
func helper(): void { }   // implicitly internal
```

---

## 5. Fields

Field declarations follow a clear name–type structure:

```cloth
var counter: i32 = 0;
final var id: string = "abc";
```

### Binding Keywords

| Keyword     | Meaning                       |
| ----------- | ----------------------------- |
| `var`       | Mutable field                 |
| `final var` | Assigned once, then immutable |
| `const`     | Compile-time constant         |

---

## 6. Type Qualifiers

Qualifiers modify storage semantics:

```cloth
var x: atomic long? = 10;
```

Currently supported:

| Qualifier | Meaning                 |
| --------- | ----------------------- |
| `atomic`  | Atomic memory semantics |

---

## 7. Nullability

Nullability is expressed as a type suffix:

```cloth
var name: string? = null;
```

* `T?` means the value may be `null`.
* Nullability is part of the type system, not the value syntax.

---

## 8. Primary Constructors

Classes declare their constructor directly in the class header:

```cloth
class Animal(food: string) { ... }
```

### Behavior

Primary constructor parameters automatically become:

> **Private, immutable fields of the same name.**

So:

```cloth
class Animal(food: string)
```

implicitly defines:

```cloth
private final var food: string;
```

This removes boilerplate while preserving explicit semantics.

---

## 9. Nested Classes

Classes may be nested.

```cloth
static class Dog: Animal("Kibble") { ... }
```

### `static class`

A `static class`:

* Does not capture an instance of the enclosing class.
* Behaves like a normal top-level type scoped inside another class.

---

## 10. Inheritance Model

Cloth supports:

* **Single inheritance**
* Multiple interfaces (future feature)

```cloth
class Dog: Animal("Kibble") { }
```

---

## 11. Method Semantics

Methods are virtual (overridable) by default.

### Overriding Requires Explicit Intent

```cloth
override func eat(): void { ... }
```

Failing to mark an override is a compile-time error.

---

## 12. Final Methods and Classes

The `final` modifier prevents extension or overriding.

```cloth
final func increaseHappiness(): void { }
final class ImmutableConfig { }
```

This ensures behavioral guarantees required in enterprise systems.

---

## 13. Entry Point

A Cloth program starts execution at a method named `main`.

### Rules

1. `main` must be declared **inside a class**.
2. The class name is not significant.
3. `main` must be `static`.
4. Exactly one valid `main` must exist per program.

### Valid Signatures

```cloth
static func main(): void
static func main(): i32
static func main(args: string[]): void
static func main(args: string[]): i32
```

### Example

```cloth
public class Main(args: string[]) {

    public {
        func run(): i32 {
            println("Hello, world!");
            return 0;
        }
    }

    public {
        static func main(args: string[]): i32 {
            var app = Main(args);
            return app.run();
        }
    }
}
```

### Rationale

This model:

* Keeps constructors focused on state initialization
* Makes startup explicit and tool-friendly
* Avoids hidden runtime behavior

---

## 14. Example Putting It All Together

```cloth
module cloth;

import std.io;

public class Main(args: string[]) {

    public {
        var x: atomic long? = 10;
        var arguments: string[] = args;

        static class Animal(food: string) {

            private {
                var happiness: f32 = 100.0;

                final func increaseHappiness(amount: f32): void {
                    this.happiness += amount;
                    println("Happiness increased to " + this.happiness);
                }
            }

            func eat(): void {
                println("Eating " + this.food);
            }
        }

        static class Dog: Animal("Kibble") {

            func bark(): void {
                println("Woof!");
            }

            override func eat(): void {
                println("The dog is eating " + this.food);
            }
        }
    }

    private {
        func run(): i32 {
            println("Hello, world! " + this.x);
            return 0;
        }
    }

    public {
        static func main(args: string[]): i32 {
            var app = Main(args);
            return app.run();
        }
    }
}
```

---

## 15. Design Principles Summary

Cloth enforces a strong separation of concerns:

| Concern          | Mechanism                     |
| ---------------- | ----------------------------- |
| Organization     | Classes                       |
| Visibility       | Visibility blocks             |
| Construction     | Primary constructors          |
| Mutability       | `var` / `final var` / `const` |
| Inheritance      | Single inheritance            |
| Override control | `override` / `final`          |
| Memory semantics | Type qualifiers (`atomic`)    |
| Nullability      | `?`                           |
| Execution start  | `static func main`            |
