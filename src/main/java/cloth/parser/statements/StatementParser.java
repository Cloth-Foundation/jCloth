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
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
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
        if (is(Tokens.Keyword.For)) {
            return parseFor();
        }
        if (is(Tokens.Keyword.Break)) {
            return parseBreak();
        }
        if (is(Tokens.Keyword.Continue)) {
            return parseContinue();
        }
        if (is(Tokens.Keyword.Delete)) {
            return parseDelete();
        }
        if (is(Tokens.Operator.PlusPlus) || is(Tokens.Operator.MinusMinus)) {
            return parsePrefixIncrementDecrement();
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

    // region For

    /**
     * Parses either a C-style for loop or a foreach loop.
     * Disambiguates by looking for {@code <name> in} after the opening paren.
     */
    @SneakyThrows
    private Statement parseFor() {
        IToken keyword = advance(); // consume 'for'

        expect(Tokens.Operator.LeftParen, () ->
            new CompileError("Expected '('", peek().span(),
                "Expected opening parenthesis for 'for' loop header.",
                "for (var i: i32 = 0; i < 10; i = i + 1) { }"));

        if (isForEachHeader()) {
            return parseForEach(keyword);
        }

        return parseCStyleFor(keyword);
    }

    /**
     * Peeks ahead to decide if the for-header is a foreach ({@code <ident> in ...}).
     */
    private boolean isForEachHeader() {
        return is(TokenKind.Identifier) && peek(1).is(TokenKind.Keyword)
            && ((cloth.token.Token) peek(1)).keyword() == Tokens.Keyword.In;
    }

    @SneakyThrows
    private Statement.ForEach parseForEach(IToken keyword) {
        IToken name = advance(); // consume identifier
        advance(); // consume 'in'

        Expression iterable = new ExpressionParser(getLexer(), getFile()).parse();

        expect(Tokens.Operator.RightParen, () ->
            new CompileError("Expected ')'", peek().span(),
                "Expected closing parenthesis for foreach header.",
                "for (x in array) { }"));

        expect(Tokens.Operator.LeftBrace, () ->
            new CompileError("Expected '{'", peek().span(),
                "Expected opening brace for foreach body.",
                "for (x in array) { }"));

        Statement.Block body = parseBlock();

        IToken closeBrace = expect(Tokens.Operator.RightBrace, () ->
            new CompileError("Expected '}'", peek().span(),
                "Expected closing brace for foreach body.",
                "for (x in array) { }"));

        return new Statement.ForEach(name, iterable, body,
            new SourceSpan(keyword.span().start(), closeBrace.span().end()));
    }

    @SneakyThrows
    private Statement.For parseCStyleFor(IToken keyword) {
        @Nullable Statement init = null;
        if (!is(Tokens.Operator.Semicolon)) {
            init = parseForInit();
        }
        expect(Tokens.Operator.Semicolon, () ->
            new CompileError("Expected ';'", peek().span(),
                "Expected semicolon after for-loop initializer.",
                "for (var i: i32 = 0; i < 10; i = i + 1) { }"));

        @Nullable Expression condition = null;
        if (!is(Tokens.Operator.Semicolon)) {
            condition = new ExpressionParser(getLexer(), getFile()).parse();
        }
        expect(Tokens.Operator.Semicolon, () ->
            new CompileError("Expected ';'", peek().span(),
                "Expected semicolon after for-loop condition.",
                "for (var i: i32 = 0; i < 10; i = i + 1) { }"));

        @Nullable Statement step = null;
        if (!is(Tokens.Operator.RightParen)) {
            step = parseForStep();
        }
        expect(Tokens.Operator.RightParen, () ->
            new CompileError("Expected ')'", peek().span(),
                "Expected closing parenthesis for for-loop header.",
                "for (var i: i32 = 0; i < 10; i = i + 1) { }"));

        expect(Tokens.Operator.LeftBrace, () ->
            new CompileError("Expected '{'", peek().span(),
                "Expected opening brace for for-loop body.",
                "for (...) { }"));

        Statement.Block body = parseBlock();

        IToken closeBrace = expect(Tokens.Operator.RightBrace, () ->
            new CompileError("Expected '}'", peek().span(),
                "Expected closing brace for for-loop body.",
                "for (...) { }"));

        return new Statement.For(init, condition, step, body,
            new SourceSpan(keyword.span().start(), closeBrace.span().end()));
    }

    /**
     * Parses the init clause of a C-style for loop.
     * Either a var/let/const declaration (without trailing semicolon)
     * or an assignment / increment-decrement.
     */
    @SneakyThrows
    private Statement parseForInit() {
        if (is(Tokens.Keyword.Var) || is(Tokens.Keyword.Let) || is(Tokens.Keyword.Const)) {
            return parseForVarDeclaration();
        }
        return parseForAssignmentOrIncrement();
    }

    /**
     * Parses a var/let/const declaration inside a for-init without consuming the trailing semicolon.
     */
    @SneakyThrows
    private Statement.VarDeclaration parseForVarDeclaration() {
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
                "var i: i32 = 0"));

        expect(Tokens.Operator.Colon, () ->
            new CompileError("Expected ':'", peek().span(),
                "Expected ':' between variable name and type.",
                "var i: i32 = 0"));

        TypeReferenceParser.TypeReference type =
            new TypeReferenceParser(getLexer(), getFile()).parse();

        Expression initializer = null;
        if (is(Tokens.Operator.Assign)) {
            advance();
            initializer = new ExpressionParser(getLexer(), getFile()).parse();
        }

        SourceSpan span = new SourceSpan(
            bindingToken.span().start(),
            initializer != null ? initializer.span().end() : type.span().end()
        );

        return new Statement.VarDeclaration(binding, bindingToken, name, type, initializer, span);
    }

    /**
     * Parses an assignment ({@code target = expr} or compound {@code target += expr})
     * without consuming a trailing semicolon. Used for for-init and for-step.
     */
    @SneakyThrows
    private Statement parseForAssignmentOrIncrement() {
        if (is(Tokens.Operator.PlusPlus) || is(Tokens.Operator.MinusMinus)) {
            IToken op = advance();
            Expression target = new ExpressionParser(getLexer(), getFile()).parse();
            return new Statement.IncrementDecrement(target, op, true,
                new SourceSpan(op.span().start(), target.span().end()));
        }

        Expression target = new ExpressionParser(getLexer(), getFile()).parse();

        if (is(Tokens.Operator.PlusPlus) || is(Tokens.Operator.MinusMinus)) {
            IToken op = advance();
            return new Statement.IncrementDecrement(target, op, false,
                new SourceSpan(target.span().start(), op.span().end()));
        }

        if (!isAssignmentOperator()) {
            throw new CompileError("Expected assignment operator", peek().span(),
                "For-loop header requires a variable declaration, assignment, or increment/decrement.",
                "i = i + 1");
        }

        IToken op = advance();
        Expression value = new ExpressionParser(getLexer(), getFile()).parse();

        return new Statement.Assignment(target, op, value,
            new SourceSpan(target.span().start(), value.span().end()));
    }

    @SneakyThrows
    private Statement parseForStep() {
        return parseForAssignmentOrIncrement();
    }

    // endregion

    // region Break / Continue

    @SneakyThrows
    private Statement.Break parseBreak() {
        IToken keyword = advance(); // consume 'break'
        IToken semi = expectSemiColon();
        return new Statement.Break(keyword,
            new SourceSpan(keyword.span().start(), semi.span().end()));
    }

    @SneakyThrows
    private Statement.Continue parseContinue() {
        IToken keyword = advance(); // consume 'continue'
        IToken semi = expectSemiColon();
        return new Statement.Continue(keyword,
            new SourceSpan(keyword.span().start(), semi.span().end()));
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

    // region Increment / Decrement

    @SneakyThrows
    private Statement.IncrementDecrement parsePrefixIncrementDecrement() {
        IToken op = advance(); // consume ++ or --
        Expression target = new ExpressionParser(getLexer(), getFile()).parse();
        IToken semi = expectSemiColon();
        return new Statement.IncrementDecrement(target, op, true,
            new SourceSpan(op.span().start(), semi.span().end()));
    }

    // endregion

    // region Expression or assignment

    private boolean isAssignmentOperator() {
        return is(Tokens.Operator.Assign)
            || is(Tokens.Operator.PlusAssign)
            || is(Tokens.Operator.MinusAssign)
            || is(Tokens.Operator.StarAssign)
            || is(Tokens.Operator.SlashAssign)
            || is(Tokens.Operator.PercentAssign);
    }

    @SneakyThrows
    private Statement parseExpressionOrAssignment() {
        Expression expr = new ExpressionParser(getLexer(), getFile()).parse();

        if (isAssignmentOperator()) {
            IToken op = advance();
            Expression value = new ExpressionParser(getLexer(), getFile()).parse();
            IToken semi = expectSemiColon();
            return new Statement.Assignment(expr, op, value,
                new SourceSpan(expr.span().start(), semi.span().end()));
        }

        if (is(Tokens.Operator.PlusPlus) || is(Tokens.Operator.MinusMinus)) {
            IToken op = advance();
            IToken semi = expectSemiColon();
            return new Statement.IncrementDecrement(expr, op, false,
                new SourceSpan(expr.span().start(), semi.span().end()));
        }

        IToken semi = expectSemiColon();
        return new Statement.ExpressionStmt(expr,
            new SourceSpan(expr.span().start(), semi.span().end()));
    }

    // endregion
}
