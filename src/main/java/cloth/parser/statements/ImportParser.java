package cloth.parser.statements;

import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.parser.ParserPart;
import cloth.token.Tokens;
import cloth.token.span.SourceSpan;

public class ImportParser extends ParserPart<ImportParser.Import> {

    public ImportParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    @Override
    public Import parse() {
        if (!match(Tokens.Keyword.Import)) {
            return null;
        }

        QualifiedNameParser.QualifiedName name = new QualifiedNameParser(getLexer(), getFile()).parse();

        expectSemiColon();
        return new Import(name, name.span());
    }

    public record Import(QualifiedNameParser.QualifiedName name, SourceSpan span) {
    }

}
