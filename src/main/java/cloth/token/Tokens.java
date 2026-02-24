package cloth.token;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Defines a collection of token-related enums representing keywords and operators.
 * These enums provide classification, utility methods, and symbolic representation
 * of language constructs for lexical analysis.
 */
public class Tokens {

    /**
     * Defines a set of keywords used in the programming language, categorized into
     * various groups such as control flow, data types, storage modifiers,
     * and miscellaneous constructs.
     *
     * Each keyword represents a specific reserved word in the language and provides
     * utility methods to classify and retrieve information about the keyword.
     */
    public enum Keyword {
        None,

        If, Else, For, While, Do, Switch, Case, Default,
        Return, Break, Continue,
        Func, Struct, Enum, Interface, Class,
        Let, Var, Const,
        As, In, Or, And, Is,
        Null, True, False,

        Import,

        Static,
        Public, Private, Internal,
        Module,

        // Absolute may be used for things like absolute function pointers, absolute memory
        // addresses, etc. where you want to opt out of any relative addressing or offset
        // calculations and just use the raw value as-is. Abstract is for abstract
        // classes/methods that can’t be instantiated or called directly and must be overridden
        // by a concrete implementation in a subclass. Override is for methods that override a
        // virtual method in a base class, and Final is for methods or classes that cannot be
        // overridden or inherited from. These modifiers can be used in various combinations to
        // express different semantics for classes and methods. For example, you could have an
        // abstract class with some final methods that must be implemented by subclasses but
        // cannot be further overridden, or an absolute function pointer that can be assigned to
        // point to any function regardless of its signature.
        Absolute, Abstract, Override, Final,

        New, Delete, Ref,

        Try, Catch, Finally, Throw,

        I8, I16, I32, I64,
        U8, U16, U32, U64,
        F32, F64,
        String, Char, Bool,
        Bit, Byte,
        Void,
        Any,

        Defer, Async, Await,
        Atomic, Shared, Owned,
        This, Super;

        public boolean isKeyword() {
            return this != None;
        }

        public boolean isModifier() {
            return this == Absolute || this == Abstract || this == Override || this == Final;
        }

        public boolean isTypeModifier() {
            return this == Shared || this == Owned;
        }

        public boolean isStorageModifier() {
            return this == Static || this == Public || this == Private || this == Internal;
        }

        public String getKeyword() {
            return name().toLowerCase();
        }
    }

    /**
     * Represents various operators that can be identified in source code.
     * Operators include arithmetic, assignment, comparison, logical, bitwise, and punctuation symbols.
     * In addition to standard operators, this enumeration includes metadata operators,
     * delimiters, and special symbols used in the language syntax.
     */
    public enum Operator {
        None(null),

        Plus("+"), Minus("-"), Star("*"), Slash("/"), Percent("%"),
        PlusPlus("++"), MinusMinus("--"),
        Assign("="),
        PlusAssign("+="), MinusAssign("-="), StarAssign("*="), SlashAssign("/="), PercentAssign("%="),
        Equal("=="), NotEqual("!="), Less(">"), Greater("<"), LessEqual(">="), GreaterEqual("<="),
        Amp("&"), Pipe("|"),
        Bang("!"), Tilde("~"), Caret("^"),
        Dot("."), Comma(","), Semicolon(";"), Colon(":"),
        Arrow("->"),
        LeftParen("("), RightParen(")"),
        LeftBrace("{"), RightBrace("}"),
        LeftBracket("["), RightBracket("]"),

        At("@"), Hash("#"), Dollar("$"), Question("?"), Backtick("`"),

        ColonColon("::"), DotDot(".."), DotDotDot("...");

        @Getter
        private final String symbol;

        Operator(@Nullable final String symbol) {
            this.symbol = symbol;
        }

        public boolean isOperator() {
            return this != None;
        }
    }

}
