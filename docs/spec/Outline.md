# Cloth Language Specification (Draft)

> Status: Draft / evolving  
> This document defines the normative behavior of the Cloth language.
> Where behavior is not yet finalized, sections are marked **(TBD)**.

---

## Table of Contents

1. Introduction
   1. Goals and Non-Goals
   2. Conformance Language (MUST/SHOULD/MAY)
   3. Terminology

2. Lexical Structure (TBD)
   1. Source Text and Encoding
   2. Tokens
   3. Meta Tokens
   4. Comments and Whitespace
   5. Identifiers
   6. Keywords
   7. Meta Keywords
   8. Literals

3. Program Structure
   1. Modules
   2. Imports
   3. Compilation Units
   4. Visibility and Access Control

4. Types
   1. Built-in Primitive Types
   2. Nullability (`?`)
   3. Arrays
   4. User-defined Types (Classes, Interfaces)
   5. Type Qualifiers (e.g., `atomic`)
   6. Type Conversions (TBD)

5. Declarations
   1. Class Declarations
   2. Primary Constructors
   3. Field Declarations
   4. Method Declarations
   5. Nested Types
   6. Static Members
   7. Constants (`final` vs `const`)

6. Statements and Expressions (TBD)
   1. Blocks
   2. Control Flow (`if`, `while`, `for`, `return`)
   3. Assignment
   4. Calls
   5. Operators
   6. String concatenation

7. Object Model
   1. `this`
   2. Inheritance
   3. Overriding (`override`)
   4. Final members (`final`)
   5. Dynamic Dispatch (TBD)

8. Memory Model
   1. Allocation (`new`)
   2. Deallocation (`delete`)
   3. Ownership and `ref`
   4. (Optional) GC Mode (TBD)

9. Entrypoint
   1. `main` selection
   2. Valid signatures
   3. Errors

10. Standard Library Surface (Non-normative) (TBD)

---

## 1. Introduction

### 1.1 Goals and Non-Goals

This specification defines the source-level behavior of the Cloth programming language.

Cloth is a low-level, compiled, object-oriented systems language intended to produce native binaries.

The goals of Cloth include:

* Predictability: the language SHOULD favor deterministic behavior over implicit convenience.
* Explicitness: allocation, initialization, and visibility SHOULD be directly visible in source.
* Tooling: programs SHOULD be easy for compilers and analysis tools to reason about.
* Maintainability: programs SHOULD encourage structured code and discourage ambiguous constructs.
* Diagnostics: errors SHOULD be described precisely with accurate source locations.

The non-goals of Cloth include:

* Minimizing syntax: Cloth is not designed to minimize tokens or maximize terseness.
* Implicit runtime services: features that require hidden allocations, hidden threads, or hidden global initialization are not assumed.
* Unconstrained metaprogramming: reflection and meta facilities, when present, are intended to be bounded and tool-friendly.

This specification is written in a normative style:

* If a behavior is specified using MUST/MUST NOT/SHOULD/SHOULD NOT/MAY, implementations and programs are expected to follow it.
* Where behavior is not yet finalized, the text will be marked `(TBD)`.

### 1.2 Conformance Language

The keywords **MUST**, **MUST NOT**, **SHOULD**, **SHOULD NOT**, and **MAY** in this specification are to be interpreted as described in RFC 2119.

Interpretation guidance:

* **MUST**: required for all conforming implementations and all conforming programs.
* **MUST NOT**: forbidden.
* **SHOULD**: required unless there is a compelling reason to deviate; deviations SHOULD be rare and documented.
* **SHOULD NOT**: discouraged; permitted only with a compelling reason.
* **MAY**: optional.

When the specification uses the phrase "compile-time error", it means that a conforming implementation MUST reject the program and MUST report at least one diagnostic.

When the specification uses the phrase "implementation-defined", it means the implementation MUST choose a behavior and SHOULD document it.

### 1.3 Terminology

The following terms are used throughout this specification.

- **Module**: A named namespace boundary declared by `module <qualified_name>;`. A module controls name resolution, visibility boundaries, and import behavior.
- **Compilation unit**: One or more source files compiled together as a single unit of compilation.
- **Type**: A compile-time classification of values and expressions (e.g., `i32`, `string`, `Dog`, `Vec2[]`).
- **Declaration**: A source construct that introduces a named entity (type, field, method, variable, etc.).
- **Member**: A field, method, or nested type declared within a type body.
- **Static member**: A member associated with a type itself rather than with a particular instance.
- **Instance member**: A member associated with each instance value.
- **Owning object**: A value responsible for deallocation of its storage when manual memory management is in effect.
- **Reference**: A non-owning alias (`ref T`) that refers to an object without transferring ownership.
- **Initialization**: The process of computing the initial value of a variable, field, or static state.
- **Construction**: The process of producing a valid instance value of a type, including evaluation of header parameters and initialization of fields.

---

## 2. Lexical Structure

This chapter defines how a Cloth implementation reads source text and converts it into a sequence of tokens.

The lexical structure defines what characters form identifiers, literals, operators, and comments, and it defines how source locations are tracked.

Lexing is a purely textual phase. The lexer:

* MUST NOT perform type checking.
* MUST NOT resolve names.
* MUST NOT interpret numeric ranges or overflow semantics.

Instead, the lexer MUST:

1. Convert source text into a sequence of tokens.
2. Track accurate source locations (file/offset/line/column) for diagnostics.
3. Optionally preserve non-semantic text (whitespace/comments) as trivia.

This chapter describes the behavior of the current implementation.
Where future extensions are anticipated (for example, Unicode identifier rules), the text is marked as reserved.

---

### 2.1 Source Text and Encoding

The compiler reads source files as UTF-8 text.

The input to the lexer is the entire file contents as a sequence of characters.

Rules:

* The implementation MUST preserve the exact source characters for use in token `lexeme` strings.
* A file MAY contain any Unicode code points representable in UTF-8.
* The identifier character classification rules are currently ASCII-first (see Identifiers).

Lexing operates on a `char[]` view of the input text.
In the current implementation, character classification for identifiers is limited to ASCII letters, `_`, and `$`, with a reserved option flag for future Unicode identifier support.

#### 2.1.1 Source locations

Each token carries a `SourceSpan`:

- `start`: file, absolute character offset, line, column
- `end`: file, absolute character offset, line, column

Line and column are tracked incrementally as characters are consumed.

- A newline is recognized for *line counting* when the lexer consumes `\n`.
- `\r` is treated as whitespace, but it does not currently increment the line counter.

Invariants:

* `start` MUST refer to the first character of the token lexeme.
* `end` MUST refer to the character position immediately after the last character of the token lexeme.
* For an `EndOfFile` token, `start` and `end` MUST be equal and MUST represent the end of the file.

---

### 2.2 Tokens

The lexer produces `LexedToken` values:

```text
LexedToken = (token: IToken, trivia: Trivia)
Trivia     = (leading: TriviaPiece[], trailing: TriviaPiece[])
```

The core token kinds are:

- `EndOfFile`
- `Error`
- `Identifier`
- `Number`
- `String`
- `Keyword`
- `Operator`
- `Punctuation`
- `Meta`
- `Whitespace` (optional; see options)
- `Comment` (optional; see options)

All tokens store their exact source substring as the `lexeme`.

Token stream rules:

* The lexer MUST produce tokens in source order.
* The lexer MUST eventually produce exactly one `EndOfFile` token.
* If a lexical error is encountered, the lexer SHOULD emit an `Error` token and SHOULD continue lexing subsequent characters when possible.

Token classification is based on the token kind:

* `Identifier`: an identifier lexeme that is not a keyword and not a meta keyword.
* `Keyword`: an identifier lexeme that matches a reserved keyword.
* `Meta`: an identifier lexeme that matches a meta keyword.
* `Operator` / `Punctuation`: symbolic tokens.
* `Number` / `String`: literal tokens.
* `Whitespace` / `Comment`: only produced when the relevant lexer options request emission.

---

### 2.3 Meta Tokens

Cloth has a distinct meta-token channel used for meta-programming keywords.

Meta tokens are recognized when an identifier lexeme matches one of the known meta keywords *exactly* (case-sensitive, uppercase):

- `ALIGNOF`
- `DEFAULT`
- `LENGTH`
- `MAX`
- `MEMSPACE`
- `MIN`
- `SIZEOF`
- `TO_BITS`
- `TO_BYTES`
- `TO_STRING`
- `TYPEOF`

When recognized, the lexer emits a `MetaToken` (kind `Meta`) rather than a normal identifier/keyword token.

Meta keywords are reserved:

* A meta keyword lexeme MUST NOT be tokenized as a normal `Identifier`.
* The language may assign semantics to meta keywords in later compilation phases.
* Outside of those semantics, meta keywords SHOULD be treated as reserved words to avoid ambiguous tooling behavior.

---

### 2.4 Comments and Whitespace

Whitespace and comments may be handled in three different ways depending on lexer options:

- **Discarded** (default): whitespace/comments are skipped and do not appear as tokens.
- **Emitted as tokens**: whitespace becomes `TokenKind.Whitespace`, comments become `TokenKind.Comment`.
- **Preserved as trivia**: whitespace/comments are attached to neighboring tokens as leading/trailing trivia.

The lexer supports the following options (see `LexerOptions`):

- `--emit-whitespace`: emit whitespace as tokens instead of consuming it as trivia/skip
- `--emit-comments`: emit comments as tokens instead of consuming them as trivia/skip
- `--keep-trivia`: attach leading/trailing trivia to each `LexedToken`
- `--allow-unicode`: currently reserved (identifier rules are still ASCII-first)

#### 2.4.1 Whitespace characters

The lexer treats the following characters as whitespace for scanning:

- space (`' '`) 
- tab (`'\t'`)
- carriage return (`'\r'`)
- line feed (`'\n'`)
- form feed (`'\f'`)

When `--emit-whitespace` is enabled, the lexer produces `Whitespace` tokens whose lexeme is the consecutive run of whitespace characters.

Whitespace and token boundaries:

* Whitespace MAY appear between any two tokens.
* Whitespace MAY appear at the beginning or end of a file.
* Whitespace is not otherwise significant at the lexical level.

#### 2.4.2 Comment forms

Cloth supports two comment forms:

- **Line comment**: `// ...` up to (but not including) the next `\n`.
- **Block comment**: `/* ... */`.

Block comments support **nesting** in the current implementation. Unterminated block comments produce a diagnostic and the lexer continues.

Comment rules:

* Comments are not tokens by default.
* If `--emit-comments` is enabled, comments MUST be emitted as `Comment` tokens and MUST include the exact comment text in the lexeme.
* If a block comment is unterminated, the lexer MUST report a diagnostic and SHOULD still produce a token stream that reaches `EndOfFile`.

#### 2.4.3 Trivia attachment rules

When `--keep-trivia` is enabled, the lexer captures trivia as `TriviaPiece(kind, span, text)`.

- **Leading trivia**: collected before lexing each token. Includes whitespace and comments that are skipped (i.e., not emitted as tokens).
- **Trailing trivia**: collected after lexing a token.

The current trailing trivia rules are intentionally conservative:

- Trailing whitespace includes: space, tab, vertical tab (`'\u000B'`), and form feed.
- Trailing comments include: `//` line comments only.
- Block comments (`/* */`) are not currently collected as trailing trivia.

If trivia is preserved, trivia pieces MUST preserve:

* kind (`Whitespace` or `Comment`)
* exact source span
* exact raw text

---

### 2.5 Identifiers

Identifiers name declared entities (types, members, variables, modules, etc.).

The identifier rules in this chapter specify only what lexemes are tokenized as `Identifier` tokens.
Whether an identifier is valid in a particular syntactic position is specified by later chapters.

Identifier matching is case-sensitive.

#### 2.5.1 Identifier start

An identifier MUST begin with one of:

- ASCII letter `A-Z` or `a-z`
- underscore (`_`)
- dollar sign (`$`)

#### 2.5.2 Identifier continuation

After the first character, an identifier MAY contain:

- ASCII letters `A-Z` or `a-z`
- digits `0-9`
- underscore (`_`)
- dollar sign (`$`)

#### 2.5.3 Keyword and meta-keyword resolution

After scanning an identifier lexeme:

1. If it matches a meta keyword exactly (Section 2.3), a `Meta` token is produced.
2. Else if it matches a language keyword (Section 2.6), a `Keyword` token is produced.
3. Otherwise, an `Identifier` token is produced.

Reserved word rule:

* If a lexeme is recognized as a keyword or meta keyword, it MUST NOT be tokenized as an `Identifier`.
* Programs MUST NOT declare identifiers that are keywords or meta keywords.
* A conforming implementation MUST report a diagnostic if a declaration attempts to use a reserved word as an identifier.

---

### 2.6 Keywords

Keywords are reserved words recognized by the lexer.

Rules:

* Keyword recognition is case-sensitive.
* A keyword lexeme MUST be emitted as a `Keyword` token.
* Keyword lexemes are reserved and MUST NOT be used as identifiers.

The current implementation recognizes (at least) the following keyword families:

