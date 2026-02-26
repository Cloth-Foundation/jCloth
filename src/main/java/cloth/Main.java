package cloth;

import cloth.args.ArgParser;
import cloth.error.DiagnosticSink;
import cloth.error.Error;
import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.lexer.LexerOptions;
import cloth.lexer.SourceBuffer;
import cloth.token.span.SourceLocation;
import cloth.token.span.SourceSpan;
import cloth.utility.charsets.ConsoleIO;
import cloth.utility.printers.LexerPrinter;
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

        LexerPrinter.getInstance().print(lexer);

        new CompileError(
                "Invalid namespace",
                new SourceSpan(new SourceLocation(file, 6, 1, 8), new SourceLocation(file, 6, 1, 19)),
                "cloth namespace is reserved by the system.",
                "Change namespace to something else."
        );
    }
}
