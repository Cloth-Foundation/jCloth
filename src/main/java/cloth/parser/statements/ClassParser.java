package cloth.parser.statements;

import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.parser.ParserPart;
import cloth.parser.flags.DeclarationFlags;
import cloth.token.IToken;
import cloth.token.TokenKind;
import cloth.token.Tokens;
import cloth.token.span.SourceSpan;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses class declarations from a source file.
 * This parser is responsible for interpreting the structure of a class, including
 * its modifiers, name, parameters, base class, implemented interfaces, fields, and methods.
 * The parsing process ensures that the syntax adheres to the expected language grammar rules.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public class ClassParser extends ParserPart<ClassParser.ClassDeclaration> {

    public ClassParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    /**
     * Parses a class declaration from the provided token stream.
     * This method handles parsing of declaration modifiers,
     * class name, potential primary constructor parameters,
     * base class specification, implemented interfaces, and
     * the class body, including fields and methods.
     *
     * @return A {@link ClassDeclaration} instance representing the parsed class,
     *         including its metadata (modifiers, name, base class, etc.),
     *         members (fields and methods), and source span.
     */
    @Override
    @SneakyThrows
    public ClassDeclaration parse() {
        DeclarationFlags flags = parseDeclarationFlags();

        IToken classKeyword = expect(Tokens.Keyword.Class, () ->
            new CompileError("Expected 'class'", peek().span(),
                "Expected a class declaration.",
                "Class declarations begin with the 'class' keyword."));

        IToken name = expect(TokenKind.Identifier, () ->
            new CompileError("Expected class name", peek().span(),
                "A class name must be a valid identifier.",
                "class MyClass { }"));

        List<ParameterListParser.Parameter> primaryParams = null;
        if (is(Tokens.Operator.LeftParen)) {
            primaryParams = new ParameterListParser(getLexer(), getFile()).parse();
        }

        QualifiedNameParser.QualifiedName baseClass = null;
        if (is(Tokens.Operator.Colon)) {
            baseClass = parseBaseClass();
        }

        List<QualifiedNameParser.QualifiedName> interfaces = List.of();
        if (is(Tokens.Keyword.Is)) {
            interfaces = parseInterfaces();
        }

        expect(Tokens.Operator.LeftBrace, () ->
            new CompileError("Expected '{'", peek().span(),
                "Expected opening brace for class body.",
                "class MyClass { }"));

        var fields = new ArrayList<FieldParser.FieldDeclaration>();
        var methods = new ArrayList<FuncParser.FuncDeclaration>();
        parseClassBody(fields, methods);

        IToken closeBrace = expect(Tokens.Operator.RightBrace, () ->
            new CompileError("Expected '}'", peek().span(),
                "Expected closing brace for class body.",
                "class MyClass { }"));

        IToken firstFlag = flags.firstToken();
        SourceSpan span = new SourceSpan(
            firstFlag != null ? firstFlag.span().start() : classKeyword.span().start(),
            closeBrace.span().end()
        );

        return new ClassDeclaration(flags, name, primaryParams, baseClass, interfaces, fields, methods, span);
    }

    /**
     * Parses and returns the base class specified for a class declaration.
     * This method consumes the current token (which is expected to be a colon `:`)
     * and proceeds to parse the base class description using a {@link QualifiedNameParser}.
     * It also handles any balanced parentheses that may follow the base class name.
     *
     * @return a {@link QualifiedNameParser.QualifiedName} object representing the fully parsed
     *         and validated base class name.
     */
    private QualifiedNameParser.QualifiedName parseBaseClass() {
        advance(); // consume :
        QualifiedNameParser.QualifiedName baseClass = new QualifiedNameParser(getLexer(), getFile()).parse();
        skipBalancedParens();
        return baseClass;
    }

    /**
     * Parses a list of implemented interfaces from the token stream.
     * This method processes a sequence of qualified names, separating them
     * by commas, and collects the resulting names into a list. Each qualified
     * name is parsed using the {@link QualifiedNameParser}.
     * <p>
     * This method advances past tokens representing the `is` keyword and
     * continues to parse until no more comma-separated qualified names are found.
     *
     * @return a list of {@link QualifiedNameParser.QualifiedName} objects representing
     *         the parsed interfaces.
     */
    private List<QualifiedNameParser.QualifiedName> parseInterfaces() {
        advance(); // consume `is`
        var interfaces = new ArrayList<QualifiedNameParser.QualifiedName>();
        do {
            interfaces.add(new QualifiedNameParser(getLexer(), getFile()).parse());
        } while (match(Tokens.Operator.Comma));
        return interfaces;
    }

    /**
     * Skips a balanced sequence of parentheses, starting with an opening parenthesis.
     * <p>
     * This method consumes the initial opening parenthesis token and continues to
     * advance through subsequent tokens, ensuring that any nested pairs of parentheses
     * are properly balanced. It stops when the matching closing parenthesis is found
     * or when the end of the file is reached.
     * <p>
     * The method uses a depth counter to track the nesting level of parentheses:
     * <ul>
     *   <li>Each opening parenthesis (`Tokens.Operator.LeftParen`) increases the count.</li>
     *   <li>Each closing parenthesis (`Tokens.Operator.RightParen`) decreases the count.</li>
     * </ul>
     * <p>
     * The loop terminates when the depth returns to zero, indicating that the entire
     * balanced pair has been skipped.
     *
     * This method is typically used during parsing to bypass sections of code enclosed
     * in parentheses that are not immediately relevant to the current parsing context.
     */
    private void skipBalancedParens() {
        advance(); // consume (
        int depth = 1;
        while (depth > 0 && !isEndOfFile()) {
            if (is(Tokens.Operator.LeftParen)) depth++;
            else if (is(Tokens.Operator.RightParen)) depth--;
            advance();
        }
    }

    /**
     * Parses the body of a class and collects its fields and methods.
     * This method continues parsing until it encounters a closing right brace
     * or reaches the end of the file. Fields and methods are identified
     * based on their respective declaration keywords and are added to the
     * provided lists.
     *
     * @param fields A list to collect instances of {@link FieldParser.FieldDeclaration} representing
     *               parsed field declarations within the class body.
     * @param methods A list to collect instances of {@link FuncParser.FuncDeclaration} representing
     *                parsed method declarations within the class body.
     */
    private void parseClassBody(List<FieldParser.FieldDeclaration> fields,
                                List<FuncParser.FuncDeclaration> methods) {
        while (!is(Tokens.Operator.RightBrace) && !isEndOfFile()) {
            Tokens.Keyword memberKeyword = peekDeclarationKeyword();

            if (memberKeyword == Tokens.Keyword.Var
                || memberKeyword == Tokens.Keyword.Let
                || memberKeyword == Tokens.Keyword.Const) {
                fields.add(new FieldParser(getLexer(), getFile()).parse());
            } else if (memberKeyword == Tokens.Keyword.Func) {
                methods.add(new FuncParser(getLexer(), getFile()).parse());
            } else {
                skipUnknownMember();
            }
        }
    }

    /**
     * Skips a single unrecognized member in the class body.
     * If the current token opens a brace block, skips the entire block;
     * otherwise advances past one token.
     */
    private void skipUnknownMember() {
        if (is(Tokens.Operator.LeftBrace)) {
            advance();
            int depth = 1;
            while (depth > 0 && !isEndOfFile()) {
                if (is(Tokens.Operator.LeftBrace)) depth++;
                else if (is(Tokens.Operator.RightBrace)) depth--;
                if (depth > 0) advance();
            }
            if (!isEndOfFile()) advance();
        } else {
            advance();
        }
    }

    /**
     * Represents the declaration of a class within the source code. This record encapsulates
     * various components of a class, including its modifiers, name, constructor parameters,
     * base class, implemented interfaces, fields, methods, and location within the source.
     */
    public record ClassDeclaration(
        DeclarationFlags flags,
        IToken name,
        @Nullable List<ParameterListParser.Parameter> primaryConstructor,
        @Nullable QualifiedNameParser.QualifiedName baseClass,
        List<QualifiedNameParser.QualifiedName> interfaces,
        List<FieldParser.FieldDeclaration> fields,
        List<FuncParser.FuncDeclaration> methods,
        SourceSpan span
    ) {}

}
