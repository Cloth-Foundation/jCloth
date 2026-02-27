package cloth.lexer;

import cloth.error.DiagnosticSink;
import cloth.file.SourceFile;
import cloth.token.IToken;
import cloth.token.TokenKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class LexerTest {

    @TempDir
    Path tempDir;

    @Test
    public void testPreviousToken() throws IOException {
        Path testFile = tempDir.resolve("test.co");
        String content = "var x = 10;";
        Files.writeString(testFile, content);

        SourceFile sourceFile = new SourceFile(testFile.toString());
        SourceBuffer buffer = new SourceBuffer(sourceFile, content);
        DiagnosticSink diagnostics = new DiagnosticSink();
        LexerOptions options = new LexerOptions();

        Lexer lexer = new Lexer(buffer, diagnostics, options);

        assertNull(lexer.getPreviousToken(), "Initial previousToken should be null");

        // First token: var
        LexedToken lt1 = lexer.next();
        IToken tok1 = lt1.token();
        System.out.println("[DEBUG_LOG] First token: " + tok1.kind() + " '" + tok1.lexeme() + "'");
        assertEquals(TokenKind.Keyword, tok1.kind());
        assertEquals("var", tok1.lexeme());
        assertEquals(tok1, lexer.getPreviousToken(), "previousToken should be 'var'");

        // Second token: x
        LexedToken lt2 = lexer.next();
        IToken tok2 = lt2.token();
        assertEquals(TokenKind.Identifier, tok2.kind());
        assertEquals("x", tok2.lexeme());
        assertEquals(tok2, lexer.getPreviousToken(), "previousToken should be 'x'");

        // Third token: =
        LexedToken lt3 = lexer.next();
        IToken tok3 = lt3.token();
        assertEquals(TokenKind.Operator, tok3.kind());
        assertEquals("=", tok3.lexeme());
        assertEquals(tok3, lexer.getPreviousToken(), "previousToken should be '='");

        // Fourth token: 10
        LexedToken lt4 = lexer.next();
        IToken tok4 = lt4.token();
        assertEquals(TokenKind.Number, tok4.kind());
        assertEquals("10", tok4.lexeme());
        assertEquals(tok4, lexer.getPreviousToken(), "previousToken should be '10'");

        // Fifth token: ;
        LexedToken lt5 = lexer.next();
        IToken tok5 = lt5.token();
        assertEquals(TokenKind.Punctuation, tok5.kind());
        assertEquals(";", tok5.lexeme());
        assertEquals(tok5, lexer.getPreviousToken(), "previousToken should be ';'");

        // Sixth token: EndOfFile
        LexedToken lt6 = lexer.next();
        IToken tok6 = lt6.token();
        assertEquals(TokenKind.EndOfFile, tok6.kind());
        assertEquals(tok6, lexer.getPreviousToken(), "previousToken should be EndOfFile");
    }
}
