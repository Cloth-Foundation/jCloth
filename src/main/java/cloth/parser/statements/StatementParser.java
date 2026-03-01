package cloth.parser.statements;

import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.parser.ParserPart;
import cloth.parser.expressions.Expression;
import cloth.parser.expressions.ExpressionParser;
import cloth.token.IToken;
import cloth.token.TokenKind;
import cloth.token.Tokens;
import cloth.token.span.SourceSpan;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Parses the contents of a brace-delimited block into a {@link Statement.Block}.
 * <p>
 * The {@link #parse()} entry point expects the opening and closing braces.
 * The {@link #parseBlock()} method parses only the interior statements and
 * is used when the caller has already consumed the opening brace (e.g. FuncParser).
 */
public class StatementParser extends ParserPart<Statement.Block> {

    public StatementParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    @Override
    @SneakyThrows
    public Statement.Block parse() {
        expect(Tokens.Operator.LeftBrace, () ->
            new CompileError("Expected '{'", peek().span(),
                "Expected opening brace for block.",
                "{ stmt; }"));

        Statement.Block block = parseBlock();

        expect(Tokens.Operator.RightBrace, () ->
            new CompileError("Expected '}'", peek().span(),
                "Expected closing brace for block.",
                "{ stmt; }"));

        return block;
    }

    /**
     * Parses statements until a closing brace or end-of-file is reached.
     * The opening brace must already be consumed; the closing brace is
     * left unconsumed for the caller.
     */
    @SneakyThrows
    public Statement.Block parseBlock() {
        var statements = new ArrayList<Statement>();
        IToken firstToken = peek();

        while (!is(Tokens.Operator.RightBrace) && !isEndOfFile()) {
            statements.add(parseStatement());
        }

        // span covers the first through last statement; empty blocks get a zero-width span
        SourceSpan span = statements.isEmpty()
            ? new SourceSpan(firstToken.span().start(), firstToken.span().start())
            : new SourceSpan(statements.getFirst().span().start(), statements.getLast().span().end());

        return new Statement.Block(statements, span);
    }

    // region Statement dispatch

    @SneakyThrows
    private Statement parseStatement() {
        if (is(Tokens.Keyword.Var) || is(Tokens.Keyword.Let) || is(Tokens.Keyword.Const)) {
            return parseVarDeclaration();
        }
        if (is(Tokens.Keyword.Return)) {
            return parseReturn();
        }
        if (is(Tokens.Keyword.If)) {
            return parseIf();
        }
        if (is(Tokens.Keyword.While)) {
            return parseWhile();
        }
        if (is(Tokens.Keyword.Delete)) {
            return parseDelete();
        }
        if (is(Tokens.Operator.LeftBrace)) {
            return parseNestedBlock();
        }
        return parseExpressionOrAssignment();
    }

    // endregion

    // region Variable declaration

    @SneakyThrows
    private Statement.VarDeclaration parseVarDeclaration() {
        FieldParser.BindingKind binding;
        IToken bindingToken;
        if (is(Tokens.Keyword.Var)) {
            binding = FieldParser.BindingKind.VAR;
            bindingToken = advance();
        } else if (is(Tokens.Keyword.Let)) {
            binding = FieldParser.BindingKind.LET;
            bindingToken = advance();
        } else {
            binding = FieldParser.BindingKind.CONST;
            bindingToken = advance();
        }

        IToken name = expect(TokenKind.Identifier, () ->
            new CompileError("Expected variable name", peek().span(),
                "Expected an identifier for the variable name.",
                "var x: i32 = 0;"));

        expect(Tokens.Operator.Colon, () ->
            new CompileError("Expected ':'", peek().span(),
                "Expected ':' between variable name and type.",
                "var x: i32;"));

        TypeReferenceParser.TypeReference type =
            new TypeReferenceParser(getLexer(), getFile()).parse();

        Expression initializer = null;
        if (is(Tokens.Operator.Assign)) {
            advance();
            initializer = new ExpressionParser(getLexer(), getFile()).parse();
        }

        IToken semi = expectSemiColon();
        return new Statement.VarDeclaration(binding, bindingToken, name, type, initializer,
            new SourceSpan(bindingToken.span().start(), semi.span().end()));
    }

    // endregion

    // region Return

    @SneakyThrows
    private Statement.Return parseReturn() {
        IToken keyword = advance(); // consume 'return'

        @Nullable Expression value = null;
        if (!is(Tokens.Operator.Semicolon)) {
            value = new ExpressionParser(getLexer(), getFile()).parse();
        }

        IToken semi = expectSemiColon();
        return new Statement.Return(keyword, value,
            new SourceSpan(keyword.span().start(), semi.span().end()));
    }

    // endregion

    // region If / else

    @SneakyThrows
    private Statement.If parseIf() {
        IToken keyword = advance(); // consume 'if'
        boolean hasParentheses = is(Tokens.Operator.LeftParen);

        if (hasParentheses) {
            advance();
        }

        Expression condition = new ExpressionParser(getLexer(), getFile()).parse();

        if (hasParentheses) {
            expect(Tokens.Operator.RightParen, () ->
                new CompileError("Expected ')'", peek().span(),
                    "Expected closing parenthesis for if condition.",
                    "if (condition) { }"));
        }

        expect(Tokens.Operator.LeftBrace, () ->
            new CompileError("Expected '{'", peek().span(),
                "Expected opening brace for if body.",
                "if condition { }"));

        Statement.Block thenBlock = parseBlock();

        IToken thenClose = expect(Tokens.Operator.RightBrace, () ->
            new CompileError("Expected '}'", peek().span(),
                "Expected closing brace for if body.",
                "if condition { }"));

        @Nullable Statement elseBlock = null;
        if (is(Tokens.Keyword.Else)) {
            advance(); // consume 'else'
            if (is(Tokens.Keyword.If)) {
                elseBlock = parseIf();
            } else {
                expect(Tokens.Operator.LeftBrace, () ->
                    new CompileError("Expected '{' or 'if'", peek().span(),
                        "Expected opening brace for else body, or 'if' for else-if chain.",
                        "else { }"));

                Statement.Block elseBody = parseBlock();

                IToken elseClose = expect(Tokens.Operator.RightBrace, () ->
                    new CompileError("Expected '}'", peek().span(),
                        "Expected closing brace for else body.",
                        "else { }"));

                elseBlock = new Statement.Block(elseBody.statements(),
                    new SourceSpan(elseBody.span().start(), elseClose.span().end()));
            }
        }

        SourceSpan span = new SourceSpan(
            keyword.span().start(),
            elseBlock != null ? elseBlock.span().end() : thenClose.span().end()
        );

        return new Statement.If(condition, thenBlock, elseBlock, span);
    }

    // endregion

    // region While

    @SneakyThrows
    private Statement.While parseWhile() {
        IToken keyword = advance(); // consume 'while'
        boolean hasParentheses = is(Tokens.Operator.LeftParen);

        if (hasParentheses) {
            advance();
        }

        Expression condition = new ExpressionParser(getLexer(), getFile()).parse();

        if (hasParentheses) {
            expect(Tokens.Operator.RightParen, () ->
                    new CompileError("Expected ')'", peek().span(),
                            "Expected closing parenthesis for while condition.",
                            "while (condition) { }"));
        }

        expect(Tokens.Operator.LeftBrace, () ->
            new CompileError("Expected '{'", peek().span(),
                "Expected opening brace for while body.",
                "while condition { }"));

        Statement.Block body = parseBlock();

        IToken closeBrace = expect(Tokens.Operator.RightBrace, () ->
            new CompileError("Expected '}'", peek().span(),
                "Expected closing brace for while body.",
                "while condition { }"));

        return new Statement.While(condition, body,
            new SourceSpan(keyword.span().start(), closeBrace.span().end()));
    }

    // endregion

    // region Delete

    @SneakyThrows
    private Statement.Delete parseDelete() {
        IToken keyword = advance(); // consume 'delete'

        Expression target = new ExpressionParser(getLexer(), getFile()).parse();

        IToken semi = expectSemiColon();
        return new Statement.Delete(target,
            new SourceSpan(keyword.span().start(), semi.span().end()));
    }

    // endregion

    // region Nested block

    @SneakyThrows
    private Statement.Block parseNestedBlock() {
        IToken open = advance(); // consume '{'

        Statement.Block block = parseBlock();

        IToken close = expect(Tokens.Operator.RightBrace, () ->
            new CompileError("Expected '}'", peek().span(),
                "Expected closing brace for block.",
                "{ stmt; }"));

        return new Statement.Block(block.statements(),
            new SourceSpan(open.span().start(), close.span().end()));
    }

    // endregion

    // region Expression or assignment

    @SneakyThrows
    private Statement parseExpressionOrAssignment() {
        Expression expr = new ExpressionParser(getLexer(), getFile()).parse();

        if (is(Tokens.Operator.Assign)) {
            IToken op = advance();
            Expression value = new ExpressionParser(getLexer(), getFile()).parse();
            IToken semi = expectSemiColon();
            return new Statement.Assignment(expr, op, value,
                new SourceSpan(expr.span().start(), semi.span().end()));
        }

        IToken semi = expectSemiColon();
        return new Statement.ExpressionStmt(expr,
            new SourceSpan(expr.span().start(), semi.span().end()));
    }

    // endregion
}