- **Control flow**: `if`, `else`, `for`, `while`, `do`, `switch`, `case`, `default`, `break`, `continue`, `return`
- **Declarations**: `func`, `struct`, `enum`, `interface`, `class`
- **Bindings**: `let`, `var`, `const`
- **Boolean/null**: `true`, `false`, `null`
- **Modules**: `module`, `import`
- **Visibility/storage**: `public`, `private`, `internal`, `static`
- **OO / modifiers**: `abstract`, `absolute`, `override`, `final`, `this`, `super`
- **Memory**: `new`, `delete`, `ref`, `shared`, `owned`, `atomic`
- **Async (reserved)**: `async`, `await`
- **Exceptions (reserved)**: `try`, `catch`, `finally`, `throw`
- **Operators-as-keywords**: `as`, `in`, `and`, `or`, `is`
- **Built-in type names (lexed as keywords)**: `i8`, `i16`, `i32`, `i64`, `u8`, `u16`, `u32`, `u64`, `f32`, `f64`, `string`, `char`, `bool`, `byte`, `bit`, `void`, `any`
- **Short forms**: `int`, `long`, `uint`, `float`, `double`, `real`

This chapter describes lexing behavior only: parsing and any future contextual keyword rules are out of scope here.

Future extensions MAY introduce additional reserved words.
If additional reserved words are introduced, they MUST be treated as keywords for lexing purposes and MUST NOT be tokenized as identifiers.

---

### 2.7 Operators and Punctuation

Operators and punctuation are symbolic tokens that are not lexed as identifiers.

The lexer applies a "longest match" rule for symbolic tokens:

* If multiple operators share a prefix, the lexer MUST emit the longest operator that matches at the current position.

This rule exists so that, for example, `...` is not tokenized as `..` followed by `.`.

#### 2.7.1 Multi-character operators

The current implementation recognizes:

- `...`
- `++`, `--`
- `==`, `!=`, `<=`, `>=`
- `->`
- `+=`, `-=`, `*=`, `/=`, `%=`
- `::`
- `..`

#### 2.7.2 Single-character operators

The lexer recognizes the following single-character operators:

- Arithmetic: `+`, `-`, `*`, `/`, `%`
- Assignment: `=`
- Comparison/logical/bitwise: `<`, `>`, `!`, `&`, `|`, `~`, `^`
- Misc: `?`, `@`, `#`, `$`, `` ` ``

#### 2.7.3 Punctuation

The lexer recognizes the following punctuation and delimiters:

- Grouping: `(`, `)`, `{`, `}`, `[`, `]`
- Separators: `.`, `,`, `:`, `;`

`TokenKind.Punctuation` is used for delimiters and separators; most other symbolic forms are `TokenKind.Operator`.

Any unexpected character produces an `Error` token and a diagnostic.

When an unexpected character occurs, the lexer SHOULD consume at least that character so that lexing can make progress.

---

### 2.8 Literals

#### 2.8.1 Numeric literals

Numeric literals are lexed as `TokenKind.Number`. The lexer supports:

- **Decimal**: `123`, `0`, `42`
- **Hexadecimal**: `0xDEAD`, `0Xbeef`
- **Binary**: `0b1010`, `0B0110`
- **Fractional part** (only if `.` is followed by a digit): `3.14`, `0.5`
- **Exponent**: `1e10`, `1E+10`, `1e-3`
- **Underscores** as separators: permitted in all digit sequences, e.g. `1_000_000`, `0xDEAD_BEEF`

Additional scanning rules:

* A numeric literal beginning with `0x` or `0X` is scanned as hexadecimal.
* A numeric literal beginning with `0b` or `0B` is scanned as binary.
* A `.` begins a fractional part only when it is immediately followed by a digit. Otherwise `.` is tokenized separately as punctuation/operator.
* An exponent marker `e` or `E` is only recognized as part of a numeric literal when it appears after at least one digit (in either the integer or fractional part).

Underscore separators:

* `_` MAY appear between digits in any scanned digit sequence.
* `_` MUST NOT appear as the first character of a digit sequence and MUST NOT appear as the last character of a digit sequence.

These underscore placement restrictions are a semantic requirement; if the current lexer accepts additional underscore placements, the compiler SHOULD diagnose them during later validation.

Malformed forms produce an `Error` token and diagnostic:

- `0x` with no hex digits
- `0b` with no binary digits
- exponent marker with no digits after it

The lexer also consumes a trailing alphanumeric suffix after a number (e.g., `123abc`) as part of the same `Number` token. This is a lexical behavior only; semantic validation of suffixes is handled later in compilation.

Suffix note:

* The lexeme of a `Number` token may include characters that are not digits (for example, user-defined or future suffixes).
* Later compilation stages MUST validate that any such suffixes are permitted by the language rules.

#### 2.8.2 String literals

String literals are lexed as `TokenKind.String`.

- A string literal begins with either `"` or `'` and must terminate with the same quote character.
- Newlines inside a string literal are not permitted and produce an unterminated-string diagnostic.
- The lexer enforces a maximum string size (`maxStringLiteralBytes`, default 1 MiB).

Supported escape sequences:

- `\n`, `\r`, `\t`
- `\\`, `\'`, `\"`
- `\0`
- `\xNN` (two hex digits)
- `\uNNNN` (four hex digits)

Invalid escape sequences produce an `Error` token and diagnostic.

If a string literal reaches the end of file before a closing quote, the lexer MUST produce an `Error` token and MUST report an unterminated-string diagnostic.

At this stage, both single-quoted and double-quoted forms produce `String` tokens; a distinct `char` literal token kind is not yet modeled in the lexer.

---

## 3. Program Structure

This chapter defines the top-level structure of a Cloth source file and the ordering rules for top-level declarations.

A Cloth source file is a sequence of tokens that, after parsing, forms a compilation unit.

At the top level, a file contains only declarations.
Executable statements are not permitted at the top level.

### 3.1 Modules

A file MAY declare a module using:

```cloth
module <qualified_name>;
````

Example:

```cloth
module cloth.math;
```

A module name MUST be a dot-separated sequence of identifiers.

If a module declaration is present, it MUST appear before any import or declaration.

Rules:

* A file MUST contain at most one module declaration.
* If a file does not declare a module, it is considered to belong to the empty module (the unnamed module).
* The module name establishes the namespace for top-level type declarations in the file.
* The module namespace root `cloth` is reserved for system-provided code.
  - User code MUST NOT declare modules named `cloth` or any module whose qualified name begins with `cloth.`.
  - User code MAY import modules under `cloth.*` when they are provided by the system or standard library.

Required errors:

* Multiple module declarations in a single file.
* A module declaration appearing after an import or any type declaration.
* A module name that is not a dot-separated sequence of identifiers.
* A module declaration whose name is `cloth` or begins with `cloth.` in user code.

### 3.2 Imports

Imports bring other modules into scope:

```cloth
import <qualified_name>;
```

Example:

```cloth
import std.io;
```

Import declarations MUST appear after the module declaration (if present) and before any type declarations.

Rules:

* Each import declaration names exactly one module.
* Importing a module makes its exported names available for use in this compilation unit according to the visibility rules.
* The effect of an import is limited to the compilation unit in which it appears; imports do not implicitly apply to other files.

Duplicate imports:

* Importing the same module multiple times in a single file is permitted but redundant. Implementations MAY warn on redundant imports.

Reserved:

* Selective imports (importing a single name), alias imports, and wildcard imports are reserved.

Required errors:

* An import declaration appearing before a module declaration (when a module declaration is present later in the file).
* An import declaration appearing after any type declaration.
* An import whose qualified name is not a dot-separated sequence of identifiers.

### 3.3 Types as the Top-Level Declaration Unit

A Cloth source file (`.co`) consists of:

1. an optional `module` declaration,
2. zero or more `import` declarations,
3. one or more **type declarations**.

The following type declarations are permitted at the top level:

* `class`
* `enum`
* `struct`
* `interface`

No executable statements are permitted at the top level.

All executable behavior in Cloth MUST be declared inside a type.

For executable targets, the program entrypoint is still a `static func main` declared inside a **class** (see Entrypoint).

Rules:

* The file MUST contain one or more type declarations.
* Type declarations at the top level MUST NOT be nested inside other top-level constructs.
* A type name declared at the top level is scoped to the module and participates in module-level name lookup.

Required errors:

* Any non-declaration statement at the top level.
* A file containing no type declarations.

### 3.4 Visibility and Access Control

Cloth supports three visibility levels:

* `public`: visible outside the module
* `internal`: visible within the module
* `private`: visible only within the declaring class

Visibility MAY be expressed either:

1. Inline on a declaration, or
2. Using visibility blocks.

Default visibility:

* If a top-level type declaration omits a visibility modifier, it is `internal`.
* If a member declaration inside a type omits a visibility modifier, it is `private`.

The exact meaning of "visible" depends on the kind of declaration and its use site:

* A declaration is **accessible** only if it is visible and is not otherwise restricted by its declaring context.
* Visibility is checked at compile time.

#### 3.4.1 Inline visibility

Example:

```cloth
public class Math() { }
private static func helper(): void { }
```

#### 3.4.2 Visibility blocks

A visibility block applies the visibility modifier to each enclosed member declaration:

```cloth
public {
    func api(): void { }
    static func main(): i32 { return 0; }
}
```

Visibility blocks:

* MUST NOT be nested.
* MUST appear only at type member level (not inside method bodies).
* MUST NOT introduce a new lexical scope.

If a declaration inside a visibility block also specifies inline visibility, the inline visibility MUST take precedence.

Required errors:

* Nested visibility blocks.
* A visibility block appearing inside a method body.
* A visibility block containing statements (only declarations are permitted inside a visibility block).

---

## 4. Types

### 4.1 Built-in Primitive Types

Cloth defines a fixed set of **built-in primitive types**.

Primitive types are keywords and do not require any declaration.

The primitive types are:

Signed integers:

* `i8`, `i16`, `i32`, `i64`

Unsigned integers:

* `u8`, `u16`, `u32`, `u64`

Floating-point:

* `f32`, `f64`

Text and character:

* `string`
* `char`

Booleans:

* `bool`

Bit and byte:

* `bit`
* `byte`

Special:

* `void`
* `any`

#### 4.1.1 Integer types

`i8`, `i16`, `i32`, and `i64` represent signed integers of width 8, 16, 32, and 64 bits.

`u8`, `u16`, `u32`, and `u64` represent unsigned integers of width 8, 16, 32, and 64 bits.

Rules:

* Integer arithmetic overflow behavior is not fully specified in this chapter. Unless and until a checked/unchecked model is specified, compilers SHOULD diagnose constant overflows and MAY define runtime overflow behavior as implementation-defined.
* Comparison operators on integer types produce `bool`.

#### 4.1.2 Floating-point types

`f32` and `f64` represent 32-bit and 64-bit floating-point numbers.

The detailed floating-point semantics (NaN comparisons, rounding mode, etc.) are reserved for a dedicated numeric semantics chapter.

#### 4.1.3 `bool`

`bool` has exactly two values: `true` and `false`.

`bool` is the required type for conditions in `if`, `while`, and other control-flow constructs.

#### 4.1.4 `char` and `string`

`char` represents a single character value.

`string` represents an immutable sequence of characters.

Rules:

* The precise character encoding model (`char` width, Unicode handling, and string internal representation) is reserved.
* String literals produce values of type `string`.
* Character literal syntax and its exact interaction with lexing/token kinds is reserved; until specified, `char` values MAY be constructed through library APIs.

#### 4.1.5 `bit` and `byte`

`bit` represents a single binary digit.

`byte` represents an 8-bit byte-sized value.

Rules:

* The exact relationship between `byte` and `u8` is reserved. In v1, they are distinct types and are not implicitly interchangeable.
* The exact relationship between `bit` and `bool` is reserved. In v1, they are distinct types and are not implicitly interchangeable.

#### 4.1.6 `void`

`void` represents the absence of a value.

Rules:

* `void` MAY be used only as a function return type.
* Variables, fields, and parameters MUST NOT have type `void`.

#### 4.1.7 `any`

`any` is a dynamic top type that may hold a value of any non-`void` type.

Rules:

* A value of any type MAY be stored into a location of type `any`.
* Extracting a value from `any` requires an explicit cast using `as`.
* The runtime representation of `any` (boxing, tagging, etc.) is reserved, but the cast semantics MUST be sound: if the contained value is not compatible with the target type, the cast MUST fail according to the cast failure rules.

### 4.2 Nullability

A type MAY be made nullable by adding `?` as a suffix:

```cloth
var name: string? = null;
```

Nullability is a property of the type, not an expression.

### 4.3 Arrays

Cloth supports array types written using a repeated `[]` suffix.

#### 4.3.1 Array type syntax

An array type is written by adding brackets to an element type:

```cloth
T[]
```

`T` is called the **element type**.

`T` MAY itself be an array type. This permits multi-dimensional array types.

Examples:

```cloth
var grid: i32[][] = [ [ 1, 2 ], [ 3, 4 ] ];
var cubes: f32[][][] = ...;
```

Examples:

```cloth
var xs: i32[] = [ 10, 20, 30 ];
var names: string[] = [ "a", "b" ];
```

#### 4.3.2 Array literals

An array literal has the form:

```cloth
[ <expr0>, <expr1>, ... ]
```

Rules:

* Array literal element expressions MUST be evaluated left-to-right.
* An array literal constructs an array value containing the evaluated element values in source order.
* The length of an array is the number of elements in the literal.

The memory allocation model of arrays is specified in the Memory Model chapter. Regardless of representation, writing an array literal is considered an explicit construction form.

#### 4.3.3 Typing rules

Given an array literal with elements `e0, e1, ...`:

* If the context requires a specific array type `T[]`, then each element expression MUST be assignable to `T`.
* If there is no required type context, the compiler MAY infer `T` only if all element expressions have the same type `T`.
* The type of `[]` (an empty array literal) is not inferred by default. `[]` MUST require a type context (e.g., a variable annotation) or a dedicated typed-literal syntax (reserved).

#### 4.3.4 Mutability and indexing

The mutability model for arrays (whether elements can be assigned through indexing, and whether arrays are fixed-size) is reserved.

Indexing syntax `a[i]` is specified in the Expressions chapter.

#### 4.3.5 Required errors

Compilation MUST fail for any of the following:

* An array literal whose elements cannot be assigned to the required element type.
* Using `void` as an array element type (e.g., `void[]`).

---

## 5. Declarations

### 5.1 Class Declarations

Class declarations have the form:

```cloth
[visibility] class <Name>(<primary_constructor_params>) { <members> }
```

Example:

```cloth
public class Main(args: string[]) { }
```

#### 5.1.1 Primary constructors

Primary constructor parameters are declared in the class header parameter list.

Each primary constructor parameter MUST become a **private immutable field** of the same name.

Example:

```cloth
class Animal(food: string) {
    func eat(): void {
        println(this.food);
    }
}
```

The field `food` is implicitly available as `this.food`.

### 5.2 Field Declarations

Fields MAY be declared using:

* `var` (mutable)
* `final var` (assigned once, then immutable)
* `const` (compile-time constant) **(TBD final vs const semantics)**

Example:

```cloth
var x: i32 = 0;
final var name: string = "Cloth";
```

Fields MAY be declared `static`, making them class-level members.

Example:

```cloth
public static {
    final var PI: f64 = 3.14159;
}
```

### 5.3 Method Declarations

Methods use `func`:

```cloth
func <name>(<params>): <return_type> { <body> }
```

Parameters MUST be written as `name: type`.

Example:

```cloth
func add(a: i32, b: i32): i32 {
    return a + b;
}
```

### 5.4 Overriding and `final`

By default, instance methods MAY be overridden in subclasses.

A method override MUST be explicitly marked with `override`:

```cloth
override func eat(): void { ... }
```

A method marked `final` MUST NOT be overridden:

```cloth
final func increaseHappiness(amount: f32): void { ... }
```

A class marked `final` MUST NOT be subclassed. **(TBD syntax and enforcement details)**

---

### 5.5 Enum Declarations

Enums define a closed set of named values.

Cloth enums have the following properties:

* An enum defines a finite set of **cases**.
* An enum may declare **methods**.
* A case MAY carry **associated data** (a payload).
* Every enum value has a well-defined **numeric discriminant** (ordinal) representation.
* Enums expose a **reflection surface** for enumerating cases and inspecting metadata.

#### 5.5.1 Declaration form

An enum declaration has the form:

```cloth
[visibility] enum <Name> { <cases> <members>? }
```

Cases appear first in the enum body, followed by optional members (methods and static members).

Example:

```cloth
public enum Color {
    Red,
    Green,
    Blue;

