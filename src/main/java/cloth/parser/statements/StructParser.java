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
 * Parses a struct declaration of the form:
 * <pre>[modifiers] struct Name(params?) { members }</pre>
 * Structs are small, data-centric value types with optional primary parameters
 * and a body containing fields (and eventually methods).
 * Structs do NOT support inheritance, interfaces, or the {@code abstract}/{@code override} modifiers.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public class StructParser extends ParserPart<StructParser.StructDeclaration> {

    public StructParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    @Override
    @SneakyThrows
    public StructDeclaration parse() {
        DeclarationFlags flags = parseDeclarationFlags();

        if (flags.isAbstract()) {
            throw new CompileError(
                "Structs cannot be abstract",
                flags.getAbstractToken().span(),
                "Remove the 'abstract' modifier.",
                "Structs are value types and do not support abstract members or inheritance."
            );
        }
        if (flags.isOverride()) {
            throw new CompileError(
                "'override' is not valid on a struct",
                flags.getOverrideToken().span(),
                "Remove the 'override' modifier.",
                "Structs are value types and do not participate in inheritance."
            );
        }

        IToken structKeyword = expect(Tokens.Keyword.Struct, () ->
            new CompileError("Expected 'struct'", peek().span(),
                "Expected a struct declaration.",
                "Struct declarations begin with the 'struct' keyword."));

        IToken name = expect(TokenKind.Identifier, () ->
            new CompileError("Expected struct name", peek().span(),
                "A struct name must be a valid identifier.",
                "struct Vec2(x: f32, y: f32) { }"));

        List<ParameterListParser.Parameter> primaryParams = null;
        if (is(Tokens.Operator.LeftParen)) {
            primaryParams = new ParameterListParser(getLexer(), getFile()).parse();
        }

        expect(Tokens.Operator.LeftBrace, () ->
            new CompileError("Expected '{'", peek().span(),
                "Expected opening brace for struct body.",
                "struct Vec2(x: f32, y: f32) { }"));

        var fields = new ArrayList<FieldParser.FieldDeclaration>();
        var methods = new ArrayList<FuncParser.FuncDeclaration>();
        parseStructBody(fields, methods);

        IToken closeBrace = expect(Tokens.Operator.RightBrace, () ->
            new CompileError("Expected '}'", peek().span(),
                "Expected closing brace for struct body.",
                "struct Vec2(x: f32, y: f32) { }"));

        IToken firstFlag = flags.firstToken();
        SourceSpan span = new SourceSpan(
            firstFlag != null ? firstFlag.span().start() : structKeyword.span().start(),
            closeBrace.span().end()
        );

        return new StructDeclaration(flags, name, primaryParams, fields, methods, span);
    }

    // region Struct Body

    private void parseStructBody(List<FieldParser.FieldDeclaration> fields,
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

    // endregion

    // region Records

    public record StructDeclaration(
        DeclarationFlags flags,
        IToken name,
        @Nullable List<ParameterListParser.Parameter> primaryConstructor,
        List<FieldParser.FieldDeclaration> fields,
        List<FuncParser.FuncDeclaration> methods,
        SourceSpan span
    ) {}

    // endregion
}
