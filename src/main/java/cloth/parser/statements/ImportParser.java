package cloth.parser.statements;

import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.parser.ParserPart;
import cloth.token.Tokens;
import cloth.token.span.SourceSpan;

/**
 * A subclass of {@code ParserPart} that is responsible for parsing import statements
 * from a given source using a provided {@code Lexer} and {@code SourceFile}.
 * The primary function of this class is to extract and wrap import-related data
 * into an {@code Import} record, which consists of the import's qualified name
 * and its location in the source file.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
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