    func isPrimary(): bool {
        return true;
    }
}
```

The semicolon after the case list is REQUIRED if and only if additional members follow.

#### 5.5.2 Case declarations

Each enum case declares a distinct value.

Case forms:

1. **Unit case** (no payload):

```cloth
Red
```

2. **Data case** (payload):

```cloth
Rgb(r: u8, g: u8, b: u8)
```

3. **Explicit discriminant** (optional):

```cloth
Error = 500
```

These forms MAY be combined:

```cloth
Http(code: i32, message: string) = 500
```

Rules:

* Case names MUST be unique within the enum.
* Payload parameter names MUST be unique within the case.
* Payload parameter declarations use the same `name: Type` form as methods.
* If a case declares a payload, that payload is part of the enum value and participates in equality (see 5.5.6).

#### 5.5.3 Discriminants and numeric representation

Every enum value has a numeric discriminant (also called *ordinal*).

Rules:

* If no explicit discriminant is given, discriminants are assigned in **source order**, starting from `0`, incrementing by 1.
* If explicit discriminants are used, they MUST be compile-time constants.
* If any explicit discriminant is present, the compiler MUST still assign discriminants for cases without explicit values using the previous case’s discriminant + 1, in source order.
* Two cases MUST NOT have the same discriminant value. Duplicate discriminants are a compile-time error.

Underlying storage type:

* The default discriminant storage type is `i32`.
* A future extension MAY allow specifying the underlying integer type explicitly (reserved).

Conversions:

* Converting an enum value to its discriminant is permitted via the reflection surface (e.g., `ordinal()`).
* Direct casts between integers and enums are not defined by default. If such casts are introduced, they MUST be explicit and checked (TBD).

#### 5.5.4 Construction of enum values

Enum values are not allocated with `new`.

* A unit case value is referenced by the qualified name `EnumName.CaseName`.
* A data case value is constructed using call syntax on the case name:

```cloth
var c = Color.Rgb(255, 0, 0);
```

Data case construction MUST initialize all payload fields.

Evaluation order of payload arguments is left-to-right.

#### 5.5.5 Methods and members inside enums

Enums MAY declare:

* instance methods (`func ...`) that operate on an enum value
* static methods (`static func ...`)
* static fields (`static var`, `static final var`, `static const`)

Instance methods may use `this`.

Enums do not participate in class inheritance.

* An enum MUST NOT extend a base class.
* A class MUST NOT extend an enum.

#### 5.5.6 Equality and identity

Enum equality is value-based:

* Two enum values are equal if and only if:
  1. They are of the same enum type, and
  2. They have the same case discriminant, and
  3. If the case has payload data, each payload field compares equal (field-wise, in source order).

Unit cases therefore behave like unique singletons.

#### 5.5.7 Exhaustiveness and closed-world guarantees

Because an enum’s cases are closed, the compiler MAY use enums for exhaustiveness checking.

When a statement form supporting exhaustive case analysis exists (e.g., `switch`), the compiler SHOULD require all enum cases to be handled or require an explicit `default`.

The exact exhaustiveness rules are specified in the `switch` chapter (TBD).

#### 5.5.8 Reflection surface (required)

Enums provide a standardized reflection surface.

For an enum type `E`, the compiler MUST provide the following members (either as implicitly declared methods or as compiler intrinsics) with the described semantics:

* `static func values(): E[]`
  * Returns all **unit cases** of `E` in source order.
  * Data cases are excluded because they represent infinitely many values.

* `func name(): string`
  * Returns the case name (e.g., `"Red"`).

* `func ordinal(): i32`
  * Returns the discriminant value.

* `static func fromOrdinal(x: i32): E?`
  * Returns the matching unit case if one exists with discriminant `x`, otherwise `null`.
  * This MUST NOT construct data cases.

* `static func hasCase(name: string): bool`
  * Returns true if `name` is the name of any case in `E`.

Additionally, the compiler SHOULD make the following metadata available to tooling and reflection APIs (format is implementation-defined):

* case list in source order
* discriminant values
* per-case payload field names and types (for data cases)

Reflection MUST NOT require executing user code.

#### 5.5.9 Diagnostics and required errors

Compilation MUST fail for any of the following:

* Duplicate case names.
* Duplicate discriminant values.
* A case payload field missing a type annotation.
* Use of `new` with an enum type.
* An enum participating in class inheritance.
* Attempting to construct a unit case using call syntax (e.g., `Color.Red(...)`) or attempting to reference a data case without providing its payload.

---

### 5.6 Struct Declarations

Structs define **value types**.

Structs are designed for:

* predictable layout
* no hidden allocations
* efficient copying
* use in low-level systems contexts

#### 5.6.1 Declaration form

A struct declaration has the form:

```cloth
[visibility] struct <Name>(<primary_params>?) { <members> }
```

Structs MAY declare a primary parameter list in the header, using the same syntax as classes.

Each struct primary parameter MUST create an implicit instance field with the same name, with the same rules as class primary parameters:

* Visibility: `private`
* Mutability: immutable (equivalent to `final var`)
* Type: the declared parameter type

Example:

```cloth
struct Vec2(x: f32, y: f32) {
    func lengthSquared(): f32 {
        return this.x * this.x + this.y * this.y;
    }
}
```

#### 5.6.2 Value semantics

Struct values have **value semantics**:

* Assignment copies the value.
* Passing a struct as a parameter passes the value (copy) unless the parameter type uses an explicit reference qualifier (e.g., `ref`), if/when those are fully specified.

The exact ABI details of copying (bitwise vs field-wise) are implementation-defined, but behavior MUST be observationally equivalent to copying all fields.

#### 5.6.3 Construction

Struct values are constructed without heap allocation.

Form:

```cloth
var v: Vec2 = Vec2(1.0, 2.0);
```

Rules:

* Struct construction MUST evaluate arguments left-to-right.
* Struct construction MUST initialize all fields (definite initialization rules apply).
* Using `new` with a struct type MUST be a compile-time error.

#### 5.6.4 Members

Structs MAY declare:

* instance fields (`var`, `final var`, `const`)
* instance methods
* static fields and static methods

Struct methods follow the same declaration rules as class methods.

#### 5.6.5 Equality

Struct equality is value-based by default:

* Two struct values are equal if and only if all fields compare equal in source order.

(TBD: user-defined equality or operator overloading is intentionally not part of v1.)

#### 5.6.6 Restrictions and required errors

Compilation MUST fail for any of the following:

* `new S(...)` where `S` is a struct.
* A struct attempting to inherit from a base class.

---

### 5.7 Interface Declarations

Interfaces define **behavioral contracts**: a set of method signatures that a type may implement.

Interfaces are intended to support decoupling and polymorphism without introducing multiple inheritance of implementation.

#### 5.7.1 Declaration form

An interface declaration has the form:

```cloth
[visibility] interface <Name> { <members> }
```

In v1, interface members are limited to **method signatures**.

Example:

```cloth
public interface Drawable {
    func draw(): void;
}
```

Interface method declarations:

* MUST NOT have a body.
* MUST end with `;`.
* MUST specify a return type.

#### 5.7.2 Implementing interfaces

Classes and structs MAY implement one or more interfaces.

Implementation clause syntax (v1):

```cloth
class C(): Base(...), I1, I2 { ... }
struct S(...): I1, I2 { ... }
```

Rules:

* At most one base class constructor call may appear and it MUST appear first after `:`.
* Any remaining items after the optional base call MUST be interface type names.
* An interface type name in the implementation clause MUST NOT have constructor arguments.
* A type MUST NOT list the same interface more than once.

#### 5.7.3 Conformance requirements

If a type `T` claims to implement interface `I`, then `T` MUST provide a concrete implementation for every method declared in `I`.

Rules:

* A method satisfies an interface requirement only if the signature matches exactly.
* Visibility of implementing methods MUST be `public` or `internal` (since the interface contract is externally callable).

(TBD: default interface methods, generic interfaces, and variance.)

#### 5.7.4 Dynamic dispatch and representation

The runtime representation and dispatch mechanism for interface values is TBD (vtable, fat pointers, etc.).

However, the language MUST preserve the following semantic guarantees:

* Calling an interface method on a value invokes the implementation provided by the dynamic type.
* If the implementing method is marked `final` on a class, it still satisfies interface requirements but cannot be overridden in derived classes.

#### 5.7.5 Reflection surface (reserved)

Interfaces SHOULD be discoverable via reflection tooling.

At minimum, the compiler SHOULD be able to report:

* the list of methods declared by an interface
* the list of interfaces implemented by a type

Exact reflection APIs are TBD.

#### 5.7.6 Required errors

Compilation MUST fail for any of the following:

* An interface declaring fields.
* An interface method declaring a body.
* A type claiming to implement an interface but failing to implement all required methods.
* A type listing an interface with constructor arguments.

---

## 6. Entrypoint

This chapter defines how an executable Cloth program begins execution.

An implementation may support building multiple kinds of artifacts (for example, executables and libraries).
This chapter applies only to executable targets.

### 6.1 Definition of the entrypoint

An executable program MUST provide exactly one entrypoint method named `main`.

The entrypoint is the method invoked first by the runtime when the program starts.

### 6.2 Entrypoint candidates

An entrypoint candidate is any method declaration that satisfies all of the following:

* The method name is exactly `main`.
* The method is declared as a member of a class.
* The method is declared `static`.
* The method visibility is `public` or `internal`.

Entrypoint candidates are considered across the entire program being built as an executable (all compilation units participating in the build).

### 6.3 Allowed signatures

The entrypoint MUST match one of the following signatures exactly:

```cloth
static func main(): void
static func main(): i32
static func main(args: string[]): void
static func main(args: string[]): i32
```

Rules:

* The parameter name is not semantically significant. Only the parameter type and arity matter.
* The return type MUST be either `void` or `i32`.
* The parameter type, if present, MUST be exactly `string[]`.

Restrictions:

* The entrypoint MUST NOT be generic.
* The entrypoint MUST NOT be overloaded such that more than one overload satisfies the allowed signatures.
* Default parameter values, if present in the language, MUST NOT be used to make a non-matching `main` appear to match a permitted signature.

### 6.4 Selection algorithm

The compiler MUST select the entrypoint as follows:

1. Collect all entrypoint candidates (Section 6.2).
2. Filter candidates to those whose signatures match an allowed signature (Section 6.3).
3. If the filtered set is empty, compilation MUST fail.
4. If the filtered set contains more than one method, compilation MUST fail.
5. Otherwise, the single remaining method is the program entrypoint.

The selection algorithm is intentionally simple and MUST NOT depend on source file names, class names, or module names.

### 6.5 Program arguments

If the selected `main` method has a single parameter of type `string[]`, it receives the program arguments as an array of strings.

Rules:

* The argument array MUST be non-null.
* The order of arguments in the array MUST match the process invocation order.
* The encoding/normalization rules for the argument strings are implementation-defined.

### 6.6 Return value and process exit code

If `main` returns `i32`, that value is the program exit code.

If `main` returns `void`, the program exit code is `0`.

### 6.7 Initialization before `main`

Before `main` begins execution, the runtime MUST ensure that any required static initialization has occurred for code that will be executed as part of program startup.

At minimum:

* The declaring class of `main` MUST be statically initialized before executing the first instruction of `main`.

Additional static initialization may occur as required by the Static Initialization Order rules.

### 6.8 Required diagnostics

Compilation MUST fail and report at least one diagnostic for any of the following:

* No method named `main` exists.
* Methods named `main` exist, but none match an allowed signature.
* More than one method matches an allowed signature.
* A method named `main` matches an allowed signature but is not declared `static`.
* A method named `main` matches an allowed signature but is not declared inside a class.
* A method named `main` matches an allowed signature but has visibility `private`.

---

## 7. Expressions

This chapter defines the syntax, structure, and evaluation rules for expressions in Cloth.

An expression is a source construct that, when evaluated, produces a value (except where the result is discarded) and may produce side effects.

Expressions are evaluated within an execution context (a function body, initializer expression, etc.).
Expressions are not permitted at the top level of a file.

Cloth expressions are intentionally conservative and structured to favor readability and predictability.

Unless otherwise stated:

* Expression evaluation either produces a value of some type, or compilation fails.
* When a type is required by context (for example, in `var x: T = <expr>`), the expression MUST be type-checkable as that required type.
* If an expression form requires allocation or dynamic dispatch, it MUST be explicit in source (for example, via `new` or an interface call).

---

### 7.1 General Rules

- Expressions are evaluated **left-to-right** unless otherwise specified.
- There are no user-defined operators.
- Operator precedence is fixed and cannot be altered.
- Assignment is a statement-level construct and is **not an expression**.
- Expressions MUST NOT contain implicit allocations unless explicitly written using `new`.

Diagnostics:

* If an expression is ill-formed, compilation MUST fail and report a diagnostic at the relevant source span.
* If an expression refers to a name that cannot be resolved (variable, field, type, or member), compilation MUST fail.

Example:

```cloth
var x: i32 = 10 + 20;
````

