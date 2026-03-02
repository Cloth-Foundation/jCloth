package cloth.parser.statements;

import cloth.error.CommonErrors;
import cloth.error.errors.SyntaxError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.parser.ParserPart;
import cloth.parser.expressions.Expression;
import cloth.parser.expressions.ExpressionParser;
import cloth.token.IToken;
import cloth.token.Token;
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

    /**
     * Parses a block statement enclosed within braces ('{' and '}').
     * The method expects an opening brace before the block content and a closing brace
     * after the block content. If the expected braces are not found, it throws a compile error.
     *
     * @return A {@link Statement.Block} object representing the parsed block of statements.
     */
    @Override
    @SneakyThrows
    public Statement.Block parse() {
        expect(Tokens.Operator.LeftBrace, CommonErrors.EXPECTED_OPEN_BRACE);
        Statement.Block block = parseBlock();
        expect(Tokens.Operator.RightBrace, CommonErrors.EXPECTED_CLOSE_BRACE);

        return block;
    }

    /**
     * Parses a block of statements enclosed by a right brace and returns
     * it as a {@link Statement.Block}.
     * <p>
     * The method reads and processes statements starting from the current token
     * until it encounters a right brace or the end of file. It calculates the
     * source span of the block based on the positions of the first and last
     * statements. For empty blocks, it assigns a zero-width span at the position
     * of the first token.
     *
     * @return a {@link Statement.Block} containing the parsed statements and
     *         the computed source span.
     */
    @SneakyThrows
    public Statement.Block parseBlock() {
        var statements = new ArrayList<Statement>();
        IToken firstToken = peek();

        while (!is(Tokens.Operator.RightBrace) && !isEndOfFile()) statements.add(parseStatement());

        // span covers the first through last statement; empty blocks get a zero-width span
        SourceSpan span = statements.isEmpty() ? new SourceSpan(firstToken.span().start(), firstToken.span().start()) : new SourceSpan(statements.getFirst().span().start(), statements.getLast().span().end());

        return new Statement.Block(statements, span);
    }

    /**
     * Parses a single statement based on the current token in the token stream.
     * The statement type is determined by inspecting the keyword associated
     * with the current token and dispatching to the appropriate parsing method.
     * The supported statement types include variable declarations, control flow
     * constructs, and various operations such as loops, returns, and breaks.
     *
     * @return the parsed {@code Statement} corresponding to the current token context.
     *         The returned {@code Statement} may represent any of the statement
     *         types in the Cloth language.
     */
    @SneakyThrows
    private Statement parseStatement() {
        var token = peek();

        return switch (((Token) token).keyword()) {
            case Tokens.Keyword.Var, Tokens.Keyword.Let, Tokens.Keyword.Const -> parseVarDeclaration();
            case Tokens.Keyword.Return -> parseReturn();
            case Tokens.Keyword.If -> parseIf();
            case Tokens.Keyword.While -> parseWhile();
            case Tokens.Keyword.For -> parseFor();
            case Tokens.Keyword.Break -> parseBreak();
            case Tokens.Keyword.Continue -> parseContinue();
            case Tokens.Keyword.Delete -> parseDelete();
            case Tokens.Keyword.Defer -> parseDefer();
            default -> parseOpStatement((Token) token);
        };
    }

    /**
     * Parses an operational statement based on the operator of the given token.
     * Dispatches to the appropriate parsing method depending on the type of operator
     * present in the token. Supported operations include prefix increment/decrement,
     * nested blocks, and expressions or assignments.
     *
     * @param token The {@code Token} whose operator determines the type of operation
     *              to parse. Must provide a valid operator for correct statement parsing.
     * @return The parsed {@code Statement} representing the operational statement associated
     *         with the token's operator. The returned statement can be a prefix increment/decrement,
     *         a nested block, or an expression/assignment.
     */
    private Statement parseOpStatement(Token token) {
        return switch (token.operator()) {
            case Tokens.Operator.PlusPlus, Tokens.Operator.MinusMinus -> parsePrefixIncrementDecrement();
            case Tokens.Operator.LeftBrace -> parseNestedBlock();
            default -> parseExpressionOrAssignment();
        };
    }

    /**
     * Parses a variable declaration statement, including its binding kind
     * (var, let, or const), identifier, type, optional initializer, and
     * ensures it ends with a semicolon. This method also performs necessary
     * validations and generates appropriate compile errors for invalid syntax.
     *
     * @return a {@code Statement.VarDeclaration} representing the parsed variable
     *         declaration, including binding type, variable name, type reference,
     *         optional initializer expression, and the source span of the declaration.
     */
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

        IToken name = expect(TokenKind.Identifier, CommonErrors.EXPECTED_IDENTIFIER, "Expected variable name.");
        expect(Tokens.Operator.Colon, CommonErrors.EXPECTED_COLON, "Expected ':' between variable name and type.");

        TypeReferenceParser.TypeReference type = new TypeReferenceParser(getLexer(), getFile()).parse();
        Expression initializer = null;
        if (is(Tokens.Operator.Assign)) {
            advance();
            initializer = new ExpressionParser(getLexer(), getFile()).parse();
        }

        IToken semi = expectSemiColon();

        return new Statement.VarDeclaration(binding, bindingToken, name, type, initializer, new SourceSpan(bindingToken.span().start(), semi.span().end()));
    }

    /**
     * Parses a return statement from the current position in the token stream.
     * It consumes the 'return' keyword, optionally processes a return value expression,
     * and ensures the statement ends with a semicolon.
     *
     * @return A {@code Statement.Return} object representing the parsed return statement,
     * including its keyword, optional value, and source span.
     */
    @SneakyThrows
    private Statement.Return parseReturn() {
        IToken keyword = advance(); // consume 'return'

        @Nullable Expression value = null;
        if (!is(Tokens.Operator.Semicolon)) value = new ExpressionParser(getLexer(), getFile()).parse();

        IToken semi = expectSemiColon();
        return new Statement.Return(keyword, value, new SourceSpan(keyword.span().start(), semi.span().end()));
    }

    /**
     * Parses an `if` statement from the input tokens. The method constructs and returns
     * an {@code If} statement representation including its condition, the block of statements
     * to execute if the condition is true, and optionally, the block (or another `if` statement)
     * to execute if the condition is false (an `else` block or chain of `else-if` conditions).
     * <p>
     * <strong>The method handles:</strong>
     * <ul>
     *     <li>The presence of parentheses around the condition (optional).</li>
     *     <li>Parsing and validating the condition expression.</li>
     *     <li>Validating and parsing the associated block(s) for the `if` body and optional `else` body.</li>
     *     <li>Proper error handling for missing or mismatched braces and parentheses.</li>
     * </ul>
     *
     * @return An {@code If} statement that includes the parsed condition expression,
     *         the block to execute if the condition is true, the optional block to execute
     *         if the condition is false, and the source span representing the entire statement.
     */
    @SneakyThrows
    private Statement.If parseIf() {
        IToken keyword = advance(); // consume 'if'
        boolean hasParentheses = is(Tokens.Operator.LeftParen);

        if (hasParentheses) advance();
        Expression condition = new ExpressionParser(getLexer(), getFile()).parse();
        if (hasParentheses) expect(Tokens.Operator.RightParen, CommonErrors.EXPECTED_CLOSE_PAREN);

        expect(Tokens.Operator.LeftBrace, CommonErrors.EXPECTED_OPEN_BRACE);
        Statement.Block thenBlock = parseBlock();
        IToken thenClose = expect(Tokens.Operator.RightBrace, CommonErrors.EXPECTED_CLOSE_BRACE);

        @Nullable Statement elseBlock = null;
        if (is(Tokens.Keyword.Else)) {
            advance(); // consume 'else'
            if (is(Tokens.Keyword.If)) {
                elseBlock = parseIf();
            } else {
                expect(Tokens.Operator.LeftBrace, CommonErrors.EXPECTED_OPEN_BRACE, "Expected '{' or 'if' for else-if chain.");
                Statement.Block elseBody = parseBlock();
                IToken elseClose = expect(Tokens.Operator.RightBrace, CommonErrors.EXPECTED_CLOSE_BRACE);
                elseBlock = new Statement.Block(elseBody.statements(), new SourceSpan(elseBody.span().start(), elseClose.span().end()));
            }
        }

        SourceSpan span = new SourceSpan(keyword.span().start(), elseBlock != null ? elseBlock.span().end() : thenClose.span().end());
        return new Statement.If(condition, thenBlock, elseBlock, span);
    }

    /**
     * Parses a 'while' statement from the given token stream.
     * This method consumes tokens starting from the 'while' keyword,
     * processes the optional parentheses for the condition, validates the condition expression,
     * and ensures proper bracing for the loop body. The parsed 'while' statement
     * includes its condition, body, and source span.
     *
     * @return The parsed {@link Statement.While} object containing the condition expression,
     *         body block, and the source span encompassing the entire 'while' statement.
     * @throws CompileError if the structure of the 'while' statement is invalid, such as:
     * <ul>
     *     <li>Missing parentheses for the condition when required.</li>
     *     <li>Invalid or incomplete condition expression.</li>
     *     <li>Missing or mismatched braces for the loop body.</li>
     * </ul>
     */
    @SneakyThrows
    private Statement.While parseWhile() {
        IToken keyword = advance(); // consume 'while'
        boolean hasParentheses = is(Tokens.Operator.LeftParen);

        if (hasParentheses) advance();
        Expression condition = new ExpressionParser(getLexer(), getFile()).parse();
        if (hasParentheses) expect(Tokens.Operator.RightParen, CommonErrors.EXPECTED_CLOSE_PAREN);
        expect(Tokens.Operator.LeftBrace, CommonErrors.EXPECTED_OPEN_BRACE);

        Statement.Block body = parseBlock();
        IToken closeBrace = expect(Tokens.Operator.RightBrace, CommonErrors.EXPECTED_CLOSE_BRACE);
        return new Statement.While(condition, body, new SourceSpan(keyword.span().start(), closeBrace.span().end()));
    }

    /**
     * Parses a 'for' loop statement.
     * This method first consumes the 'for' keyword, then determines
     * if the loop is a "foreach" style loop or a classic C-style 'for' loop
     * based on the syntax. It delegates to the appropriate parsing method
     * accordingly.
     *
     * @return The parsed 'for' loop statement as a {@code Statement} object.
     * @throws CompileError if the syntax of the 'for' loop is invalid,
     *                      such as a missing opening parenthesis.
     */
    @SneakyThrows
    private Statement parseFor() {
        IToken keyword = advance(); // consume 'for'

        expect(Tokens.Operator.LeftParen, CommonErrors.EXPECTED_OPEN_PAREN);
        if (isForEachHeader()) return parseForEach(keyword);

        return parseCStyleFor(keyword);
    }

    /**
     * Determines if the current token sequence corresponds to a "for-each" loop header.
     *
     * @return true if the current token is an identifier, followed by a keyword "in" token;
     *         false otherwise.
     */
    private boolean isForEachHeader() {
        return is(TokenKind.Identifier) && peek(1).is(TokenKind.Keyword) && ((Token) peek(1)).keyword() == Tokens.Keyword.In;
    }

    /**
     * Parses a "foreach" statement from the provided tokens, constructing an AST representation
     * of the statement.
     *
     * @param keyword The token representing the "foreach" keyword, indicating the start of the
     *                "foreach" statement.
     * @return A {@link Statement.ForEach} object representing the parsed "foreach" statement,
     *         including the identifier, iterable expression, body, and source span.
     * @throws CompileError If parsing fails due to a malformed or incomplete "foreach" statement.
     */
    @SneakyThrows
    private Statement.ForEach parseForEach(IToken keyword) {
        IToken name = advance(); // consume identifier
        advance(); // consume 'in'

        Expression iterable = new ExpressionParser(getLexer(), getFile()).parse();
        expect(Tokens.Operator.RightParen, CommonErrors.EXPECTED_CLOSE_PAREN);
        expect(Tokens.Operator.LeftBrace, CommonErrors.EXPECTED_OPEN_BRACE);

        Statement.Block body = parseBlock();
        IToken closeBrace = expect(Tokens.Operator.RightBrace, CommonErrors.EXPECTED_CLOSE_BRACE);

        return new Statement.ForEach(name, iterable, body, new SourceSpan(keyword.span().start(), closeBrace.span().end()));
    }

    /**
     * Parses and returns a C-style for-loop statement.
     * <p>
     * The method processes the structure of a C-style for loop, including initialization,
     * condition, step, and the loop body. It validates the syntax using tokens such as
     * semicolons, parentheses, and braces, and throws appropriate compile errors if
     * any syntax issue is encountered.
     *
     * @param keyword the `for` keyword token that indicates the start of the for-loop.
     * @return a {@code Statement.For} object representing the parsed C-style for-loop.
     * It includes the initializer, condition, step, body, and source span.
     * @throws CompileError if the for-loop structure contains syntax errors,
     * such as missing semicolons, parentheses, or braces.
     */
    @SneakyThrows
    private Statement.For parseCStyleFor(IToken keyword) {
        @Nullable Statement init = null;
        if (!is(Tokens.Operator.Semicolon)) init = parseForInit();

        expect(Tokens.Operator.Semicolon, CommonErrors.EXPECTED_SEMICOLON, "Expected ';' after for-loop initializer.");

        @Nullable Expression condition = null;
        if (!is(Tokens.Operator.Semicolon)) condition = new ExpressionParser(getLexer(), getFile()).parse();
        expect(Tokens.Operator.Semicolon, CommonErrors.EXPECTED_SEMICOLON, "Expected ';' after for-loop condition.");

        @Nullable Statement step = null;
        if (!is(Tokens.Operator.RightParen)) step = parseForStep();

        expect(Tokens.Operator.RightParen, CommonErrors.EXPECTED_CLOSE_PAREN);
        expect(Tokens.Operator.LeftBrace, CommonErrors.EXPECTED_OPEN_BRACE);
        Statement.Block body = parseBlock();
        IToken closeBrace = expect(Tokens.Operator.RightBrace, CommonErrors.EXPECTED_CLOSE_BRACE);

        return new Statement.For(init, condition, step, body, new SourceSpan(keyword.span().start(), closeBrace.span().end()));
    }

    /**
     * Parses the initialization part of a 'for' statement. This method determines whether
     * the initialization involves variable declarations (e.g., using keywords such as var,
     * let, or const) or an assignment/increment operation, and processes it accordingly.
     *
     * @return a Statement representing the initialization of the 'for' loop. This can
     *         either be a variable declaration or an assignment/increment statement.
     */
    @SneakyThrows
    private Statement parseForInit() {
        if (is(Tokens.Keyword.Var) || is(Tokens.Keyword.Let) || is(Tokens.Keyword.Const)) return parseForVarDeclaration();

        return parseForAssignmentOrIncrement();
    }

    /**
     * Parses a variable declaration statement from the source code.
     * This method identifies the kind of variable binding (var, let, or const),
     * retrieves the variable's name, type, and optional initializer,
     * and constructs a {@link Statement.VarDeclaration} object representing the parsed declaration.
     * <p>
     * The syntax it expects is:
     * <strong><pre>[binding-kind] [name]: [type] [= [initializer]]</pre></strong>
     * <p>
     * For example:
     * <pre>
     * var x: i32 = 10;
     * let y: string;
     * const PI: f64 = 3.14;
     * </pre>
     *
     * @return A {@link Statement.VarDeclaration} object representing the parsed variable declaration,
     *         containing details such as the binding kind, variable name, type, optional initializer,
     *         and source span of the declaration.
     * @throws CompileError if the syntax is invalid, such as a missing variable name, type, or required symbol.
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

        IToken name = expect(TokenKind.Identifier, CommonErrors.EXPECTED_IDENTIFIER, "Expected variable name.");
        expect(Tokens.Operator.Colon, CommonErrors.EXPECTED_COLON, "Expected ':' between variable name and type.");
        TypeReferenceParser.TypeReference type = new TypeReferenceParser(getLexer(), getFile()).parse();
        Expression initializer = null;
        if (is(Tokens.Operator.Assign)) {
            advance();
            initializer = new ExpressionParser(getLexer(), getFile()).parse();
        }

        SourceSpan span = new SourceSpan(bindingToken.span().start(), initializer != null ? initializer.span().end() : type.span().end());

        return new Statement.VarDeclaration(binding, bindingToken, name, type, initializer, span);
    }

    /**
     * Parses a for-loop header component to produce either an assignment statement
     * or an increment/decrement statement.
     * <p>
     * The method determines if the current token sequence represents an increment or
     * decrement operation (prefix or postfix) and creates the corresponding statement.
     * Otherwise, it checks for an assignment operator and constructs an assignment statement.
     * <p>
     * If none of these cases apply, an exception is thrown indicating an invalid syntax
     * for the for-loop header.
     *
     * @return A {@code Statement} instance representing either an increment/decrement
     *         operation or an assignment operation within a for-loop.
     * @throws CompileError If the token sequence does not conform to the expected
     *         structure of a variable declaration, assignment, or increment/decrement.
     */
    @SneakyThrows
    private Statement parseForAssignmentOrIncrement() {
        if (is(Tokens.Operator.PlusPlus) || is(Tokens.Operator.MinusMinus)) {
            IToken op = advance();
            Expression target = new ExpressionParser(getLexer(), getFile()).parse();
            return new Statement.IncrementDecrement(target, op, true, new SourceSpan(op.span().start(), target.span().end()));
        }

        Expression target = new ExpressionParser(getLexer(), getFile()).parse();

        if (is(Tokens.Operator.PlusPlus) || is(Tokens.Operator.MinusMinus)) {
            IToken op = advance();
            return new Statement.IncrementDecrement(target, op, false, new SourceSpan(target.span().start(), op.span().end()));
        }

        if (!isAssignmentOperator()) throw new SyntaxError("Expected assignment operator", peek().span(), "For-loop header requires a variable declaration, assignment, or increment/decrement.", "i = i + 1");
        IToken op = advance();
        Expression value = new ExpressionParser(getLexer(), getFile()).parse();

        return new Statement.Assignment(target, op, value, new SourceSpan(target.span().start(), value.span().end()));
    }

    /**
     * Parses the step component of a for-loop to retrieve the corresponding statement.
     * This method is internally used to process either an assignment or an increment statement
     * that constitutes the step expression in a for-loop structure.
     *
     * @return a {@link Statement} object representing the parsed assignment or increment statement.
     */
    @SneakyThrows
    private Statement parseForStep() {
        return parseForAssignmentOrIncrement();
    }

    /**
     * Parses a 'break' statement in the source code.
     * <p>
     * This method consumes the 'break' keyword token and expects a semicolon token
     * to follow. It creates and returns a new instance of {@link Statement.Break},
     * which represents the 'break' statement in the parsed syntax tree.
     *
     * @return A {@link Statement.Break} object representing the parsed 'break' statement,
     *         including its associated source span.
     */
    @SneakyThrows
    private Statement.Break parseBreak() {
        IToken keyword = advance(); // consume 'break'
        IToken semi = expectSemiColon();

        return new Statement.Break(keyword, new SourceSpan(keyword.span().start(), semi.span().end()));
    }

    /**
     * Parses a 'continue' statement from the input token stream.
     * Consumes the 'continue' keyword and expects a following semicolon, constructing a {@link Statement.Continue} object representing the parsed statement.
     *
     * @return a {@link Statement.Continue} instance that encapsulates the 'continue' statement along with its source span.
     */
    @SneakyThrows
    private Statement.Continue parseContinue() {
        IToken keyword = advance(); // consume 'continue'
        IToken semi = expectSemiColon();

        return new Statement.Continue(keyword, new SourceSpan(keyword.span().start(), semi.span().end()));
    }

    /**
     * Parses a delete statement from the input source code. This method consumes the 'delete' keyword,
     * parses the target expression, and ensures the presence of a terminating semicolon.
     *
     * @return A {@link Statement.Delete} object representing the parsed delete statement,
     *         including its target and source span.
     */
    @SneakyThrows
    private Statement.Delete parseDelete() {
        IToken keyword = advance(); // consume 'delete'
        Expression target = new ExpressionParser(getLexer(), getFile()).parse();
        IToken semi = expectSemiColon();

        return new Statement.Delete(target, new SourceSpan(keyword.span().start(), semi.span().end()));
    }

    /**
     * Parses a defer statement.
     * <p>
     * This method processes a `defer` keyword, followed by a call expression,
     * and ensures the statement is properly terminated with a semicolon.
     * The parsed defer statement is represented as an instance of {@code Statement.Defer},
     * which includes the keyword token, the deferred call expression, and the
     * source span encompassing the entire statement.
     *
     * @return the parsed {@code Statement.Defer} representing the defer statement.
     *         This statement contains the deferred call expression, the source information,
     *         and ensures that only valid call expressions can be deferred.
     */
    @SneakyThrows
    private Statement.Defer parseDefer() {
        IToken keyword = advance(); // consume 'defer'
        Expression expr = new ExpressionParser(getLexer(), getFile()).parse();
        if (!(expr instanceof Expression.Call call)) throw CommonErrors.DEFER_REQUIRES_CALL.toError(expr.span());
        IToken semi = expectSemiColon();

        return new Statement.Defer(keyword, call, new SourceSpan(keyword.span().start(), semi.span().end()));
    }

    /**
     * Parses a nested block of statements enclosed by braces.
     * This method consumes the opening brace token (`{`), delegates
     * the parsing of the inner block to the {@code parseBlock} method,
     * and ensures that a matching closing brace (`}`) is present.
     * The resulting block includes all parsed statements, as well as
     * the source span encompassing the entire block, from the opening
     * brace to the closing brace.
     *
     * @return the parsed {@code Statement.Block} representing the nested block.
     *         This block contains the list of parsed statements and
     *         the source span information for the block's braces.
     */
    @SneakyThrows
    private Statement.Block parseNestedBlock() {
        IToken open = advance(); // consume '{'
        Statement.Block block = parseBlock();
        IToken close = expect(Tokens.Operator.RightBrace, CommonErrors.EXPECTED_CLOSE_BRACE);

        return new Statement.Block(block.statements(), new SourceSpan(open.span().start(), close.span().end()));
    }

    /**
     * Parses a prefix increment or decrement operation statement.
     * <p>
     * This method consumes the increment or decrement operator (e.g., {@code ++}, {@code --}),
     * parses the target expression to which the operator applies, and then ensures that the
     * statement is terminated with a semicolon. The parsed statement is represented as an
     * instance of {@code Statement.IncrementDecrement}.
     *
     * @return the parsed {@code Statement.IncrementDecrement} representing a prefix increment
     *         or decrement operation. This statement includes the target expression, the operator,
     *         and source span information encompassing the entire statement.
     */
    @SneakyThrows
    private Statement.IncrementDecrement parsePrefixIncrementDecrement() {
        IToken op = advance(); // consume ++ or --
        Expression target = new ExpressionParser(getLexer(), getFile()).parse();
        IToken semi = expectSemiColon();
        return new Statement.IncrementDecrement(target, op, true, new SourceSpan(op.span().start(), semi.span().end()));
    }

    /**
     * Checks if the current token in the token stream corresponds to an assignment operator.
     * This includes standard assignment ({@code =}) and compound assignment operators
     * such as {@code +=}, {@code -=}, {@code *=}, {@code /=}, and {@code %=}.
     *
     * @return {@code true} if the current token represents an assignment operator;
     *         {@code false} otherwise.
     */
    private boolean isAssignmentOperator() {
        return is(Tokens.Operator.Assign) || is(Tokens.Operator.PlusAssign) || is(Tokens.Operator.MinusAssign) || is(Tokens.Operator.StarAssign) || is(Tokens.Operator.SlashAssign) || is(Tokens.Operator.PercentAssign);
    }

    /**
     * Parses either an expression statement or an assignment statement.
     * Depending on the operator and subsequent tokens, this method determines
     * if the current statement is a standalone expression, an assignment (plain
     * or compound), or an increment/decrement operation.
     * <p>
     * The method begins by parsing an {@code Expression}. If the next token
     * corresponds to an assignment operator (e.g., {@code =}, {@code +=}), it
     * parses the assignment statement. If the next token is a postfix increment
     * or decrement operator (e.g., {@code ++}, {@code --}), it parses the
     * increment/decrement statement. Otherwise, it considers the parsed
     * expression as a standalone expression statement.
     * <p>
     * Each parsed statement is concluded with a semicolon, which is expected in
     * the token stream. If the semicolon is missing or the token stream context
     * is invalid, an exception will be thrown.
     *
     * @return a {@code Statement} that can be one of the following:
     * <ul>
     *     <li>{@link Statement.ExpressionStmt} if the parsed input is a standalone expression.</li>
     *     <li>{@link Statement.Assignment} if the parsed input is an assignment operation.</li>
     *     <li>{@link Statement.IncrementDecrement} if the parsed input is an increment or decrement operation.</li>
     * </ul>
     */
    @SneakyThrows
    private Statement parseExpressionOrAssignment() {
        Expression expr = new ExpressionParser(getLexer(), getFile()).parse();

        if (isAssignmentOperator()) {
            IToken op = advance();
            Expression value = new ExpressionParser(getLexer(), getFile()).parse();
            IToken semi = expectSemiColon();
            return new Statement.Assignment(expr, op, value, new SourceSpan(expr.span().start(), semi.span().end()));
        }

        if (is(Tokens.Operator.PlusPlus) || is(Tokens.Operator.MinusMinus)) {
            IToken op = advance();
            IToken semi = expectSemiColon();
            return new Statement.IncrementDecrement(expr, op, false, new SourceSpan(expr.span().start(), semi.span().end()));
        }

        IToken semi = expectSemiColon();
        return new Statement.ExpressionStmt(expr, new SourceSpan(expr.span().start(), semi.span().end()));
    }

}
