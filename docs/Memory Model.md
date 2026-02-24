# Cloth — Memory Model

> This document describes the memory model philosophy and the intended semantics for allocation, ownership, and object/reference behavior in Cloth.
>
> Cloth is a **manual-memory, compiled, object-oriented systems language**. An optional managed runtime (GC/VM) may exist, but **manual memory is the primary model** and must remain fully supported.

---

## 1. Goals

The Cloth memory model is designed to provide:

- **Explicit control** over allocation and deallocation
- **Predictable performance** and deterministic behavior
- **Readable ownership intent** in large codebases
- A standardized approach that avoids the “many ways to do it” problem seen in C++

Cloth prioritizes **clarity and maintainability** over compile-time memory proofs.

---

## 2. Non-Goals

Cloth intentionally does **not** attempt to be:

- A borrow-checked language (not Rust)
- A fully automatic memory-managed language by default (not Java)
- A language with multiple competing memory paradigms

Cloth does not require compile-time lifetime tracking to be correct. Instead, it emphasizes explicit ownership and standardized patterns.

---

## 3. Allocation Model

### 3.1 Stack vs Heap

- **Stack allocation** is used for normal local values (implementation-defined layout).
- **Heap allocation** is explicit via `new`.

Heap allocation example:

```cloth
var dog: Dog = new Dog();
````

### 3.2 Deallocation

Heap memory is released explicitly via `delete`.

```cloth
delete dog;
```

Deallocation is deterministic and immediate.

---

## 4. Ownership

Cloth distinguishes between **owning objects** and **non-owning references**.

### 4.1 Owning Objects

A variable of type `T` (e.g. `Dog`) represents an **owning handle** to a heap-allocated object created via `new`.

```cloth
var dog: Dog = new Dog();
```

Owning variables are responsible for deallocation.

```cloth
delete dog;
```

### 4.2 Non-Owning References

A `ref T` is a **non-owning alias** to an object managed elsewhere.

```cloth
var dog: Dog = new Dog();
var view: ref Dog = dog;
```

A `ref`:

* May read/write the object
* **Must not** deallocate the object
* Does not imply lifetime guarantees (no borrow checking)

Attempting to delete a `ref` is illegal:

```cloth
delete view; // error: cannot delete a non-owning reference
```

### 4.3 Read-Only References

`const ref T` is a read-only alias.

```cloth
func printDog(d: const ref Dog): void {
    println(d.name);
}
```

---

## 5. Copies, Assignment, and Aliasing

Cloth is designed to avoid accidental “double-owner” bugs.

The language must define **one** consistent rule for assignment of owning types.

### 5.1 Recommended Rule (Design Direction)

**Owning types are non-copyable by default.**

This prevents:

* Double frees
* Accidental shared ownership without intent
* Silent expensive deep copies

Example:

```cloth
var a: Dog = new Dog();
var b: Dog = a; // error: owning types cannot be copied
```

To transfer ownership, Cloth may use an explicit mechanism (to be specified), such as:

* `move a`
* `take(a)`
* `a.transfer()`

(Exact syntax intentionally deferred until the ownership transfer model is finalized.)

### 5.2 Reference Assignment

References may be copied freely:

```cloth
var a: Dog = new Dog();
var r1: ref Dog = a;
var r2: ref Dog = r1; // ok
```

---

## 6. Nullability and Pointers

Cloth supports explicit nullability using `?`.

```cloth
var dog: Dog? = null;
```

Nullability is a **type property**, not a value suffix.

---

## 7. Object Lifetime

### 7.1 Deterministic Destruction

When `delete` is invoked:

* The object’s destructor/finalizer behavior (if any) runs deterministically
* Memory is released immediately

(Exact destructor semantics and naming are a separate specification concern.)

### 7.2 Dangling References

Because Cloth is manual-memory by default, it is possible to create dangling references:

```cloth
var dog: Dog = new Dog();
var r: ref Dog = dog;

delete dog;
r.bark(); // undefined behavior or runtime trap depending on build mode
```

Cloth may offer optional debug/runtime modes to detect use-after-free, but correctness is ultimately the developer’s responsibility in manual mode.

---

## 8. Optional Managed Runtime (GC Mode)

Cloth may support an optional managed runtime that provides garbage collection.

### 8.1 Design Intent

* GC mode should not change the surface language drastically.
* The same code should largely compile in manual mode and GC mode.
* GC mode is intended for productivity where deterministic destruction is not required.

### 8.2 Expected Differences (High-Level)

In GC mode:

* `new` allocates managed objects
* `delete` may be disallowed or treated as a no-op (to be decided)
* Use-after-free becomes less likely but not impossible in all cases (e.g. external resources)

Exact semantics are deferred until the runtime strategy is chosen.

---

## 9. Resource Management Beyond Memory

Cloth must provide a consistent pattern for non-memory resources (files, sockets, locks).

Design direction:

* Prefer explicit cleanup patterns
* Avoid implicit RAII magic
* Consider a structured cleanup mechanism (e.g., `defer`), to reduce leak risk in manual mode

This is a future-focused section; syntax is not finalized.

---

## 10. Summary

Cloth’s memory model is built on:

* Manual allocation (`new`)
* Manual deallocation (`delete`)
* Clear ownership (`T` owns, `ref T` borrows)
* No borrow checker
* Standardized behavior to keep codebases consistent and readable

Cloth aims to provide low-level control with enterprise readability, without inheriting C++’s complexity or Rust’s cognitive overhead.