---

### 7.2 Primary Expressions

Primary expressions are the most basic forms:

| Form                     | Description                       |
|--------------------------|-----------------------------------|
| Identifier               | Variable or field reference       |
| Literal                  | Numeric, string, boolean, or null |
| `this`                   | Current instance                  |
| Parenthesized expression | `(expr)`                          |
| Object creation          | `new Type(args...)`               |
| Member access            | `expr.name`                       |
| Function call            | `expr(args...)`                   |
| Array indexing           | `expr[index]`                     |

Rules:

* Primary expressions are evaluated left-to-right, including evaluation of subexpressions (receiver expressions, index expressions, and call arguments).
* Parentheses MAY be used to force grouping and improve clarity.

Required errors:

* Using `this` outside of an instance method body.
* Using `super` outside of a valid base-member access context.

Example:

```cloth
new Dog("Kibble")
player.position.x
matrix[row][col]
```

---

### 7.3 Member Access

Member access uses `.` and is left-associative:

```cloth
a.b.c.d
```

Each access is evaluated in sequence.

Rules:

* The receiver expression is evaluated first.
* Member lookup and visibility rules are specified in the Member Lookup and Shadowing chapter.
* If the receiver type does not have an accessible member with the requested name, compilation MUST fail.

Required errors:

* Accessing an instance member through a type name.
* Accessing a static member through an instance when the language requires static qualification (exact rules are specified in the member access chapter).

---

### 7.4 Function Calls

Calls use:

```cloth
expr(arg1, arg2, ...)
```

Arguments are evaluated left-to-right.

Method calls are syntactic sugar for passing the receiver as the first parameter in the underlying call model (implementation detail).

Rules:

* The callee expression is evaluated before any arguments.
* Each argument expression is evaluated left-to-right.
* Overload resolution and argument-to-parameter matching rules are specified in the Functions and Methods chapter.

Required errors:

* Calling an expression that is not callable.
* Providing the wrong number of arguments.
* Providing an argument value that is not assignable to the corresponding parameter type.

---

### 7.5 Object Creation

Heap allocation is explicit:

```cloth
new Type(args...)
```

This MUST allocate memory using the active memory mode (manual or GC).

`new` is never implicit.

Rules:

* The type being allocated MUST be a heap-allocatable type (for example, a class type).
* The argument expressions MUST be evaluated left-to-right.
* Allocation MUST either succeed and produce a valid object reference/value according to the active memory mode, or the program MUST terminate/throw according to the runtime's allocation-failure policy (reserved).

Required errors:

* Using `new` with a struct type.
* Using `new` with an enum type.
* Using `new` with `void`.

---

### 7.6 Unary Operators

| Operator | Meaning             |
|----------|---------------------|
| `-`      | Arithmetic negation |
| `!`      | Logical NOT         |

Example:

```cloth
-x
!flag
```

Rules:

* Unary `-` requires a numeric operand type.
* Unary `!` requires a `bool` operand type.

Required errors:

* Applying `-` to a non-numeric operand.
* Applying `!` to a non-`bool` operand.

---

### 7.7 Binary Operators

#### Logical

Cloth uses keyword-based logical operators rather than symbolic forms.

| Operator | Meaning     | Short-Circuit |
|----------|-------------|---------------|
| `and`    | Logical AND | Yes           |
| `or`     | Logical OR  | Yes           |

Example:

```cloth
if ready and initialized {
    start();
}

if error or timeout {
    retry();
}
````

Rules:

* `and` and `or` operands MUST be of type `bool`.
* `and` and `or` MUST short-circuit: the right operand is evaluated only if needed to determine the result.

Required errors:

* Using `and` or `or` with non-`bool` operands.

### 7.8 String Concatenation

`+` MAY concatenate strings when one operand is of type `string`.

```cloth
"Hello " + name
```

Cloth does allow string concatenation with numeric operands, but the left operand MUST be of type `string`.
This is to avoid ambiguity with numeric promotion. Cloth will append the number in whole to the right side of the `string`.

```cloth
var s = "10" + 20;
println(s);
// prints "1020"
```

Rules:

* If the left operand is of type `string`, `+` produces a `string` result.
* The right operand MUST be convertible to `string` using a defined conversion (reserved; implementations MAY provide a standard conversion mechanism).

Required errors:

* Using `+` where neither operand is `string` and the operand types do not support numeric addition.
* Using `+` with left operand `string` but with a right operand that has no defined conversion to `string`.

---

### 7.9 Operator Precedence

Operators are evaluated according to the following precedence table (highest first):

| Level | Operators         | Associativity |
|-------|-------------------|---------------|
| 1     | `()` `[]` `.`     | Left          |
| 2     | `new`             | Right         |
| 3     | Unary `-` `!`     | Right         |
| 4     | `*` `/` `%`       | Left          |
| 5     | `+` `-`           | Left          |
| 6     | `<` `<=` `>` `>=` | Left          |
| 7     | `==` `!=`         | Left          |

Cloth intentionally omits assignment from the precedence hierarchy.

---

### 7.10 No Assignment Expressions

Cloth does **not** allow assignment within expressions.

Cloth does, however, allow chained assignment as a statement form (for example, `a = b = c;`).

Invalid:

```cloth
if (x = 5) { }   // error
```

Valid:

```cloth
x = 5;
a = b = c;
if (x == 5) { }
```

This rule prevents a class of bugs.

---

### 7.11 Evaluation Order Guarantees

Cloth guarantees:

* Left operand is evaluated before right operand.
* Function arguments are evaluated left-to-right.
* No unspecified evaluation order is permitted.

This is intentionally strict and is required for predictability.

Required errors:

* Statement forms that would require unspecified evaluation order MUST NOT be introduced without explicitly specifying their evaluation order.

---

### 7.12 Design Choices and Non-Features

This section clarifies a small set of expression features that are either intentionally omitted or intentionally constrained.

Not supported:

* Operator overloading.
* The comma operator (an expression operator where `a, b` evaluates `a` for side effects and yields `b`).

Supported:

* Ternary conditional expressions (`condition ? thenExpr : elseExpr`).
* Pointer arithmetic syntax.
* Chained assignment in statement position (for example, `a = b = c;`) while still rejecting assignment inside expressions.

Clarifications:

* Cloth uses commas as separators in syntax (for example, parameter lists, argument lists, and variable declarations). This is not the same as a comma operator.
* Cloth does not perform implicit numeric or structural conversions; conversions require an explicit cast using `as`.

---

### 7.13 Future Extensions (Reserved)

The grammar reserves space for:

* Safe navigation operators (if ever added)
* Pattern matching constructs

---

### 7.14 Explicit Cast Expressions

Cloth supports **explicit type conversion** using the `as` operator.

Syntax:

```cloth
<expression> as <Type>
````

Example:

```cloth
let x: f32 = 10.5;
println(x as i32);
```

The `as` operator performs an explicit, programmer-requested conversion.
Cloth does not perform implicit numeric or structural conversions.

---

#### 7.14.1 Cast Semantics

A cast using `as` MUST be explicitly allowed by the type system.
If no defined conversion exists between the source and target types,
compilation MUST fail.

Example (valid):

```cloth
let x: f64 = 42.8;
let y: i32 = x as i32;
```

Example (invalid):

```cloth
let s: string = "hello";
let n: i32 = s as i32;   // error: no defined conversion
```

---

#### 7.14.2 Numeric Cast Behavior

Numeric casts follow well-defined truncation/extension rules:

| From → To     | Behavior                                             |
|---------------|------------------------------------------------------|
| float → int   | Truncates toward zero                                |
| int → float   | Exact if representable                               |
| narrow → wide | Zero/sign-extended                                   |
| wide → narrow | Truncated (implementation-defined overflow handling) |

Rules:

* Numeric casts MUST NOT perform rounding. In particular, casting from a floating-point type to an integer type truncates toward zero.
* When casting from an integer type to a narrower integer type, the high-order bits that do not fit in the target type are discarded.
  * This corresponds to taking the value modulo `2^N`, where `N` is the bit-width of the target integer type.
* When casting from an integer type to a wider integer type, the value is sign-extended for signed integers and zero-extended for unsigned integers.

Numeric to string:

* A numeric value MAY be cast to `string` using `as string`.
* The result is a textual representation of the numeric value.
* The exact formatting (for example, number of digits, exponent form, and representation of `NaN` and infinities) is implementation-defined.

---

#### 7.14.3 Reference Casts

This section defines casts between reference types (such as classes and interfaces).

In v1, reference casts are **runtime-checked** when the cast is not provably safe at compile time.

Rules (compile-time legality):

* A reference cast is only defined when both the source type and the target type are reference types.
* If the target type is a known supertype of the source type (for example, derived class to base class, or class to an implemented interface), the cast MUST be accepted and does not require a runtime check.
* If the source type is a known supertype of the target type (for example, base class to derived class), the cast MUST be accepted and MUST perform a runtime check.
* If neither type is a known subtype of the other (unrelated types), compilation MUST fail.

Rules (runtime behavior):

* For a runtime-checked cast, the operand expression is evaluated first.
* If the runtime value is an instance of the target type, the cast yields the same reference value, typed as the target type.
* If the runtime value is not an instance of the target type, the program MUST trap with a runtime cast error.

Required errors:

* Attempting to cast between unrelated reference types.
* Attempting to use reference casts in configurations where the implementation does not provide a runtime type check for the involved types.

---

### 7.15 Operator Precedence Update

The `as` operator has lower precedence than arithmetic but higher precedence than logical operators.

| Level | Operators         | Associativity |
|-------|-------------------|---------------|
| 1     | `()` `[]` `.`     | Left          |
| 2     | `new`             | Right         |
| 3     | Unary `-` `!`     | Right         |
| 4     | `*` `/` `%`       | Left          |
| 5     | `+` `-`           | Left          |
| 6     | `as`              | Left          |
| 7     | `<` `<=` `>` `>=` | Left          |
| 8     | `==` `!=`         | Left          |
| 9     | `and`             | Left          |
| 10    | `or`              | Left          |

---

#### 7.15.1 Rationale

Placing `as` below arithmetic ensures intuitive reading:

```cloth
x + y as i32
```

is parsed as:

```cloth
x + (y as i32)
```

Parentheses may always be used for clarity.

---

### 7.16 No Implicit Conversions

Cloth intentionally avoids implicit conversions to preserve clarity and predictability.

The following MUST require an explicit cast:

* float ↔ integer
* width-changing numeric conversions
* signed ↔ unsigned (reserved)
* pointer/reference reinterpretation (future)

Example:

```cloth
let x: i32 = 10;
let y: f32 = x as f32;   // required
```
---

Perfect — we’ll lock in **Option A** (type parameters before the value parameters).
That gives Cloth a clean, readable, and compiler-friendly function-literal form that still feels consistent with the rest of the language.

Below is the **spec-ready addition** you can drop into the Expressions section.

---

### 7.17 Function Literal Expressions

Cloth supports function literals, which create callable objects inline without declaring a named method.

Function literals combine characteristics of lambdas and nested functions while preserving explicit typing and predictable compilation behavior.

---

#### 7.17.1 Syntax

A function literal has the form:

```text
[TypeParameters] "(" ParameterList? ")" ":" ReturnType "->" Block
````

Examples:

```cloth
var double = (y: i32): i32 -> { return y * 2; };

var add = (a: i32, b: i32): i32 -> { return a + b; };

