package cloth.parser.statements;

import cloth.parser.expressions.Expression;
import cloth.token.IToken;
import cloth.token.span.SourceSpan;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Sealed interface representing all statement forms in the Cloth language.
 * Statements exist only inside method bodies.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public sealed interface Statement {

    SourceSpan span();

    record Block(List<Statement> statements, SourceSpan span) implements Statement {}

    record VarDeclaration(
        FieldParser.BindingKind binding,
        IToken bindingToken,
        IToken name,
        TypeReferenceParser.TypeReference type,
        @Nullable Expression initializer,
        SourceSpan span
    ) implements Statement {}

    record ExpressionStmt(Expression expression, SourceSpan span) implements Statement {}

    /** Plain {@code =} or compound {@code +=  -=  *=  /=  %=} assignment. */
    record Assignment(Expression target, IToken operator, Expression value, SourceSpan span) implements Statement {}

    /** Prefix or postfix {@code ++} / {@code --}. */
    record IncrementDecrement(Expression target, IToken operator, boolean prefix, SourceSpan span) implements Statement {}

    record Return(IToken keyword, @Nullable Expression value, SourceSpan span) implements Statement {}

    record If(Expression condition, Block thenBlock, @Nullable Statement elseBlock, SourceSpan span) implements Statement {}

    record While(Expression condition, Block body, SourceSpan span) implements Statement {}

    /**
     * C-style for loop: {@code for (init?; condition?; step?) { body }}
     * <p>
     * {@code init} is either a {@link VarDeclaration} or an {@link Assignment} (or null).
     * {@code step} is an {@link Assignment}, {@link IncrementDecrement}, or null.
     */
    record For(
        @Nullable Statement init,
        @Nullable Expression condition,
        @Nullable Statement step,
        Block body,
        SourceSpan span
    ) implements Statement {}

    /** Foreach loop: {@code for (name in arrayExpr) { body }} */
    record ForEach(IToken name, Expression iterable, Block body, SourceSpan span) implements Statement {}

    record Break(IToken keyword, SourceSpan span) implements Statement {}

    record Continue(IToken keyword, SourceSpan span) implements Statement {}

    record Delete(Expression target, SourceSpan span) implements Statement {}

    /** {@code defer <callExpr>;} — deferred call, runs at scope exit in LIFO order. */
    record Defer(IToken keyword, Expression.Call call, SourceSpan span) implements Statement {}
}
