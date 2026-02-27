package cloth.parser;

import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.parser.statements.ClassParser;
import cloth.parser.statements.ImportParser;
import cloth.parser.statements.ModuleParser;
import cloth.token.Token;
import cloth.token.TokenKind;
import cloth.token.Tokens;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class Parser extends ParserPart<Parser> {

    public Parser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    @Override
    public Parser parse() {
        var module = parseModule();
        var imports = parseImports();
        var declarations = parseTopLevelDeclarations();

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

    private List<ClassParser.ClassDeclaration> parseTopLevelDeclarations() {
        var declarations = new ArrayList<ClassParser.ClassDeclaration>();
        while (!isEndOfFile()) {
            Tokens.Keyword declarationKeyword = peekDeclarationKeyword();

            if (declarationKeyword == Tokens.Keyword.Class) {
                declarations.add(new ClassParser(getLexer(), getFile()).parse());
            } else {
                // TODO: dispatch to other declaration parsers (enum, struct, interface, func)
                break;
            }
        }
        return declarations;
    }

    /**
     * Peeks ahead past any modifier keywords to find the actual declaration keyword
     * (e.g. {@code class}, {@code func}, {@code enum}). Does not consume any tokens.
     */
    private Tokens.Keyword peekDeclarationKeyword() {
        int offset = 0;
        while (true) {
            var token = peek(offset);
            if (token.is(TokenKind.Keyword) && ((Token) token).keyword().isStorageModifier()) {
                offset++;
            } else if (token.is(TokenKind.Keyword) && ((Token) token).keyword().isModifier()) {
                offset++;
            } else {
                if (token.is(TokenKind.Keyword)) {
                    return ((Token) token).keyword();
                }
                return Tokens.Keyword.None;
            }
        }
    }
}