var id = <T>(x: T): T -> { return x; };
```

Type parameters, when present, MUST appear before the parameter list.

---

#### 7.17.2 Type Parameters

Generic parameters are declared using angle brackets before the parameter list:

```cloth
var id = <T>(x: T): T -> { return x; };
```

These parameters are scoped to the function literal only.

---

#### 7.17.3 Capture Clause

Cloth function literals do not capture values from an enclosing scope.

Rules:

* A name referenced from within a function literal body MUST resolve to one of the following:
  * a parameter of the function literal,
  * a local declared within the function literal body,
  * a global name, or a member selected explicitly through an expression.

Examples:

```cloth
var double = (y: i32): i32 -> { return y * 2; };

var id = <T>(x: T): T -> { return x; };
```

The following is rejected because `base` is a local in the enclosing scope:

```cloth
let base: i32 = 10;
var addBase = (x: i32): i32 -> { return x + base; };
```

Required errors:

* Referencing a local variable from an enclosing scope inside a function literal body.

---

#### 7.17.4 Callable Semantics

A function literal evaluates to a callable object that exposes an `invoke(...)` method.

Calling syntax:

```cloth
f(args...)
```

is defined as syntactic sugar for:

```cloth
f.invoke(args...)
```

Example:

```cloth
var double = (x: i32): i32 -> { return x * 2; };
println(double(5));       // sugar
println(double.invoke(5)); // equivalent
```

---

#### 7.17.5 Allocation Behavior

Since function literals do not capture values, an implementation MAY compile a function literal to a plain callable with no environment allocation.

---

#### 7.17.6 Expression Classification

Function literals are **primary expressions** and therefore bind tightly in precedence.

Example:

```cloth
call( (x: i32): i32 -> { return x + 1; } );
```

---

#### 7.17.7 Rationale

Function literals provide localized behavior without introducing hidden allocation, implicit capture, or metaprogramming complexity. They are designed to remain analyzable and predictable within Cloth’s systems-oriented model.

---

## 8. Statements

This chapter defines the statement forms in Cloth.

A statement is an execution step. Statements may:

* evaluate expressions,
* control control-flow,
* introduce and initialize local variables,
* perform explicit resource management actions.

Statements exist only inside method bodies.
Statements are not permitted at the top level of a file.

Declarations (for example, type declarations, fields, and method declarations) MAY appear outside method bodies according to the rules of the surrounding type and module structure.

Cloth statements are designed to be explicit, readable, and unambiguous. In particular:

* Assignment is a statement (not an expression).
* Control flow forms are statement-oriented (not expression-oriented).
* Evaluation order is deterministic.

---

### 8.1 Statement Terminators

Most statements are terminated by `;`.

Block statements (e.g., `if { ... }`, `while { ... }`) do not require a trailing `;`.

Rules:

* A statement that syntactically ends with a block MUST NOT be followed by an additional `;`.
* A statement that does not end with a block MUST be terminated by `;`.

Required errors:

* Missing `;` where required.
* Using `;` after a block statement where it is not permitted.

Examples:

```cloth
x = 5;
println("Hello");
````

```cloth
if ready {
    start();
}
```

---

### 8.2 Blocks

A block is a sequence of statements enclosed in `{` and `}`.

```cloth
{
    stmt1;
    stmt2;
}
```

Blocks introduce a new lexical scope for local variables.

Rules:

* Declarations in a block are visible from their declaration point to the end of the block.
* A local variable declared in an inner block MAY shadow a variable declared in an outer block.
* Shadowing MUST NOT change the meaning of already-parsed code; name lookup is purely lexical.

Required errors:

* Declaring the same local name twice in the same block scope.

---

### 8.3 Variable Declarations

Local variable declarations use the binding keywords:

* `let` (immutable)
* `var` (mutable)
* `final var` (assigned once; then immutable)
* `const` (compile-time constant) (TBD: constant expression rules)

Forms:

```cloth
<binding> <name>: <Type>;
<binding> <name>: <Type> = <expr>;
```

Rules:

* If an initializer is present, the initializer expression MUST be type-checkable and MUST be assignable to the declared type.
* The declared name MUST be a valid identifier and MUST NOT be a reserved word.

Binding semantics:

* `let` declares an immutable local binding.
* `var` declares a mutable local binding.
* `final var` declares a mutable binding that may be assigned at most once (typically during initialization paths), after which it becomes immutable.
* `const` declares a compile-time constant.

Initializer requirements:

* A `let` declaration MUST include an initializer.
* A `const` declaration MUST include an initializer.
* A `var` declaration MAY omit an initializer.
* A `final var` declaration MAY omit an initializer.

If a local declaration omits an initializer, the variable is considered uninitialized and MUST NOT be read until it becomes definitely initialized (see 8.3.1).

`const` rules:

* A `const` initializer MUST be a constant expression (constant-expression rules are specified elsewhere).
* A `const` local MUST be of a type permitted for compile-time constants (reserved until the const model is finalized).

Type annotations MAY be omitted when the initializer provides a type (TBD: inference rules).

Required errors:

* Missing initializer for `let`.
* Missing initializer for `const`.
* Initializer type not assignable to the declared type.
* `return`, `break`, `continue`, or other statement forms used where an expression is required.

#### 8.3.1 Definite Initialization of Local Variables

Cloth requires that a local variable be initialized before it is used.

Rules:

* A read of a local variable (using its value in an expression) MUST be rejected unless the compiler can prove that the variable has been assigned on every control-flow path leading to that read.
* This rule applies even for `var` locals; mutability does not imply an implicit default value.
* For `final var` locals, the compiler MUST additionally enforce that the variable is assigned at most once.

Required errors:

* Reading a local variable that is not definitely initialized at that program point.
* Assigning to a `final var` local more than once.

Examples:

```cloth
let name: string = "Cloth";
var count: i32 = 0;
final var id: i64 = 123;
```

Nullability is expressed on the type:

```cloth
var maybeUser: User? = null;
```

---

### 8.4 Expression Statements

An expression statement evaluates an expression for its side effects.

```cloth
println("Hello");
foo(1, 2, 3);
new Dog();
```

If the expression produces a value, it is discarded.

Rules:

* The expression MUST be a valid expression form.
* Implementations SHOULD warn when an expression statement has no observable effect.

Required errors:

* Using an expression that is not permitted as a statement in the current context (for example, a bare type name).

---

### 8.5 Assignment Statements

Assignment updates an existing variable or member.

Form:

```cloth
<target> = <expr>;
```

Valid assignment targets:

* local variables: `x`
* member access: `obj.field`
* array indexing: `arr[i]`

Rules:

* The assignment target MUST refer to a mutable storage location.
* The right-hand side expression MUST be type-checkable and MUST be assignable to the target type.
* The target expression is evaluated before the right-hand side expression when the target contains subexpressions (for example, `arr[i] = v`).

Required errors:

* Assigning to a `let` binding.
* Assigning to a `final var` binding more than once.
* Assigning to a `const` binding.
* Assigning to a non-assignable expression (for example, a literal or a temporary value).

Examples:

```cloth
x = 10;
player.health = 100;
buffer[i] = 0;
```

Assignment is **not** an expression and cannot appear where an expression is required.

Invalid:

```cloth
if (x = 5) { }  // error
```

---

### 8.6 Compound Assignment (TBD)

Cloth may support compound assignment operators:

* `+=`, `-=`, `*=`, `/=`, `%=` (TBD)

If supported, they MUST be equivalent to:

```cloth
x += y;
```

```cloth
x = x + y;
```

Subject to evaluation-order guarantees.

If compound assignment is introduced, the language MUST specify:

* whether the target expression is evaluated once or multiple times,
* how numeric conversions are applied,
* whether overflow behavior differs from the expanded form.

---

### 8.7 `return`

`return` exits the current function.

Forms:

```cloth
return;
return <expr>;
```

* `return;` is valid only in `void`-returning functions.
* `return <expr>;` requires that `<expr>` is assignable to the function return type.

Rules:

* `return` immediately exits the current function.
* In a non-`void` function, all control-flow paths MUST end in a `return` statement (definite return rules are specified elsewhere).

Required errors:

* `return;` in a non-`void` function.
* `return <expr>;` in a `void` function.
* Returning an expression not assignable to the return type.

Examples:

```cloth
func f(): void {
    return;
}

func g(): i32 {
    return 0;
}
```

---

### 8.8 `if` Statement

Form:

```cloth
if <condition> <block>
[else <block>]
```

The condition expression MUST be of type `bool`.

Rules:

* The condition expression is evaluated first.
* Exactly one of the branches executes.
* Each branch introduces its own block scope.

Required errors:

* A condition expression that is not of type `bool`.
* Missing required blocks.

Examples:

```cloth
if ready {
    start();
}
```

```cloth
if x > 0 {
    println("positive");
} else {
    println("non-positive");
}
```
```cloth
if x >= 10 {
    println("big");
} else if x < 5 {
    println("small");
} else {
    println("medium");
}
```

---

### 8.9 `while` Statement

Form:

```cloth
while <condition> <block>
```

The condition MUST be `bool`.

Rules:

* The condition is evaluated before each loop iteration.
* The loop body is executed only when the condition evaluates to `true`.
* The loop body introduces a block scope.

Required errors:

* A condition expression that is not of type `bool`.

Example:

```cloth
while running {
    tick();
}
```

---

### 8.10 `for` Statement (TBD)

Cloth may support a structured `for` loop.
Exact syntax is TBD.

Possible forms include:

* parenthesized form: `for (init; cond; step) { ... }`
* range form: `for x in range(...) { ... }`

This section is reserved until the loop model is finalized.

If a `for` statement is introduced, it MUST preserve the deterministic evaluation and scoping rules defined in this chapter.

---

### 8.11 `break` and `continue` (TBD)

If included, `break` and `continue` MUST only be valid within loop bodies.

Forms:

```cloth
break;
continue;
```

If included:

* `break` MUST exit the innermost enclosing loop.
* `continue` MUST skip to the next iteration of the innermost enclosing loop.
* Using `break` or `continue` outside a loop MUST be a compile-time error.

---

### 8.12 `defer` (TBD)

Cloth may include `defer` to simplify explicit resource cleanup in manual memory mode.

Example:

```cloth
var f = File.open("a.txt");
defer f.close();
```

This section is reserved until resource management semantics are finalized.

If included, `defer` MUST guarantee that the deferred action runs exactly once when control leaves the current scope, regardless of which statement transfers control (return, break, continue, etc.).

---

### 8.13 `delete` Statement

Manual deallocation uses the `delete` statement.

Form:

```cloth
delete <expr>;
```

The expression MUST evaluate to an owning object (not a `ref`).

Rules:

* `delete` performs explicit deallocation in manual memory mode.
* The operand expression is evaluated exactly once.
* After `delete x;`, the value of `x` is considered invalid for further use unless the language defines a post-delete state (reserved).

Required errors:

* Deleting a non-owning reference.
* Deleting a value that is not deletable under the active memory mode (exact rules are specified in the Memory Model chapter).

Examples:

```cloth
var dog: Dog = new Dog();
delete dog;
```

Invalid:

```cloth
var dog: Dog = new Dog();
var view: ref Dog = dog;
delete view; // error: cannot delete a non-owning reference
```

---

### 8.14 `new` as a Statement

`new` may appear as an expression statement; if its result is unused, the allocated object becomes unreachable.

Example:

```cloth
new Dog();  // allowed, but likely a warning in manual memory mode
```

Compilers SHOULD warn on unused allocations in manual memory mode.

Rules:

* If the allocated value becomes unreachable immediately, the program leaks memory in manual memory mode unless the runtime or compiler performs an optimization (optimizations are not guaranteed).
* Implementations MAY treat unused allocations as a warning or error in strict modes.

---

### 8.15 Deterministic Evaluation Order

Cloth guarantees deterministic evaluation order:

* Statement execution occurs top-to-bottom.
* Expression evaluation is left-to-right.
* Function call arguments are evaluated left-to-right.

This is intentionally strict and is required for predictability.

---

### 8.16 Unreachable Statements (TBD)

The compiler MAY warn or error on statements proven unreachable (e.g., after `return`).

If unreachable-statement diagnostics are enabled:

* The compiler SHOULD warn when a statement is syntactically reachable but semantically unreachable due to constant conditions or prior control-flow termination.
* The compiler MAY treat unreachable statements as errors in strict modes.

---

## 9. Classes, Members, and Initialization Order

This section defines class declarations, member kinds, member modifiers, and the initialization/construction order for instances and static state.

Cloth is class-centric. All executable behavior is declared inside classes.

---

### 9.1 Class Declarations

A class declaration has the form:

