package cloth.parser.top;

import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.parser.ParserPart;
import cloth.token.IToken;
import cloth.token.TokenKind;
import cloth.token.Tokens;
import cloth.token.span.SourceSpan;
import org.jetbrains.annotations.NotNull;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;

public class ModuleParser extends ParserPart<ModuleParser.Module> {

    public ModuleParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    @Override
    public Module parse() {
        expect(Tokens.Keyword.Module, () -> new CompileError("Module must be declared at top level.", peek().span(), "You must declare the module for this Cloth Object.", "Insert module <relative.source.path>"));

        var name = parseQualifiedName();
        expectSemiColon();
        return new Module(name, name.span());
    }

    @SneakyThrows
    private QualifiedName parseQualifiedName() {
        IToken first;
        if (is(TokenKind.Identifier) || is(TokenKind.Keyword)) {
            first = advance();
        } else {
            throw new CompileError("Expected identifier", peek().span(), "Expected an identifier for the module name.", "Insert module <relative.source.path>");
        }

        var parts = new ArrayList<IToken>();;
        parts.add(first);

        while (match(Tokens.Operator.Dot)) {
            IToken part;
            if (is(TokenKind.Identifier) || is(TokenKind.Keyword)) {
                part = advance();
            } else {
                throw new CompileError("Expected identifier", peek().span(), "Expected an identifier for the module name.", "Insert module <relative.source.path>");
            }
            parts.add(part);
        }

        var segments = parts.stream().map(IToken::lexeme).toList();
        SourceSpan span = new SourceSpan(first.span().start(), parts.getLast().span().end());
        return new QualifiedName(segments, span);
    }

    public record QualifiedName(List<String> segments, SourceSpan span) {

        @Override
        public @NotNull String toString() {
            return String.join(".", segments);
        }

    }

    public record Module(QualifiedName name, SourceSpan span) {
    }

}
