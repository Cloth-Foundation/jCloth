package cloth.parser.expressions;

import cloth.parser.statements.TypeReferenceParser;
import cloth.token.IToken;
import cloth.token.span.SourceSpan;

import java.util.List;

/**
 * Sealed interface representing all expression forms in the Cloth language.
 * Each variant is a record carrying the relevant AST children and a {@link SourceSpan}.
 */
public sealed interface Expression {

    SourceSpan span();

    // --- Primary expressions ---

    record Literal(IToken value, SourceSpan span) implements Expression {}

    record Identifier(IToken name, SourceSpan span) implements Expression {}

    record ThisExpr(IToken token, SourceSpan span) implements Expression {}

    record Group(Expression inner, SourceSpan span) implements Expression {}

    // --- Unary / Binary / Ternary ---

    record Unary(IToken operator, Expression operand, SourceSpan span) implements Expression {}

    record Binary(Expression left, IToken operator, Expression right, SourceSpan span) implements Expression {}

    record Ternary(Expression condition, Expression thenExpr, Expression elseExpr, SourceSpan span) implements Expression {}

    // --- Postfix ---

    record MemberAccess(Expression receiver, IToken member, SourceSpan span) implements Expression {}

    record Call(Expression callee, List<Expression> arguments, SourceSpan span) implements Expression {}

    record Index(Expression receiver, Expression index, SourceSpan span) implements Expression {}

    // --- Special forms ---

    record NewExpr(TypeReferenceParser.TypeReference type, List<Expression> arguments, SourceSpan span) implements Expression {}

    record Cast(Expression expression, TypeReferenceParser.TypeReference targetType, SourceSpan span) implements Expression {}
}
