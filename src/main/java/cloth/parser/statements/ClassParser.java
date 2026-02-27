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

public class ClassParser extends ParserPart<ClassParser.ClassDeclaration> {

    public ClassParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

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

        // TODO: parse class members once member parsers exist
        skipBlockContents();

        IToken closeBrace = expect(Tokens.Operator.RightBrace, () ->
            new CompileError("Expected '}'", peek().span(),
                "Expected closing brace for class body.",
                "class MyClass { }"));

        IToken firstFlag = flags.firstToken();
        SourceSpan span = new SourceSpan(
            firstFlag != null ? firstFlag.span().start() : classKeyword.span().start(),
            closeBrace.span().end()
        );

        return new ClassDeclaration(flags, name, primaryParams, baseClass, interfaces, span);
    }

    // region Base Class & Interfaces

    private QualifiedNameParser.QualifiedName parseBaseClass() {
        advance(); // consume :
        QualifiedNameParser.QualifiedName baseClass = new QualifiedNameParser(getLexer(), getFile()).parse();
        skipBalancedParens();
        return baseClass;
    }

    private List<QualifiedNameParser.QualifiedName> parseInterfaces() {
        advance(); // consume `is`
        var interfaces = new ArrayList<QualifiedNameParser.QualifiedName>();
        do {
            interfaces.add(new QualifiedNameParser(getLexer(), getFile()).parse());
        } while (match(Tokens.Operator.Comma));
        return interfaces;
    }

    private void skipBalancedParens() {
        advance(); // consume (
        int depth = 1;
        while (depth > 0 && !isEndOfFile()) {
            if (is(Tokens.Operator.LeftParen)) depth++;
            else if (is(Tokens.Operator.RightParen)) depth--;
            advance();
        }
    }

    // endregion

    /**
     * Consumes tokens inside a brace-delimited block, handling nested braces.
     * Stops when the matching closing brace is reached (leaving it for expect to consume).
     */
    private void skipBlockContents() {
        int depth = 0;
        while (!isEndOfFile()) {
            if (is(Tokens.Operator.LeftBrace)) {
                depth++;
                advance();
            } else if (is(Tokens.Operator.RightBrace)) {
                if (depth == 0) break;
                depth--;
                advance();
            } else {
                advance();
            }
        }
    }

    // region Records

    public record ClassDeclaration(
        DeclarationFlags flags,
        IToken name,
        @Nullable List<ParameterListParser.Parameter> primaryConstructor,
        @Nullable QualifiedNameParser.QualifiedName baseClass,
        List<QualifiedNameParser.QualifiedName> interfaces,
        SourceSpan span
    ) {}

    // endregion
}
