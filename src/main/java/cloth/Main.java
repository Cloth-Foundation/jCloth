package cloth;

import cloth.error.DiagnosticSink;
import cloth.error.ErrorPrinter;
import cloth.error.type.Error;
import cloth.error.type.SpannedCompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.lexer.LexerOptions;
import cloth.lexer.SourceBuffer;
import cloth.token.span.SourceLocation;
import cloth.token.span.SourceSpan;
import cloth.utility.printers.LexerPrinter;

import java.util.ArrayList;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) throws Error {
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

        LexerPrinter.getInstance().print(lexer);

        throw new SpannedCompileError(
                "Invalid namespace",
                new SourceSpan(new SourceLocation(file, 6, 1, 8), new SourceLocation(file, 6, 1, 19)),
                "cloth namespace is reserved by the system.",
                "Change namespace to something else."
        ).andThrow(1);
    }
}
