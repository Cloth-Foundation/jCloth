package cloth.parser.statements;

import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.parser.ParserPart;
import cloth.token.Tokens;
import cloth.token.span.SourceSpan;

/**
 * The {@code ModuleParser} class is responsible for parsing module declarations
 * from source code. It extends {@link ParserPart} and provides functionality to
 * validate module syntax and ensure proper structure according to the language grammar.
 * The module declaration specifies a logical grouping of code using a fully qualified
 * name and is expected to follow strict syntax rules.
 */
public final class ModuleParser extends ParserPart<ModuleParser.Module> {

    /**
     * Constructs a new instance of the ModuleParser, which is responsible for parsing
     * module declarations from the source code. This parser validates the module syntax
     * and ensures proper structure as per the expected language grammar.
     *
     * @param lexer the {@code Lexer} instance used to tokenize the source code; provides
     *              the stream of tokens that this parser will process.
     * @param file  the {@code SourceFile} representing the source code being parsed; it
     *              contains metadata and access to the raw contents of the file.
     */
    public ModuleParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    /**
     * Parses a module declaration in the source code.
     * <p>
     * This method expects a module keyword at the top level of the source code. If
     * the keyword is missing or improperly placed, it throws a {@link CompileError}
     * with an appropriate error message and suggested resolution.
     * <p>
     * Once the module keyword is validated, it proceeds to parse a fully qualified
     * name representing the module's path and structure. A semicolon is then expected
     * to terminate the module declaration. Any deviation from this syntax results in
     * a {@link CompileError}.
     *
     * @return a {@link java.lang.Module} instance representing the module declaration, which
     *         includes the fully qualified name of the module and its corresponding
     *         source span.
     */
    @Override
    public Module parse() {
        expect(Tokens.Keyword.Module, () -> new CompileError("Module must be declared at top level.", peek().span(), "You must declare the module for this Cloth Object.", "Insert module <relative.source.path>"));

        var name = new QualifiedNameParser(getLexer(), getFile()).parse();
        expectSemiColon();
        return new Module(name, name.span());
    }

    /**
     * Represents a module declaration in the source code.
     * <p>
     * A module provides a way to define a logical grouping of code. It includes
     * a qualified name to specify the module path and a source span to indicate the
     * location of the module declaration in the source file.
     *
     * @param name The fully qualified name of the module, composed of segments.
     * @param span The source span indicating where the module is located in the source file.
     */
    public record Module(QualifiedNameParser.QualifiedName name, SourceSpan span) {
    }

}
