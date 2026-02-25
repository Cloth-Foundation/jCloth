package cloth.utility.printers;

import cloth.lexer.LexedToken;
import cloth.lexer.Lexer;
import cloth.lexer.trivia.Trivia;
import cloth.token.IToken;
import cloth.token.Token;
import cloth.token.TokenKind;
import cloth.token.meta.MetaToken;
import cloth.token.span.SourceSpan;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LexerPrinter implements IPrinter<Lexer> {

    private static final LexerPrinter INSTANCE = new LexerPrinter();

    public static LexerPrinter getInstance() {
        return INSTANCE;
    }

    @Override
    public void print(Lexer lexer) {
        List<Entry> entries = new ArrayList<>();
        while (true) {
            LexedToken lt = lexer.next();
            IToken t = lt.token();

            String kw = "";
            String op = "";
            String meta = "";

            if (t instanceof Token tok) {
                kw = (tok.keyword() != null && tok.keyword().isKeyword()) ? tok.keyword().name() : "";
                op = (tok.operator() != null && tok.operator().isOperator()) ? tok.operator().name() : "";
            }
            if (t instanceof MetaToken mt) {
                meta = mt.keyword() != null ? mt.keyword().name() : "";
            }

            entries.add(new Entry(t.span().start().getFile().getName(), t.kind().name(), quote(t.lexeme()), kw, op, meta, t.span(), lt.trivia()));

            if (t.kind() == TokenKind.EndOfFile) break;
        }

        String[] headers = {"File", "Kind", "Lexeme", "Keyword", "Operator", "Meta", "Span", "Trivia"};
        int[] widths = new int[headers.length];

        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }

        for (Entry entry : entries) {
            widths[0] = Math.max(widths[0], entry.file.length());
            widths[1] = Math.max(widths[1], entry.kind.length());
            widths[2] = Math.max(widths[2], entry.lexeme.length());
            widths[3] = Math.max(widths[3], entry.keyword.length());
            widths[4] = Math.max(widths[4], entry.op.length());
            widths[5] = Math.max(widths[5], entry.meta.length());
            widths[6] = Math.max(widths[6], formatSpan(entry.span).length());
            widths[7] = Math.max(widths[7], formatTrivia(entry.trivia).length());
        }

        printRow(headers, widths);
        printSeparator(widths);
        String lastFile = null;
        for (Entry entry : entries) {
            String displayFile = "";
            if (lastFile == null || !lastFile.equals(entry.file)) {
                displayFile = entry.file;
                lastFile = entry.file;
            }

            printRow(new String[]{
                    displayFile,
                    entry.kind,
                    entry.lexeme,
                    entry.keyword,
                    entry.op,
                    entry.meta,
                    formatSpan(entry.span),
                    formatTrivia(entry.trivia)
            }, widths);
        }
    }

    private static void printRow(String[] columns, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < columns.length; i++) {
            sb.append(" ").append(padRight(columns[i], widths[i])).append(" |");
        }
        System.out.println(sb);
    }

    private static void printSeparator(int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int width : widths) {
            sb.append("-".repeat(width + 2)).append("|");
        }
        System.out.println(sb);
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    private static String formatSpan(SourceSpan span) {
        return String.format("[%d:%d..%d:%d]",
                span.start().getLine(), span.start().getColumn(),
                span.end().getLine(), span.end().getColumn());
    }

    private static String formatTrivia(Trivia trivia) {
        return trivia.leading().size() + "/" + trivia.trailing().size();
    }

    private static String quote(String s) {
        if (s == null) return "<null>";
        String escaped = s
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    public record Entry(String file, String kind, String lexeme, String keyword, String op, @Nullable String meta, SourceSpan span, Trivia trivia) {
    }

}