```text
[Visibility] class Name [InheritanceClause]? PrimaryConstructor? ClassBody
````

Where:

* `Visibility` is an optional visibility modifier (`public`, `internal`, or `private`).
* `Name` is the declared name of the class.
* `InheritanceClause` (if present) specifies a single base class and any required base-construction arguments.
* `PrimaryConstructor` (if present) is the class header parameter list.
* `ClassBody` contains zero or more member declarations.

Rules:

* A class declaration MUST appear as a top-level declaration in a compilation unit.
* A class declaration MUST NOT appear inside a method body.
* The class name MUST be a valid identifier and MUST NOT be a reserved word.
* Within the same module, two top-level types MUST NOT have the same name.

Required errors:

* Missing or invalid class name.
* Duplicate top-level type name in the same module.
* Use of a reserved word as a class name.

Examples:

```cloth
public class Main(args: string[]) { }
class Animal(food: string) { }
```

#### 9.1.1 Primary Constructors

Classes MAY declare a primary constructor parameter list in the class header:

```cloth
class Animal(food: string) { ... }
```

Each primary constructor parameter MUST create an implicit field with the same name, as follows:

* Visibility: `private`
* Mutability: immutable (equivalent to `final var`)
* Type: the declared parameter type

Thus:

```cloth
class Animal(food: string)
```

is equivalent to having an implicit member:

```cloth
private final var food: string;
```

The implicit fields are accessible within instance members via `this.<name>`.

Rules:

* A primary constructor parameter list, when present, is part of the class declaration header.
* Each parameter MUST have a name and an explicit type.
* Parameter names MUST be unique within the primary constructor parameter list.
* Primary constructor parameters are values supplied at construction time by `new` (or by a factory method that calls `new`).

Required errors:

* Duplicate primary constructor parameter name.
* Missing type on a primary constructor parameter.

---

### 9.2 Member Kinds

A class body contains zero or more **members**. Cloth supports:

* Field declarations
* Method declarations
* Nested type declarations (`class`, `enum`, `struct`, `interface`)

Members MAY be declared as instance members or static members.

Member declarations are not order-dependent.

Rules:

* Within a type body, the meaning of a member reference MUST NOT depend on whether the referenced member is declared textually earlier or later in the file.
* Implementations SHOULD process type bodies in at least two conceptual phases:
    1. A declaration-discovery phase that collects member declarations and their signatures.
    2. A body-checking phase that type-checks member bodies and initializer expressions using the collected declarations.

This model allows methods to call methods declared later in the same type, while still enforcing the initialization-order rules defined elsewhere for instance and static state.

Rules:

* A member declaration MUST appear directly inside a class body or inside a visibility/modifier block that is itself inside a class body.
* A member declaration MUST NOT appear inside a method body.
* Fields and methods MAY be declared in any textual order within a class body.
* Nested type declarations are permitted only where member declarations are permitted.
* A nested type declaration MUST follow the same general declaration rules as a top-level type, except that its name is scoped within the enclosing type.
* Nested type declarations MAY themselves declare further nested types.

Required errors:

* A member declaration appearing in a context where only statements are permitted.

---

### 9.3 Visibility of Members

Visibility may be expressed:

1. Inline on a member declaration, or
2. Using visibility blocks.

Both forms are semantically equivalent.

Default visibility:

* If a member declaration omits inline visibility and does not appear inside a visibility block, its visibility is `private`.

Rules:

* A visibility block applies its visibility to all members declared directly inside the block.
* Visibility blocks MUST NOT change initialization ordering. Ordering is defined solely by source order.
* Visibility blocks MAY contain both fields and methods.

Required errors:

* A visibility block that contains statements.

Example:

```cloth
public {
    var x: i32 = 0;
    func run(): void { }
}

private func helper(): void { }
```

If a member appears inside a visibility block and also has inline visibility, the inline visibility MUST take precedence.

---

### 9.4 Member Modifiers

Cloth supports the following modifiers for class members:

General rules:

* A modifier applies to the member it appears on.
* A modifier block applies its modifier to all members declared directly inside the block.
* If a member has both a block-applied modifier and an inline modifier, the effective modifier set is the combination of both.
* Duplicate modifiers on the same member are permitted only if they are semantically identical; otherwise compilation MUST fail.

#### 9.4.1 `static`

A `static` member belongs to the class itself, not to an instance.

* Static fields have one storage location per class.
* Static methods do not have `this`.

Rules:

* A `static` field initializer expression MUST follow the restrictions defined for static initialization (9.6.2).
* An unqualified identifier inside a static method MUST NOT resolve to an instance member (see 9.7.2.1).

Example:

```cloth
public static {
    final var PI: f64 = 3.14159;
}

public static func main(): i32 { return 0; }
```

#### 9.4.2 `final`

`final` prevents overriding for methods and prevents reassignment for `final var` fields.

`final` on a class prohibits subclassing.

* A `final func` MUST NOT be overridden.
* A `final var` field MUST be assigned exactly once.
* A `final class` MUST NOT be used as a base class.

Example:

```cloth
final func f(): void { }
final var id: i64 = 123;
```

Required errors:

* Assigning to a `final var` field more than once.
* Declaring an override of a `final func` (once `final func` overriding rules are fully specified in the methods chapter).
* Declaring a class that inherits from a `final class`.

#### 9.4.3 `override`

`override` MUST be present when a method overrides a base class method.

Example:

```cloth
override func eat(): void { ... }
```

If `override` is used but no base method is overridden, compilation MUST fail.

Rules:

* `override` applies only to instance methods.
* A method marked `override` MUST match a base method by name and signature according to the method matching rules (specified in the Functions and Methods chapter).

Required errors:

* `override` used on a static method.
* `override` used on a member that is not a method.

---

### 9.5 Static vs Instance Member Blocks

Cloth allows visibility blocks to be combined with `static`:

```cloth
public static {
    final var E: f64 = 2.71828;
}
```

This applies both modifiers to all enclosed members.

Rules:

* A combined block such as `public static { ... }` is equivalent to applying `public` and `static` to each enclosed member.
* A member inside a `static` block MUST be treated as `static` even if `static` is not repeated inline.
* A member inside a non-`static` block is an instance member unless it has an inline `static` modifier.

Required errors:

* A modifier/visibility block that contains statements.

---

## 9.6 Initialization and Construction

This section defines the order in which fields and initializers are evaluated.

### 9.6.1 Deterministic Evaluation

Cloth guarantees deterministic initialization order:

* Declarations are processed top-to-bottom within their category
* Expressions are evaluated left-to-right
* No unspecified initialization order is permitted

This is intentionally strict and is required for predictable systems behavior.

---

### 9.6.2 Static Initialization Order

This section defines **class loading** and **static initialization**.

Cloth is designed to provide deterministic and predictable static initialization suitable for systems and enterprise environments.

#### 9.6.2.1 Definitions

* A class is **loaded** when the compiler/runtime has ensured the class definition is available for use (e.g., metadata, layout, method table). Loading does not execute user code.
* A class is **initialized** when its static state has been computed and is safe to read (i.e., static field initializers have executed).

In v1, the only user-defined behavior in static initialization is evaluation of **static field initializer expressions**.

#### 9.6.2.2 One-time initialization guarantee

For any class `C`, static initialization MUST occur **at most once per program execution**, even if multiple code paths attempt to use `C`.

If the program executes concurrently (for example, due to multiple threads, or due to asynchronous tasks that can run on multiple threads), the implementation MUST still guarantee that:

* Static initialization for a given class is not interleaved with itself.
* All execution contexts observe `C` as either fully uninitialized or fully initialized (no partially initialized reads).

How an implementation enforces this guarantee (for example, by locking, serialization, or waiting) is implementation-defined, but the observable result MUST satisfy the rules above.

#### 9.6.2.3 When initialization happens (initialization triggers)

Static initialization of a class `C` MUST occur before the first *active use* of `C`.

The following actions are **active uses** that trigger initialization of `C` if it is not already initialized:

1. Evaluating `new C(...)`.
2. Reading a non-`const` static field of `C`.
3. Writing a non-`const` static field of `C`.
4. Calling a static method of `C`.

For executable targets, static initialization required for entrypoint execution MUST occur before `main` begins execution.

Non-triggering uses:

* Referring to `C` as a type (e.g., in a variable declaration) does not by itself trigger initialization.
* Reading compile-time constants (`const`) does not trigger initialization if the value is embedded at compile time (exact constant-folding rules are specified in the `const` section).

#### 9.6.2.4 Initialization order within a class

Static initialization order for a class `C`:

1. Identify all static field declarations with initializer expressions.
2. Evaluate those initializer expressions in **source order** within the class body.

Visibility blocks and modifier blocks (`public static { ... }`, `private { ... }`, etc.) MUST NOT affect ordering. Ordering is defined by the order the declarations appear in the source file.

#### 9.6.2.5 Initialization order across inheritance

If class `D` derives from base class `B`, then `B` MUST be fully initialized before `D` is initialized.

This rule applies even if `D` is the first class actively used.

#### 9.6.2.6 Initialization order across dependencies

If, during initialization of class `C`, an initializer expression performs an active use of another class `X`, then initialization of `X` MUST occur before the active use proceeds.

If this requirement would create a cycle (e.g., `A` initialization depends on `B` and `B` depends on `A`), the compiler/runtime MUST detect the cycle and report an error. The exact diagnostic mechanism is implementation-defined, but the program MUST NOT continue with partially initialized classes.

#### 9.6.2.7 Restrictions

Static field initializers MUST be free of instance dependencies.

The following are compile-time errors in a static initializer expression:

* Referencing `this`.
* Referencing any instance field or instance method.
* Referencing any primary constructor parameter.
* Performing any operation that requires constructing an instance of the containing class implicitly.

Static initialization MUST NOT require an instance of the class.

#### 9.6.2.8 Use-before-initialization within static state

Reading a static field of `C` within the initialization of `C` is permitted only if the field is known to have already been initialized by source order.

Reading a static field before it is initialized MUST be a compile-time error where detectable.

#### 9.6.2.9 Example

```cloth
class A() {
    public static {
        final var X: i32 = 1;
    }
}

class B(): A() {
    public static {
        final var Y: i32 = X + 1;
    }
}
```

Here `A.X` is initialized before `B.Y`.

#### 9.6.2.10 Restrictions (Summary)

* Static field initializers MUST NOT reference instance members or primary constructor parameters.
* Static initialization MUST NOT require an instance of the class.

Invalid:

```cloth
class Main(args: string[]) {
    public static {
        var bad: string[] = args; // error: static initializer cannot reference instance state
    }
}
```

---

### 9.6.3 Instance Initialization Order

When constructing an instance of class `C`, the following order is used:

1. Allocate the instance object storage.
2. Initialize implicit primary-constructor fields from the constructor arguments.
3. If `C` has a base class, evaluate the base constructor argument expressions (if any) left-to-right.
4. If `C` has a base class, construct the base subobject and initialize base instance fields according to the rules in 9.6.4.
5. Initialize `C` instance fields with initializer expressions in **source order**.
6. Execute any post-initialization logic (reserved).

> NOTE: In v1, Cloth does not define user-authored constructor bodies or explicit init blocks. If additional post-initialization logic is needed, it MUST be expressed through static factory methods that create instances using `new` and then perform additional work through normal methods.

---

### 9.6.4 Base Class Construction

This section defines inheritance and base construction semantics for classes.

Cloth supports **single inheritance**: a class may have zero or one direct base class.

Inheritance is expressed in the class header using an inheritance clause.

#### 9.6.4.1 Inheritance clause syntax

If a class has a base class, it is written after the primary constructor parameter list:

```cloth
class Derived(<primary_params>): Base(<base_args>) {
    ...
}
```

Notes:

* The base constructor call (the `Base(<base_args>)` portion) is part of the class declaration syntax.
* It is not a statement and is not executed by user code; it defines construction wiring for the type.

Example:

```cloth
class Animal(food: string) {
    func eat(): void {
        println("Eating " + this.food);
    }
}

class Dog(): Animal("Kibble") {
    override func eat(): void {
        println("The dog is eating " + this.food);
    }
}
```

#### 9.6.4.2 Existence and validity of base types

If an inheritance clause is present, the base type name MUST resolve to a valid class type.

The following MUST be compile-time errors:

* The base type does not exist or is not a class.
* The base type is the class itself.
* The inheritance graph would contain a cycle.
* The base class is declared `final`.

#### 9.6.4.3 Required base construction call

Base construction MUST be explicitly represented in the derived class header when the base class requires it.

Rules:

* If the base class declares one or more primary constructor parameters with no defaults, the derived class MUST provide a base constructor call supplying those arguments.
* If the base class primary parameters are all defaulted (or the base declares no primary parameters), the derived class MAY omit the base constructor call. If omitted, it is treated as `: Base()`.

This rule ensures construction is always readable from the class header.

#### 9.6.4.4 Evaluation of base constructor arguments

Base constructor arguments are ordinary expressions.

Evaluation rules:

* Base constructor argument expressions MUST be evaluated **left-to-right**.
* Base constructor argument expressions are evaluated **during construction of the derived instance**, before the base subobject is constructed.
* Base constructor argument expressions MUST NOT reference instance state of the derived object (including `this` and derived fields), because the derived object is not yet fully constructed.

The compiler MUST reject (at compile time) any base argument expression that uses:

* `this`
* any instance field of the derived class
* any instance method call on the derived class (directly or via `this`)

Base argument expressions MAY reference:

* local variables in scope at the construction site
* constants
* static members
* pure expressions that do not depend on `this`

#### 9.6.4.5 Base-before-derived construction guarantee

If class `D` derives from class `B`, then the base portion `B` MUST be constructed before `D` is considered constructed.

More precisely, constructing an instance of `D` conceptually constructs a single object containing:

* a base subobject of type `B`
* a derived extension of type `D`

The base subobject construction MUST complete successfully before derived initialization that depends on base state.

#### 9.6.4.6 Construction and initialization order (normative)

When executing `new D(<args...>)` for a derived class `D` with base class `B`, the construction order is:

1. Allocate storage for the full `D` instance.
2. Initialize `D` primary-constructor implicit fields from the provided arguments (and defaults).
3. Evaluate the base constructor argument expressions `(<base_args>)` left-to-right.
4. Construct the base subobject `B` using those evaluated arguments:
   1. Initialize `B` primary-constructor implicit fields.
   2. Initialize `B` instance fields with initializers in source order.
5. Initialize `D` instance fields with initializer expressions in source order.
6. Execute any post-initialization logic (reserved; see constructor/init TBD sections).

This ordering is intentionally deterministic and MUST NOT be altered.

#### 9.6.4.7 Visibility of members during initialization

During base and derived initialization:

* Within base field initializers and base instance methods, `this` is the full object whose dynamic type will ultimately be `D`, but only the base portion is guaranteed initialized.
* Derived fields MUST be treated as uninitialized while base initialization is running.

Rules:

* Accessing derived instance fields (directly or indirectly) from base field initializers MUST be a compile-time error where detectable.

This prevents use-before-initialization across the inheritance boundary.

#### 9.6.4.8 Overriding interactions during construction

Cloth is designed to avoid surprising dynamic dispatch during construction.

Rules:

* Base initialization code MUST NOT invoke overridable instance methods in a way that can dispatch to derived overrides.
* If a base initializer expression or base method invoked during construction would dynamically dispatch to a derived override, the compiler MUST reject the program as a compile-time error where detectable.

This aligns with Cloth’s goal of predictable, enterprise-friendly construction semantics.

#### 9.6.4.9 Errors

Compilation MUST fail for any of the following:

* Missing required base constructor arguments.
* Too many base constructor arguments.
* Base argument types are not assignable to the base primary parameter types (subject to the type system).
* Base argument expression uses `this` or derived instance state.
* Inheritance cycles or invalid base type resolution.

---

### 9.6.5 Field Initialization Order

Instance fields are initialized in **source order**, regardless of visibility blocks.

Example:

```cloth
class Example() {
    public {
        var a: i32 = 1;
    }

