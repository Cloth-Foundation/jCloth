package cloth.parser;

import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.parser.statements.ClassParser;
import cloth.parser.statements.EnumParser;
import cloth.parser.statements.ImportParser;
import cloth.parser.statements.InterfaceParser;
import cloth.parser.statements.ModuleParser;
import cloth.parser.statements.StructParser;
import cloth.token.Tokens;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code Parser} class is responsible for parsing a source file into
 * its constituent code components, such as modules, imports, and top-level
 * declarations. This class extends the {@link ParserPart} class to inherit
 * shared parsing functionality.
 * <p>
 * The parsing process consists of three main stages:
 * <ol>
 *     <li>Parsing the module declaration.</li>
 *     <li>Parsing import statements.</li>
 *     <li>Parsing top-level code declarations (e.g., classes).</li>
 * </ol>
 * <p>
 * This class leverages specialized parsers, such as {@link ModuleParser},
 * {@link ImportParser}, and {@link ClassParser}, to handle the parsing for
 * specific parts of the source file.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public class Parser extends ParserPart<Parser> {

    public Parser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    /**
     * Parses a source file into its modular components.
     * The result is stored internally within the {@code Parser} instance.
     *
     * @return The current {@code Parser} instance after completing the parsing process.
     */
    @Override
    public Parser parse() {
        var module = parseModule();
        var imports = parseImports();
        parseTopLevelDeclarations();

        return this;
    }

    /**
     * Parses the module declaration from the source file.
     * Invokes the {@link ModuleParser} to process the module-specific syntax,
     * such as the fully qualified module name and related structural validations.
     *
     * @return A {@link ModuleParser.Module} instance representing the parsed module declaration,
     *         including its fully qualified name and source span information.
     */
    private ModuleParser.Module parseModule() {
        return new ModuleParser(getLexer(), getFile()).parse();
    }

    /**
     * Parses import statements from the current source file using an {@code ImportParser}.
     * Iteratively processes the input to extract all valid import declarations,
     * adds them to a list, and returns the constructed list as the result.
     * Each import declaration is represented as an {@code ImportParser.Import} instance,
     * encapsulating the qualified name and source span of the import.
     *
     * @return A list of {@code ImportParser.Import} instances representing
     *         all import declarations in the source file, or an empty list if no
     *         imports are found.
     */
    private List<ImportParser.Import> parseImports() {
        var imports = new ArrayList<ImportParser.Import>();
        ImportParser.Import imp;
        while ((imp = new ImportParser(getLexer(), getFile()).parse()) != null) {
            imports.add(imp);
        }
        return imports;
    }

    /**
     * Parses the top-level declarations in the source file.
     * This method iterates through the input stream of tokens and processes
     * declarations of classes, enums, structs, and interfaces using their respective parsers.
     * <p>
     * The method identifies the type of declaration based on the keyword at the current
     * parsing position and invokes the appropriate parser to handle it.
     * All parsed declarations are collected in separate lists, grouped by their types.
     */
    private void parseTopLevelDeclarations() {
        var classes = new ArrayList<ClassParser.ClassDeclaration>();
        var enums = new ArrayList<EnumParser.EnumDeclaration>();
        var structs = new ArrayList<StructParser.StructDeclaration>();
        var interfaces = new ArrayList<InterfaceParser.InterfaceDeclaration>();

        while (!isEndOfFile()) {
            Tokens.Keyword declarationKeyword = peekDeclarationKeyword();
            switch (declarationKeyword) {
                case Tokens.Keyword.Class:
                    classes.add(new ClassParser(getLexer(), getFile()).parse());
                    break;
                case Tokens.Keyword.Enum:
                    enums.add(new EnumParser(getLexer(), getFile()).parse());
                    break;
                case Tokens.Keyword.Struct:
                    structs.add(new StructParser(getLexer(), getFile()).parse());
                    break;
                case Tokens.Keyword.Interface:
                    interfaces.add(new InterfaceParser(getLexer(), getFile()).parse());
                    break;
                default:
                    break;
            }
        }
    }

}
