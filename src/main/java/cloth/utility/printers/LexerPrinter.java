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

/**
 * A utility class for printing a human-readable representation of the tokens processed
 * by a {@link Lexer}. This class provides a formatted tabular output describing various
 * properties of each token, including its kind, lexeme, and associated meta-information.
 * Additionally, it summarises the source location and any trivia associated with each token.
 * <p>
 * This implementation is designed as a singleton to ensure that only one instance
 * of the {@code LexerPrinter} exists throughout the application.
 * <p>
 * Features:
 * <li>Processes tokens produced by a {@link Lexer} and organizes them in a tabular format.</li>
 * <li>Computes column widths dynamically to ensure proper alignment of output.</li>
 * <li>Highlights changes between files by displaying the file name only when it changes.</li>
 */
public class LexerPrinter implements Printer<Lexer> {

    /**
     * A singleton instance of the {@code LexerPrinter} class.
     * This instance ensures a single, globally accessible point of access
     * for printing lexed tokens in a formatted manner.
     * <p>
     * The {@code INSTANCE} is initialized once at class loading time and
     * provides methods to work specifically with lexed tokens, aiding in
     * presentation and debugging processes.
     */
    private static final LexerPrinter INSTANCE = new LexerPrinter();

    /**
     * Provides a singleton instance of the {@code LexerPrinter} class.
     * This ensures that a single instance of the printer is shared
     * and reused whenever requested.
     *
     * @return the singleton instance of {@code LexerPrinter}
     */
    public static LexerPrinter getInstance() {
        return INSTANCE;
    }

    /**
     * Prints the lexed tokens from the provided {@code Lexer} in a tabulated format.
     * The output includes details such as the file, token kind, lexeme, keyword, operator,
     * metadata, span, and trivia associated with each token.
     *
     * @param lexer the {@code Lexer} instance from which tokens will be retrieved and printed;
     *              must not be null
     */
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

            entries.add(new Entry(t.span().start().file().getName(), t.kind().name(), quote(t.lexeme()), kw, op, meta, t.span(), lt.trivia()));

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

    /**
     * Prints a single row of a table using the provided column values and their respective widths.
     * Each column value is padded to match its corresponding width, and the columns are
     * separated by vertical bars.
     *
     * @param columns an array of strings representing the content of each column in the row;
     *                must not be null and must have the same length as the {@code widths} array
     * @param widths an array of integers specifying the width of each column in the row;
     *               must not be null and must have the same length as the {@code columns} array
     */
    private static void printRow(String[] columns, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < columns.length; i++) {
            sb.append(" ").append(padRight(columns[i], widths[i])).append(" |");
        }
        System.out.println(sb);
    }

    /**
     * Prints a separator line based on the provided column widths.
     * The separator consists of vertical bars and dashes, with
     * the number of dashes determined by each column's respective width.
     *
     * @param widths an array of integers specifying the width of each column;
     *               must not be null
     */
    private static void printSeparator(int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int width : widths) {
            sb.append("-".repeat(width + 2)).append("|");
        }
        System.out.println(sb);
    }

    /**
     * Pads the given string on the right with spaces until it reaches the desired length.
     * If the input string is already equal to or longer than the specified length,
     * it is returned unchanged.
     *
     * @param string the input string to be padded; must not be null
     * @param totalWidth the total width of the string after padding
     * @return the input string padded with spaces on the right to the specified width
     */
    private static String padRight(String string, int totalWidth) {
        return String.format("%-" + totalWidth + "s", string);
    }

    /**
     * Formats the given {@code SourceSpan} into a string representation denoting its
     * start and end locations within a source file.
     * The formatted string is of the form "[startLine:startColumn..endLine:endColumn]".
     *
     * @param span the {@code SourceSpan} object representing the span to be formatted;
     *             must not be null
     * @return a string representing the formatted span in the specified format
     */
    private static String formatSpan(SourceSpan span) {
        return String.format("[%d:%d..%d:%d]",
                span.start().line(), span.start().column(),
                span.end().line(), span.end().column());
    }

    /**
     * Formats the given {@code Trivia} by calculating and returning a string
     * representing the sizes of the leading and trailing trivia collections.
     * The format of the returned string is "leadingSize/trailingSize".
     *
     * @param trivia the {@code Trivia} object whose leading and trailing sizes
     *               are to be formatted; must not be null
     * @return a string in the format "leadingSize/trailingSize", where
     *         leadingSize is the size of the leading trivia collection
     *         and trailingSize is the size of the trailing trivia collection
     */
    private static String formatTrivia(Trivia trivia) {
        return trivia.leading().size() + "/" + trivia.trailing().size();
    }

    /**
     * Escapes special characters in the input string and surrounds it with double quotes.
     * If the input string is null, it returns the string "<null>".
     *
     * @param input the input string to be quoted; may be null
     * @return a quoted string with special characters escaped, or "<null>" if the input is null
     */
    private static String quote(String input) {
        if (input == null) return "<null>";
        String escaped = input
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    /**
     * Represents an entry containing lexical and syntactical details about a specific token
     * or element within a source file. This record is primarily used as part of the lexical
     * analysis process and maintains detailed information about the token's attributes and its location.
     *
     * @param file    The name of the file from which the entry was extracted.
     * @param kind    The general classification of the entry (e.g., "identifier", "keyword").
     * @param lexeme  The exact sequence of characters that represents the entry in the source file.
     * @param keyword The keyword associated with the entry, if applicable.
     * @param op      The operator represented by the entry, if applicable.
     * @param meta    Optional metadata associated with the entry, providing additional context.
     * @param span    The span in the source file where the entry is located, specifying its start
     *                and end positions.
     * @param trivia  The leading and trailing trivia (e.g., comments, whitespace) associated with
     *                the entry, providing non-essential contextual information.
     */
    public record Entry(String file, String kind, String lexeme, String keyword, String op, @Nullable String meta, SourceSpan span, Trivia trivia) {
    }

}
