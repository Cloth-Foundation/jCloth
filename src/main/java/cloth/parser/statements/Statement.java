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

    record Assignment(Expression target, IToken operator, Expression value, SourceSpan span) implements Statement {}

    record Return(IToken keyword, @Nullable Expression value, SourceSpan span) implements Statement {}

    record If(Expression condition, Block thenBlock, @Nullable Statement elseBlock, SourceSpan span) implements Statement {}

    record While(Expression condition, Block body, SourceSpan span) implements Statement {}

    record Delete(Expression target, SourceSpan span) implements Statement {}
}
