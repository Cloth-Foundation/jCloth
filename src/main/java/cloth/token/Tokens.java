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
     * <p>
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

        /**
         * Determines if the current instance represents a valid programming language keyword.
         *
         * @return {@code true} if the instance is a keyword, {@code false} if it is {@code None}.
         */
        public boolean isKeyword() {
            return this != None;
        }

        /**
         * Determines if the current instance represents a keyword classified as a "modifier".
         * Modifiers are specific keywords that adjust or define the properties or behavior
         * of other programming constructs, such as classes, methods, or variables.
         *
         * @return true if the instance is one of the predefined modifier keywords
         *         (Absolute, Abstract, Override, or Final); false otherwise.
         */
        public boolean isModifier() {
            return this == Absolute || this == Abstract || this == Override || this == Final;
        }

        /**
         * Determines if the current instance represents a type modifier keyword.
         * Type modifiers are specific keywords that define ownership or usage
         * semantics typically scoped to types or variables.
         *
         * @return true if the instance is either {@code Shared} or {@code Owned};
         *         false otherwise.
         */
        public boolean isTypeModifier() {
            return this == Shared || this == Owned;
        }

        /**
         * Determines if the current instance represents a keyword classified
         * as a "storage modifier." Storage modifiers are specific keywords
         * that define access levels or storage declarations related to
         * programming constructs, such as variables or functions.
         *
         * @return true if the instance is one of the predefined storage
         *         modifier keywords (Static, Public, Private, or Internal);
         *         false otherwise.
         */
        public boolean isStorageModifier() {
            return this == Static || this == Public || this == Private || this == Internal;
        }

        /**
         * Converts the name of the current instance to its lowercase representation.
         * This can be useful for cases where a consistent format for keyword handling
         * is required.
         *
         * @return a string containing the lowercase representation of the current instance's name.
         */
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

        /**
         * Represents the textual symbol associated with an operator.
         * This symbol corresponds to the exact string representation of the operator
         * as it appears in the source code (e.g., "+", "-", "*", "==", etc.).
         * It is used to uniquely identify the operator for lexical and syntactic analysis.
         */
        @Getter
        private final String symbol;

        /**
         * Constructs an {@code Operator} with the specified symbol.
         * The symbol represents the textual string associated with the operator,
         * which is typically used to identify the operator during lexical analysis.
         *
         * @param symbol the string representation of the operator, or {@code null} if the operator has no associated symbol.
         */
        Operator(@Nullable final String symbol) {
            this.symbol = symbol;
        }

        /**
         * Determines whether the current enum instance represents an operator.
         * <p>
         * In the context of the {@code Operator} enum, all instances except {@code None}
         * are considered to represent valid operators. This method can be used to
         * distinguish meaningful operators from placeholder or default values.
         *
         * @return {@code true} if the current enum instance is not {@code None}, indicating
         *         that it represents a valid operator; otherwise {@code false}.
         */
        public boolean isOperator() {
            return this != None;
        }
    }

}