    private {
        var b: i32 = a + 1;
    }
}
```

`a` is initialized before `b`.

Visibility blocks do not affect initialization ordering.

---

### 9.6.6 Use of `this` During Initialization

During instance field initialization:

* `this` refers to the partially constructed object.
* Accessing fields declared later in source order is permitted syntactically, but reading a field before it is initialized MUST be rejected as a compile-time error where detectable.

---

### 9.6.7 Definite Initialization (Required)

Cloth enforces **definite initialization** for all instance fields.

An instance of a class MUST NOT be considered valid unless every instance field has been initialized exactly once during construction.

A field may be initialized by:

- A field initializer expression:

```cloth
var x: i32 = 10;
```

- An implicit primary-constructor assignment:

```cloth
class Animal(food: string) { }
```

- Post-construction setup performed by a factory method (where a factory method creates an instance via `new` and then calls normal instance methods). Factories do not replace definite initialization; all fields MUST still be initialized by primary parameters and/or field initializers in v1.

If any field lacks an initializer and is not assigned during construction, compilation MUST fail.

Example (invalid):

```cloth
class Bad() {
    var x: i32;   // error: not initialized
}
```

---

### 9.6.8 No Implicit Default Values

Cloth does not perform automatic zero-initialization for user-defined fields.

There is **no implicit default value** for:

* numeric types
* object references
* arrays
* user-defined types

All fields must be explicitly initialized.

This rule exists to prevent hidden state and improve correctness in systems contexts.

---

### 9.6.9 Use Before Initialization

Reading a field before it has been initialized is a compile-time error whenever detectable.

Example (invalid):

```cloth
class Example() {
    var a: i32 = b + 1;
    var b: i32 = 5;
}
```

Here `b` is read before initialization and MUST cause a compile-time error.

---

### 9.6.10 Initialization Order is Semantically Significant

Because Cloth uses source-order initialization, reordering fields may change program validity.

This is intentional and MUST NOT be relaxed by the compiler.

---

### 9.6.11 Rationale

Cloth forbids implicit initialization to ensure:

* No hidden allocations or state
* Deterministic construction behavior
* Easier auditing of object lifetimes
* Compatibility with manual-memory and bare-metal targets

---

## 9.7 Member Lookup and Shadowing

This section defines how names resolve to declarations and how shadowing/hiding works.

Cloth’s lookup rules are designed to be:

* deterministic
* readable at the call site
* friendly to tooling and diagnostics

---

### 9.7.1 Scopes and name categories

Cloth has the following relevant name categories during member lookup:

* **Local names**: parameters and local variables declared within a function body.
* **Instance members**: fields and methods declared on the current class (and its base classes).
* **Static members**: fields and methods declared `static` on a class.
* **Types**: type names visible in the current compilation context (module/import rules apply).
* **Nested types**: types declared inside other types (for example, a nested `class`, `enum`, `struct`, or `interface`).

This section specifies lookup for:

* unqualified identifiers (e.g., `x`)
* qualified member access (e.g., `obj.x`, `this.x`, `super.x`, `TypeName.x`)

---

### 9.7.2 Unqualified identifier lookup inside methods

Within a method body, an unqualified identifier `x` MUST resolve using the following ordered search:

1. **Local scope**: parameters and local variables declared in the nearest enclosing block scope.
2. **Enclosing local scopes**: progressively outward block scopes.
3. **Instance members of the current class** (as if written `this.x`).
4. **Instance members of base classes**, walking the base chain from nearest base to furthest.

If no declaration is found, compilation MUST fail.

If multiple declarations would be viable at the same step (e.g., due to future features such as multiple inheritance), compilation MUST fail. (Single inheritance avoids most ambiguity.)

#### 9.7.2.1 Static context

Inside a `static` method:

* Step (3) and (4) do not apply. There is no `this`.
* An unqualified identifier MUST NOT resolve to an instance member.

If an unqualified identifier matches only an instance member name and there is no local variable with that name, compilation MUST fail.

---

### 9.7.3 Qualified member access (`expr.name`)

Member access uses `.` and is left-associative.

To resolve `expr.name`:

1. Determine the static type of `expr`.
2. Search for a member named `name` in that type.
3. If not found, search base types (single inheritance) in order.

If `name` resolves to a nested type on the target type, `expr.name` refers to that nested type name, not to a value.
Using a type name where a value is required MUST be a compile-time error.

If no member is found, compilation MUST fail.

Visibility rules apply:

* `private` members are accessible only within the declaring class.
* `internal` members are accessible only within the declaring module.
* `public` members are accessible everywhere.

If a member exists but is not accessible, compilation MUST fail.

#### 9.7.3.1 Field vs method disambiguation

If `expr.name` refers to a method, it must be invoked with a call expression to execute it:

```cloth
obj.f();
```

Using `obj.f` without a call is only permitted when the language defines a first-class callable/value model for methods (TBD). Until then, `obj.f` without `()` SHOULD be rejected as a compile-time error.

In v1, the compiler MUST reject `obj.f` without `()` as a compile-time error.

---

### 9.7.4 `this` lookup

`this` refers to the current instance.

Rules:

* `this` is only valid inside instance methods and other instance contexts.
* Using `this` in a `static` method MUST be a compile-time error.

`this.x` resolves `x` as an instance member on the current class, then on base classes.

---

### 9.7.5 `super` lookup

`super` refers to the direct base class subobject of the current instance.

Rules:

* `super` is only valid inside instance methods of a class that has a base class.
* Using `super` in a class with no base class MUST be a compile-time error.
* Using `super` in a `static` method MUST be a compile-time error.

`super.x` resolves `x` as an instance member on the direct base class.

If the base class does not have a member named `x` that is accessible from the derived class, compilation MUST fail.

---

### 9.7.6 Static member access (`TypeName.member`)

Static members are accessed through a type name:

```cloth
TypeName.staticField
TypeName.staticFunc(...)
```

Rules:

* `TypeName.member` MUST resolve `member` as a `static` member of `TypeName` (or its base classes).
* If `member` exists only as an instance member, compilation MUST fail.
* If `member` resolves to a nested type declared inside `TypeName`, `TypeName.member` refers to that nested type.

---

### 9.7.7 Shadowing and hiding

This subsection defines name collisions.

#### 9.7.7.1 Local shadowing of members

Local variables and parameters MAY shadow (have the same name as) instance members.

If a local name `x` exists in scope, then unqualified `x` refers to the local name, not the field.

To access the instance member, the program MUST use explicit qualification:

```cloth
this.x
```

Example:

```cloth
class C(x: i32) {
    func f(x: i32): i32 {
        // unqualified x is the parameter
        // this.x is the implicit primary-constructor field
        return x + this.x;
    }
}
```

Compilers SHOULD warn when a local shadows a field, but it is not required.

#### 9.7.7.2 Hiding base members

In a derived class, a declaration with the same name as a base member **hides** that base member for unqualified lookup and for `this.name` lookup.

Rules:

* For methods: if the intent is to override a base method, the derived method MUST be marked `override` (Section 10.7). If a method has the same signature as a base method but is not marked `override`, compilation MUST fail.
* For fields: a derived field MAY hide a base field of the same name. This is allowed but discouraged. If both exist, `this.name` refers to the derived field, and `super.name` refers to the base field.

Example:

```cloth
class B() {
    var x: i32 = 1;
}

class D(): B() {
    var x: i32 = 2;

    func sum(): i32 {
        return this.x + super.x;
    }
}
```

#### 9.7.7.3 Shadowing within local scopes

Shadowing between nested local scopes follows standard block scoping:

* A declaration in an inner block MAY shadow a declaration of the same name in an outer block.
* The outer declaration remains accessible when the inner scope ends.

---

### 9.7.8 Required errors

The compiler MUST reject at least the following cases:

* Use of an unknown identifier.
* Use of `this` in a `static` method.
* Use of `super` in a class with no base class.
* Use of `super` in a `static` method.
* `TypeName.member` where `member` is not `static`.
* `obj.f` where `f` names a method but is used without `()`.
* Any access to a member that is not visible under the visibility rules.
* A derived method that matches a base method signature but omits `override`.

---

## 9.8 Examples

### 9.8.1 Static Constants

```cloth
public class Math() {
    public static {
        final var PI: f64 = 3.14159;
        final var TAU: f64 = 2.0 * PI;
    }
}
```

### 9.8.2 Inheritance + Overriding

```cloth
class Animal(food: string) {
    func eat(): void {
        println("Eating " + this.food);
    }
}

class Dog(): Animal("Kibble") {
    override func eat(): void {
        println("The dog is eating " + this.food);
    }
}
```

### 9.8.3 Nested Types

```cloth
class Container() {

    public {
        class Node(value: i32) { }

        enum Kind {
            A,
            B
        }
    }
}
```

### 9.8.4 Visibility Blocks + Static Blocks

```cloth
public final class Math() {

    private static {
        final var myMathVar: f32 = 3.14159;
    }

    public static {
        final var PI: f32 = 3.14159;
        final var TAU: f32 = 2.0 * PI;
        final var E: f32 = 2.71828;
        final var PHI: f32 = 1.61803;

        func method(): void {
            var a: f32 = PI;
        }
    }
}
```

---
## 9.9 Construction Model (Primary Parameters, Defaults, and Factories)

This section defines the construction model for v1.

Cloth constructs class instances using a **primary constructor parameter list** declared in the class header, combined with field initializer expressions and (when present) an explicit base construction clause.

In v1, Cloth does not define user-authored constructor bodies, secondary constructors, or explicit init/invariant blocks.
All custom setup and validation logic MUST be expressed through normal methods, typically called from static factory methods.

The construction forms provided in v1 are:

1. **Direct construction** using `new` with positional arguments.
2. **Default parameter values** in the class header, used when an argument is omitted.
3. **Static factory methods** declared on the class, which may perform validation and then return an instance.

---

### 9.9.1 Primary Construction

A class MAY declare a parameter list in its header:

```cloth
class Dog(name: string, age: i32) { ... }
````

Creating an instance uses `new` with the class name:

```cloth
var d: Dog = new Dog("Rex", 3);
```

Each header parameter MUST implicitly create a field with the same name:

* Visibility: `private`
* Mutability: immutable (equivalent to `final var`)
* Initialization: from the corresponding constructor argument (or default)

These fields are accessible within instance members as `this.<name>`.

Rules:

* Primary construction with `new` supplies values for the primary constructor parameters.
* Primary constructor parameter values MUST be available before any instance field initializer expressions of the class execute.
* The class’s instance fields (including implicit primary-parameter fields) MUST be fully initialized by the end of construction, as required by 9.6.7.

Required errors:

* Missing required arguments for non-defaulted primary parameters.
* Providing more arguments than there are parameters.

---

### 9.9.2 Default Parameter Values

Primary constructor parameters MAY define default values:

```cloth
class Dog(name: string = "Unnamed", age: i32 = 0) { ... }
```

If a parameter has a default value and no argument is provided for it at construction, the default value is used.

Examples:

```cloth
var a: Dog = new Dog();          // name="Unnamed", age=0
var b: Dog = new Dog("Rex");     // name="Rex", age=0
var c: Dog = new Dog("Rex", 3);  // name="Rex", age=3
```

Rules:

* Arguments are matched by position (TBD: named arguments may be added later).
* A non-defaulted parameter MUST be provided as an argument.
* Default values are evaluated at construction time.
* Default value expressions MUST NOT reference `this` or instance members.

Required errors:

* A default value expression that references `this`, instance fields, or instance methods.
* A default value expression that uses primary parameters declared later in the parameter list (reserved unless explicitly specified).

---

### 9.9.3 Definite Initialization

Cloth enforces definite initialization for all instance fields.

Because primary constructor parameters become implicit fields, any primary parameter without a default MUST be provided at construction, otherwise construction is invalid and compilation MUST fail.

Example (invalid):

```cloth
class Dog(name: string, age: i32 = 0) { }

var d = new Dog();  // error: missing required argument `name`
```

---

### 9.9.4 Static Factory Methods

Classes MAY provide alternate construction paths using static factory methods.

