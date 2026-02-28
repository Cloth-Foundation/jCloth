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
 */
@SuppressWarnings("unused")
public class Parser extends ParserPart<Parser> {

    public Parser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    @Override
    public Parser parse() {
        var module = parseModule();
        var imports = parseImports();
        parseTopLevelDeclarations();

        return this;
    }

    private ModuleParser.Module parseModule() {
        return new ModuleParser(getLexer(), getFile()).parse();
    }

    private List<ImportParser.Import> parseImports() {
        var imports = new ArrayList<ImportParser.Import>();
        ImportParser.Import imp;
        while ((imp = new ImportParser(getLexer(), getFile()).parse()) != null) {
            imports.add(imp);
        }
        return imports;
    }

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
