package cloth.lexer;

import cloth.error.DiagnosticSink;
import cloth.file.SourceFile;
import cloth.parser.top.ModuleParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModuleParserTest {

    @TempDir
    Path tempDir;

    @Test
    public void test() throws IOException {
        Path testFile = tempDir.resolve("test.co");
        String content = "module my.test.mod;";
        Files.writeString(testFile, content);

        SourceFile sourceFile = new SourceFile(testFile.toString());
        SourceBuffer buffer = new SourceBuffer(sourceFile, content);
        DiagnosticSink diagnostics = new DiagnosticSink();
        LexerOptions options = new LexerOptions();

        Lexer lexer = new Lexer(buffer, diagnostics, options);

        ModuleParser parser = new ModuleParser(lexer, sourceFile);

        var module = parser.parse();

        assertEquals(content.substring(7, content.length() - 1), module.name().toString());

        // Tests the fully qualified path
        assertEquals(module.span().getLength(), content.substring(7, content.length() - 1).length());
    }

}
