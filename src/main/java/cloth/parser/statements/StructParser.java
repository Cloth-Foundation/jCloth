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

    /**
     * Parses a struct declaration from the provided token stream, validating its components
     * and collecting its fields, methods, and optional primary constructor. This method ensures
     * that the struct adheres to the language rules and generates appropriate compile errors
     * for invalid constructs.
     *
     * @return A {@code StructDeclaration} that represents the definition of the parsed struct,
     *         including its modifiers, name, primary constructor (if present), fields, methods,
     *         and the source code location (span) of the declaration.
     */
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

    /**
     * Parses the body of a struct, identifying and collecting field and method declarations.
     * It processes tokens within the struct body until a closing right brace or the end of file is encountered.
     * Depending on the identified keyword, fields or methods are parsed and added to the respective lists.
     * If an unknown member is encountered, it is skipped gracefully.
     *
     * @param fields  a list to collect parsed field declarations. Each field is parsed using {@code FieldParser}.
     * @param methods a list to collect parsed method declarations. Each method is parsed using {@code FuncParser}.
     */
    private void parseStructBody(List<FieldParser.FieldDeclaration> fields, List<FuncParser.FuncDeclaration> methods) {
        while (!is(Tokens.Operator.RightBrace) && !isEndOfFile()) {
            Tokens.Keyword memberKeyword = peekDeclarationKeyword();

            if (memberKeyword == Tokens.Keyword.Var || memberKeyword == Tokens.Keyword.Let || memberKeyword == Tokens.Keyword.Const) {
                fields.add(new FieldParser(getLexer(), getFile()).parse());
            } else if (memberKeyword == Tokens.Keyword.Func) {
                methods.add(new FuncParser(getLexer(), getFile()).parse());
            } else {
                skipUnknownMember();
            }
        }
    }

    /**
     * Skips over unknown or unrecognized members in the input token stream.
     * This method is designed to handle cases where an unknown member is encountered during parsing,
     * advancing the token stream to bypass the unrecognized structure.
     * <p>
     * If the unknown member is enclosed within a pair of braces ({@code { }}) indicating a nested structure,
     * this method will ensure proper handling by keeping track of brace depth and advancing through
     * the token stream until the entire nested structure has been skipped.
     * <p>
     * If the unknown member is not a brace-enclosed structure, the method simply advances the token stream
     * by one token to move past it.
     * <p>
     * This method plays a key role in resilient parsing by allowing the parser to handle unrecognized elements
     * gracefully without terminating or throwing errors.
     * <p>
     * TODO: This will be removed as we will want to throw errors for unknown members.
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
     * Represents the declaration of a struct, which is a composite data type consisting
     * of fields, methods, and optionally a primary constructor. Structs are a key building
     * block for defining custom types in the language.
     *
     * @param flags                 The declaration flags that provide modifiers and visibility
     *                              information about the struct (e.g., visibility, static, or final).
     * @param name                  The name of the struct, represented as a token.
     * @param primaryConstructor    An optional list of parameters representing the primary constructor
     *                              of the struct. If {@code null}, the struct does not have a primary
     *                              constructor.
     * @param fields                A list of field declarations that define the properties of the struct.
     * @param methods               A list of method declarations that define the behavior or functionality
     *                              associated with the struct.
     * @param span                  The source span that represents the location of the struct
     *                              declaration in the source code.
     */
    public record StructDeclaration(DeclarationFlags flags, IToken name, @Nullable List<ParameterListParser.Parameter> primaryConstructor, List<FieldParser.FieldDeclaration> fields, List<FuncParser.FuncDeclaration> methods, SourceSpan span) {}

}
