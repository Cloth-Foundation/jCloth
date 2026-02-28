package cloth;

import cloth.args.ArgParser;
import cloth.error.DiagnosticSink;
import cloth.error.Error;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.lexer.LexerOptions;
import cloth.lexer.SourceBuffer;
import cloth.parser.Parser;
import cloth.utility.charsets.ConsoleIO;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;

public class Main {

    @Getter
    private static ArgParser parser;

    public static void main(String[] args) throws Error {
        ConsoleIO.forceUtf8();
        parser = new ArgParser();
        getParser().parse(args);

        String text;
        SourceFile file;

        /*if (args.length >= 1 && args[0] != null && !args[0].isBlank()) {
            file = new SourceFile(args[0]);
            text = file.getSourceText();
        } else*/ {
            file = new SourceFile("Sample.co");
            text = file.getSourceText();
        }

        SourceBuffer buffer = new SourceBuffer(file, text);
        DiagnosticSink diagnostics = new DiagnosticSink();
        LexerOptions options = LexerOptions.createFromArgs(new ArrayList<>(Arrays.asList(args)));

        Lexer lexer = new Lexer(buffer, diagnostics, options);

        new Parser(lexer, file).parse();
    }
}
