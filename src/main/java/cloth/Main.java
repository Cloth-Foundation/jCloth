package cloth;

import cloth.error.DiagnosticSink;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.lexer.LexerOptions;
import cloth.lexer.SourceBuffer;

import java.util.ArrayList;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        String text;
        SourceFile file;

        if (args.length >= 1 && args[0] != null && !args[0].isBlank()) {
            file = new SourceFile(args[0]);
            text = file.getSourceText();
        } else {
            file = new SourceFile("Sample.co");
            text = file.getSourceText();
        }

        SourceBuffer buffer = new SourceBuffer(file, text);
        DiagnosticSink diagnostics = new DiagnosticSink();
        LexerOptions options = LexerOptions.createFromArgs(new ArrayList<>(Arrays.asList(args)));

        Lexer lexer = new Lexer(buffer, diagnostics, options);

        Lexer.printLexer(lexer, text);
    }
}