Static factories are regular `static func` members that return an instance of the class.

Rules:

* A static factory method that constructs an instance of its own class MUST do so by calling `new ClassName(...)`.
* A static factory method MAY call other methods (static or instance) to perform validation, normalization, or additional setup after the instance exists.
* A static factory method MUST return an owning instance value of the class type.

Required errors:

* A factory method declared to return the class type that returns a value not assignable to that class type.

Example:

```cloth
class Dog(name: string = "Unnamed", age: i32 = 0) {

    public {
        static func puppy(name: string): Dog {
            return new Dog(name, 0);
        }

        static func fromOwner(owner: string): Dog {
            return new Dog(owner + "'s dog", 0);
        }
    }
}
```

Usage:

```cloth
var a = new Dog();
var b = Dog.puppy("Rex");
var c = Dog.fromOwner("Wylan");
```

Factories provide:

* Explicit naming at call sites
* Centralized validation and setup logic
* A standardized alternative to constructor overloading

---

### 9.9.5 Invariants and Validation (Reserved)

Cloth does not define a language-level invariant or init block.

Rules:

* Any validation and invariant enforcement MUST be expressed through ordinary code, typically implemented in static factory methods and normal instance methods.
* If a program requires that certain conditions hold for all instances of a type, the program is responsible for ensuring that instances are created only through construction paths that enforce those conditions.

---

## 10. Functions and Methods

This section defines function declarations within classes, including parameter syntax, return types, overloading, static vs instance methods, and constructor behavior.

In Cloth, all functions are declared inside classes.

---

### 10.1 Method Declarations

A method declaration has the form:

```text
[Visibility] [static] [final|override]? func Name [TypeParameters]? "(" ParameterList? ")" ":" ReturnType Block
````

Examples:

```cloth
public func run(): void { }

private static func helper(x: i32): i32 {
    return x * 2;
}
```

#### 10.1.1 Parameters

Parameters MUST be declared as:

```text
name ":" Type
```

Example:

```cloth
func add(a: i32, b: i32): i32 {
    return a + b;
}
```

Parameter evaluation order in calls is left-to-right (see Expressions).

---

### 10.2 Return Types

Methods MUST declare a return type using `:`.

```cloth
func f(): i32 { return 0; }
func g(): void { }
```

* A method returning `void` MUST NOT return a value.
* A method returning a non-void type MUST return a value on all control paths. (Definite return.)

Example (invalid):

```cloth
func bad(x: i32): i32 {
    if x > 0 {
        return x;
    }
    // error: missing return on some paths
}
```

---

### 10.3 Static vs Instance Methods

#### 10.3.1 Instance Methods

Instance methods operate on an instance and have access to `this`.

```cloth
func speak(): void {
    println(this.name);
}
```

#### 10.3.2 Static Methods

Static methods belong to the class and do not have `this`.

```cloth
static func main(): i32 { return 0; }
```

Static methods MAY be called without an instance.

---

### 10.4 Visibility Blocks and Inline Visibility

Methods may appear inside visibility blocks or specify inline visibility.

Inline visibility takes precedence over a surrounding block.

---

### 10.5 Overloading

Cloth supports method overloading within a class.

Two methods may share the same name if and only if their parameter type lists differ.

Example:

```cloth
func log(x: i32): void { }
func log(x: string): void { }
```

Overloading MUST NOT be based solely on return type.

Example (invalid):

```cloth
func f(): i32 { return 0; }
func f(): f32 { return 0.0; } // error
```

---

### 10.6 Generic Methods (Type Parameters)

Methods MAY declare type parameters using angle brackets.

Type parameters MUST appear immediately after the method name.

Example:

```cloth
func identity<T>(x: T): T {
    return x;
}
```

Type parameters are scoped to the method.

(TBD: constraints syntax.)

---

### 10.7 Override and Final

#### 10.7.1 Overriding

To override a base class method, the derived method MUST use `override`.

```cloth
override func eat(): void { ... }
```

If `override` is specified and no base method is overridden, compilation MUST fail.

The override must match the base method signature exactly (TBD: covariant return rules).

#### 10.7.2 Final Methods

A method marked `final` MUST NOT be overridden.

```cloth
final func hash(): i32 { ... }
```

If a derived class attempts to override a final method, compilation MUST fail.

---

### 10.8 Entrypoint Method `main`

Executable targets require exactly one entrypoint method named `main` (see Entrypoint section).

The entrypoint must be:

* `static`
* `public` or `internal`
* one of the allowed signatures

Examples:

```cloth
static func main(): i32 { return 0; }
static func main(args: string[]): void { }
```

---

## 10.9 Construction

Cloth does not define separate constructor declarations.

Instance construction is governed exclusively by:

1. The class header parameter list (primary constructor parameters),
2. Default parameter values (if present),
3. Static factory methods (if provided).

See Section 9.9 Construction Model for normative rules.

---

### 10.9.1 Construction via `new`

An instance of a class is created using:

```cloth
new ClassName(args...)
````

The arguments MUST match the class header parameter list, respecting default values where defined.

Example:

```cloth
class Dog(name: string = "Unnamed", age: i32 = 0) { }

var d1 = new Dog();
var d2 = new Dog("Rex");
var d3 = new Dog("Rex", 3);
```

If required parameters are omitted, compilation MUST fail.

---

### 10.9.2 Field Initialization

Primary constructor parameters are assigned to implicit private immutable fields before any instance methods are invoked.

No separate constructor body is executed.

Validation or alternate initialization logic MUST be implemented using:

* Default parameter values, or
* Static factory methods.

---

### 10.9.3 Static Factory Methods

Static factory methods are the preferred mechanism for alternate construction paths.

Example:

```cloth
class Dog(name: string, age: i32) {

    public {
        static func puppy(name: string): Dog {
            return new Dog(name, 0);
        }
    }
}
```

Factories are ordinary static methods and participate in visibility, overloading, and generic rules like any other method.

---

### 10.9.4 No Constructor Overloading

Cloth does not support constructor overloading by signature.

Similar behavior is achieved through:

* Default parameter values, or
* Static factory methods.

---

## 10.10 Function Literal Interop

Function literals evaluate to callable objects that may be passed to methods expecting compatible callable types.

(TBD: callable type syntax and standard library function interfaces.)

---

## 10.11 Deterministic Semantics

Cloth guarantees:

* Left-to-right evaluation of arguments
* Deterministic overload resolution (TBD detailed rules)
* Definite return for non-void methods
* Definite initialization for fields during construction

---

## 11. Meta Accessors (Objects and Primitives)

This chapter defines **meta accessors**, which are built-in operations used to query properties of types and values.

Meta accessors are written using **meta keywords**. Meta keywords are lexed as `Meta` tokens (see 2.3) and are reserved.

Meta accessors are designed to:

* be explicit and tooling-friendly,
* provide deterministic results,
* avoid user-defined overloading or hidden behavior.

Unless specified otherwise, the results of meta accessors are pure (no side effects) and depend only on their input value or input type.

---

### 11.1 Syntax and Basic Rules

Meta accessors use an accessor form that binds to a **target** using `::`:

```text
MetaChain:
    MetaTarget ("::" MetaKeyword)+

MetaTarget:
    Type
  | Expression
```

Examples:

```cloth
i32::SIZEOF
MyStruct::ALIGNOF
x::TO_STRING
let x: i32 = 0;
x::TYPEOF::MAX
```

Rules:

* A meta chain is an expression.
* Meta keywords are case-sensitive and MUST be written exactly as defined (for example, `SIZEOF`, not `sizeof`).
* The meta accessor name MUST be one of the recognized meta keywords.
* The `::` token is required.
* The meta target MUST be either a type or an expression, depending on the specific meta accessor.

Chaining rules:

* A meta chain MAY contain multiple `::MetaKeyword` segments.
* Each segment is applied to the result of the previous segment.
* Each segment MUST receive a compatible input category:
  * type-only meta accessors require that the previous segment yields a type,
  * value-only meta accessors require that the previous segment yields a value.

Determinism:

* The evaluation of the meta target expression (when the target is an expression) follows the normal left-to-right expression evaluation rules.
* A meta chain MUST evaluate its initial meta target expression at most once.

Required errors:

* Unknown meta keyword.
* Missing `::`.
* An empty meta chain (a meta target with no `::MetaKeyword` segment).
* Using a type where a value is required, or using a value where a type is required.
* A meta chain segment applied to an incompatible intermediate result (for example, `x::TO_STRING::MAX`).

---

### 11.2 Compile-time vs Runtime Evaluation

Meta accessors are intended to be computable at compile time whenever their input is fully known.

Rules:

* If the meta accessor operates on a type, and the type is fully known at compile time, the result MUST be a compile-time constant.
* If the meta accessor operates on a value expression, the meta target expression MUST be evaluated first, and the meta accessor result is produced from the resulting value.

In v1, meta accessors operate on the **static type** of values unless a meta accessor explicitly states that it uses the runtime value representation.

Implementations MAY constant-fold meta accessors operating on constant inputs.
Constant folding MUST NOT change observable program behavior.

---

### 11.3 Meta Accessors on Types

The following meta accessors require a **type** as their meta target.

#### 11.3.1 `Type::SIZEOF`

`T::SIZEOF` produces the size of `T` in bytes.

Rules:

* The result type is `i64`.
* `T` MUST be a complete type whose size is known to the implementation.

Required errors:

* `SIZEOF` applied to a value expression.
* `SIZEOF` applied to an incomplete or invalid type.

#### 11.3.2 `Type::ALIGNOF`

`T::ALIGNOF` produces the required alignment of `T` in bytes.

Rules:

* The result type is `i64`.
* `T` MUST be a complete type whose alignment is known to the implementation.

Required errors:

* `ALIGNOF` applied to a value expression.
* `ALIGNOF` applied to an incomplete or invalid type.

#### 11.3.3 `Type::DEFAULT`

`T::DEFAULT` produces the default value for type `T`.

In v1, Cloth does not implicitly default-initialize locals or fields. `DEFAULT` exists as an explicit mechanism for programs that want an explicit default value.

Rules:

* `T::DEFAULT` is an expression whose type is `T`.
* `T` MUST be a type for which a default value is defined.

Notes:

* The set of types that have a defined `DEFAULT` value is implementation-defined until the type defaulting model is fully specified.

Required errors:

* `DEFAULT` applied to a value expression.
* `DEFAULT` applied to a type that has no defined default value.

#### 11.3.4 `Type::LENGTH`, `Type::MAX`, and `Type::MIN`

These meta accessors query properties of integer types.

Rules:

* `T::LENGTH` produces the bit-width of integer type `T`.
* `T::MAX` produces the maximum representable value of integer type `T`.
* `T::MIN` produces the minimum representable value of integer type `T`.
* `T` MUST be an integer type.

Result types:

* `T::LENGTH` has type `i64`.
* `T::MAX` and `T::MIN` have type `T`.

Required errors:

* Applying `LENGTH`, `MAX`, or `MIN` to a non-integer type.
* Applying `LENGTH`, `MAX`, or `MIN` to a value expression.

#### 11.3.5 `TYPEOF`

`TYPEOF` produces a type that can be used as the input to later type-only meta accessors.

Forms:

* `T::TYPEOF`
* `x::TYPEOF`

Rules:

* `T::TYPEOF` yields the type `T`.
* `x::TYPEOF` yields the static type of expression `x`.
* The result of `TYPEOF` is a **type result**, meaning it is valid as the input to later type-only meta accessors in the same meta chain.
* `TYPEOF` does not imply a runtime reflection object.

Examples:

```cloth
let x: i32 = 0;
let m: i32 = x::TYPEOF::MAX;
let w: i64 = x::TYPEOF::LENGTH;
```

Required errors:

* Using `TYPEOF` in a context that requires a runtime value rather than a type result (reserved until a reflection model exists).

---

### 11.4 Meta Accessors on Values

The following meta accessors require a **value expression** as their meta target.

#### 11.4.1 `Expression::TO_STRING`

`x::TO_STRING` converts `x` to a `string` representation.

Rules:

* The result type is `string`.
* `x` MUST be a value expression.

Required errors:

* `TO_STRING` applied to a type.

#### 11.4.2 `Expression::TO_BYTES` and `Expression::TO_BITS`

`x::TO_BYTES` and `x::TO_BITS` convert a value into an explicit sequence of bytes or bits.

Rules:

* The result type of `x::TO_BYTES` is `byte[]`.
* The result type of `x::TO_BITS` is `bit[]`.
* The meaning of the conversion is implementation-defined until the object layout and endianness rules are fully specified.

Required errors:

* Applying `TO_BYTES` or `TO_BITS` to a type.
* Applying `TO_BYTES` or `TO_BITS` to a value whose representation is not defined by the implementation.

#### 11.4.3 `Expression::MEMSPACE` (Reserved)

`x::MEMSPACE` is reserved for querying memory-space information (for example, stack/heap/static placement or address space qualifiers).

In v1, `MEMSPACE` MUST be rejected unless the implementation defines and documents a concrete memory-space model and a concrete return type.

---

### 11.5 Required Diagnostics Summary

At minimum, the compiler MUST reject:

* Using a meta keyword that is not recognized.
* Applying a type-only meta accessor to a value expression.
* Applying a value-only meta accessor to a type.
* Applying an integer-type meta accessor (`LENGTH`, `MAX`, `MIN`) to a non-integer type.
* Applying a meta accessor to an incompatible intermediate result within a meta chain.
* Using reserved meta accessors (`MEMSPACE`) unless their semantics are explicitly defined by the implementation.
