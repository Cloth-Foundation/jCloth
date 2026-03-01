package cloth.utility.printers;

import cloth.Main;
import cloth.args.ArgFlags;
import cloth.error.Diagnostic;
import cloth.error.Error;
import cloth.file.SourceFile;
import cloth.token.span.SourceLocation;
import cloth.token.span.SourceSpan;
import cloth.utility.Ansi;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A utility class for printing detailed error information with annotated spans and
 * formatting. The ErrorPrinter class provides methods for rendering errors, including
 * their locations in source files, associated messages, and supplementary details
 * such as underlines and labels.
 * <p>
 * This class is a singleton and extends Printer<Error> to fulfill the generic
 * printing contract for error objects.
 * <p>
 * Fields:
 * <li>CONTEXT: A constant string used to represent the printing context for errors.</li>
 * <li>TAB_WIDTH: A constant integer representing the number of spaces per tab.</li>
 * <li>INSTANCE: A singleton instance of the ErrorPrinter class that can be reused across the application.</li>
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public class ErrorPrinter implements Printer<Error> {

    /**
     * Represents the context level used to manage the amount of surrounding lines
     * displayed when printing errors. This value is used to determine how much
     * contextual information is shown around the highlighted error in a source file.
     * A higher value indicates more lines of surrounding context will be included
     * in the output.
     */
    private static final int CONTEXT = 1;

    /**
     * Defines the default tab width used for expanding tab characters into spaces
     * when printing or formatting text. Each tab character is replaced with this
     * number of spaces to ensure consistent alignment in the output.
     * <p>
     * This value is utilized in methods that process or display text with tab
     * characters, ensuring predictable spacing and formatting.
     */
    private static final int TAB_WIDTH = 4;

    /**
     * A singleton instance of the {@link ErrorPrinter} class.
     * <p>
     * This instance provides a centralized mechanism for printing errors
     * with detailed and annotated formatting. It ensures that only one
     * instance of the class is created and shared across the application,
     * adhering to the Singleton design pattern.
     */
    private static final ErrorPrinter INSTANCE = new ErrorPrinter();

    /**
     * Prints the provided error object by delegating to the {@code printSpanned} method,
     * which handles formatted output that includes the error message and associated span details.
     *
     * @param error the error object to print; must not be null
     */
    @Override
    public void print(@NotNull Error error) {
        printSpanned(error, error);
    }

    /**
     * Returns the singleton instance of the {@link ErrorPrinter} class.
     *
     * @return the singleton {@code ErrorPrinter} instance
     */
    public static ErrorPrinter getInstance() {
        return INSTANCE;
    }

    /**
     * Prints a detailed, annotated representation of an error with its associated span
     * from a source file. This method highlights the relevant lines of code, underlines
     * the specific span, and optionally adds an associated label and help message.
     *
     * @param error the error object containing the error details, including its message
     * @param sp the spanned diagnostic object representing the span of the error in the
     *           source file and any associated metadata such as labels or help messages
     */
    private static void printSpanned(Error error, Diagnostic sp) {
        SourceSpan span = sp.getSpan();
        if (span == null || !span.isValid()) {
            out.println(Ansi.RED + "Error: " + reset() + error.getMessage());
            return;
        }

        SourceLocation start = span.start();
        SourceLocation end = span.end();
        SourceFile file = start.file();

        List<String> rawLines = splitLines(file.getSourceText());

        int startLine = start.line();
        int startCol  = start.column();
        int endLine   = end.line();
        int endCol    = end.column();

        // Header
        try {
            header(error, file, startLine, startCol);
        } catch (MalformedURLException ignored) {
        }

        int from = Math.max(1, startLine - CONTEXT);
        int to   = Math.min(rawLines.size(), endLine + CONTEXT);
        int gutterWidth = String.valueOf(to).length();

        Integer labelDropCol = null;

        for (int ln = from; ln <= to; ln++) {
            String srcRaw = getLine(rawLines, ln);
            String src = expandTabs(srcRaw, TAB_WIDTH);

            printLine(ln, gutterWidth, src);

            if (ln < startLine || ln > endLine) continue;

            UnderlineRange u = computeUnderline(ln, startLine, startCol, endLine, endCol, src.length());

            if (labelDropCol == null && ln == startLine) {
                labelDropCol = u.start + (u.len / 2);
            }

            int drop = (labelDropCol != null ? labelDropCol : (u.start + u.len - 1));
            printUnderline(gutterWidth, u.start, u.len, drop);

            if (ln == startLine) {
                printLabel(gutterWidth, drop, sp.getLabel());
            }
        }

        // Bottom connector
        out.println("  " + " ".repeat(gutterWidth) + " " + ConnectingPipe.CORNER_BOTTOM_LEFT + ConnectingPipe.HORIZONTAL);

        // Help / hint
        String help = sp.getHelp();
        if (help != null && !help.isBlank()) {
            out.println(Ansi.GREEN + Ansi.HIGH_INTENSITY + "Help: " + reset() + help);
        }
    }

    /**
     * Computes the range for underlining a portion of a source line based on the provided
     * line and column positions. The method calculates the starting index and the length
     * of the underline relative to a specific line, taking into account multi-line spans
     * and ensuring the underline range remains within the bounds of the source line.
     *
     * @param ln the line number for which the underline is being computed
     * @param startLine the line number where the spanning starts
     * @param startCol the column within the start line where the underline begins (1-based)
     * @param endLine the line number where the spanning ends
     * @param endCol the column within the end line where the underline ends (1-based)
     * @param srcLen the length of the source line, used to constrain the calculated range
     * @return an UnderlineRange object representing the computed starting position (0-based)
     *         and the length of the underline within the specified line
     */
    private static UnderlineRange computeUnderline(int ln, int startLine, int startCol, int endLine, int endCol, int srcLen) {
        int uStart;
        int uLen;

        if (startLine == endLine) {
            uStart = clamp(startCol - 1, 0, srcLen);
            int uEndExclusive = clamp(endCol - 1, uStart + 1, srcLen); // ensure >=1 char
            uLen = Math.max(1, uEndExclusive - uStart);
        } else if (ln == startLine) {
            uStart = clamp(startCol - 1, 0, srcLen);
            uLen = Math.max(1, srcLen - uStart);
        } else if (ln == endLine) {
            uStart = 0;
            int uEndExclusive = clamp(endCol - 1, 1, srcLen);
            uLen = Math.max(1, uEndExclusive - uStart);
        } else {
            uStart = 0;
            uLen = Math.max(1, srcLen);
        }
        return new UnderlineRange(uStart, uLen);
    }


    /**
     * Prints a formatted line to the output stream, including a line number,
     * gutter spacing, and the source content.
     *
     * @param ln the line number to display in the gutter
     * @param gutterWidth the width of the gutter space used for aligning line numbers
     * @param src the source content to display on the line
     */
    private static void printLine(int ln, int gutterWidth, String src) {
        out.println("  " + Ansi.WHITE + padLeft(ln, gutterWidth) + reset() + " " + ConnectingPipe.VERTICAL + " " + src);
    }

    /**
     * Prints an underline representation with customizable spacing, alignment, and character placements.
     *
     * @param gutterWidth the width of the gutter, used to align the underline relative to other elements
     * @param uStart the starting position of the underline within the line
     * @param uLen the length of the underline to be drawn
     * @param dropCol the column where a junction character will be inserted, if applicable
     */
    private static void printUnderline(int gutterWidth, int uStart, int uLen, int dropCol) {
        char[] line = new char[uStart + uLen];
        Arrays.fill(line, ' ');
        for (int i = uStart; i < uStart + uLen; i++) line[i] = '─';

        if (dropCol >= uStart && dropCol < uStart + uLen) {
            line[dropCol] = ConnectingPipe.JUNCTION.getChar();
        }

        out.println("  " + " ".repeat(gutterWidth) + " " + ConnectingPipe.BULLET + " " + Ansi.MAGENTA + new String(line) + reset());
    }

    /**
     * Prints a formatted label with a gutter and visual decorations, such as an elbow
     * and connecting elements.
     *
     * @param gutterWidth the width of the gutter space, used for aligning the label
     * @param drop the spacing applied after the vertical line and before the elbow
     * @param label the text to display as the label
     */
    private static void printLabel(int gutterWidth, int drop, String label) {
        // Prints vertical line
        // out.println("  " + " ".repeat(gutterWidth) + " " + ConnectingPipe.VERTICAL + " " + " ".repeat(drop) + Ansi.MAGENTA + ConnectingPipe.VERTICAL + reset());

        // Prints elbow + label
        out.println("  " + " ".repeat(gutterWidth) + " " + ConnectingPipe.BULLET + " " + " ".repeat(drop) + Ansi.MAGENTA + ConnectingPipe.ELBOW + ConnectingPipe.LABEL_BAR + " " + reset() + Ansi.MAGENTA + Ansi.HIGH_INTENSITY + label + reset());
    }

    /**
     * Prints a formatted error message including the file path, line, and column details.
     * The method outputs information about the specified error and its location within
     * the associated source file. The output formatting may vary based on certain flags.
     *
     * @param error the error object containing details about the error, including its message
     * @param file the source file where the error occurred
     * @param startLine the line number in the source file where the error starts
     * @param startCol the column number in the source file where the error starts
     * @throws MalformedURLException if the file path cannot be converted to a URL
     */
    private static void header(Error error, SourceFile file, int startLine, int startCol) throws MalformedURLException {
        var fullPath = file.getFile().toURI().toURL();
        out.println(Ansi.RED + Ansi.HIGH_INTENSITY + "Error: " + reset() + error.getMessage());

        boolean jetbrains = false;
        var parser = Main.getParser();
        if (parser != null) {
            try {
                jetbrains = Boolean.TRUE.equals(parser.get(ArgFlags.JETBRAINS_TERMINAL, Boolean.class));
            } catch (Exception ignored) {
                // Fallback to default path formatting when flags are not available
            }
        }
        String filePath = jetbrains ? " [" + fullPath + ":" + startLine + ":" + startCol + "]" : " " + file.getPath() + ":" + startLine + ":" + startCol;
        out.println("    " + ConnectingPipe.CORNER_TOP_LEFT + ConnectingPipe.HORIZONTAL + filePath);
    }

    /**
     * Clamps a given value within the specified minimum and maximum bounds.
     *
     * @param v the value to clamp
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @return the clamped value, which will be between {@code min} and {@code max} (inclusive)
     */
    private static int clamp(int v, int min, int max) {
        if (v < min) return min;
        return Math.min(v, max);
    }

    /**
     * Splits the given string into a list of lines, normalizing line breaks.
     * <p>
     * This method replaces all variations of line breaks (CRLF, CR) with a
     * single line feed character (LF) before splitting the string into lines.
     *
     * @param src the source string to split into lines
     * @return a list of strings, where each string corresponds to a line in the source
     */
    private static List<String> splitLines(String src) {
        src = src.replace("\r\n", "\n").replace('\r', '\n');
        String[] arr = src.split("\n", -1);
        List<String> out = new ArrayList<>(arr.length);
        Collections.addAll(out, arr);
        return out;
    }

    /**
     * Retrieves a specific line from the provided list of strings, based on a one-based index.
     * If the specified index is out of bounds, an empty string is returned.
     *
     * @param lines the list of strings representing lines of text
     * @param oneBased the one-based index of the line to retrieve
     * @return the line at the specified one-based index, or an empty string if the index is invalid
     */
    private static String getLine(List<String> lines, int oneBased) {
        int idx = oneBased - 1;
        if (idx < 0 || idx >= lines.size()) return "";
        return lines.get(idx);
    }

    /**
     * Pads the provided integer with spaces on the left to ensure it reaches the specified width.
     * If the string representation of the integer is already greater than or equal to the specified width,
     * no padding will be added.
     *
     * @param n the integer to be padded
     * @param width the total width of the resulting string, including the padding
     * @return a string representation of the integer, left-padded with spaces as needed
     */
    private static String padLeft(int n, int width) {
        String s = String.valueOf(n);
        return " ".repeat(Math.max(0, width - s.length())) + s;
    }

    /**
     * Expands tab characters in the given string into spaces, based on the specified tab width.
     *
     * @param s the input string containing tab characters to expand
     * @param tabWidth the number of spaces each tab character should be replaced with
     * @return a new string with all tab characters replaced by the appropriate number of spaces
     */
    private static String expandTabs(String s, int tabWidth) {
        StringBuilder out = new StringBuilder(s.length());
        int col = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\t') {
                int spaces = tabWidth - (col % tabWidth);
                out.append(" ".repeat(spaces));
                col += spaces;
            } else {
                out.append(ch);
                col++;
            }
        }
        return out.toString();
    }

    private static String reset() {
        return Ansi.SANE + Ansi.LOW_INTENSITY;
    }

    /**
     * Represents various types of connecting pipe characters used for visual formatting
     * and rendering structures such as corners, lines, and junctions. Each enum constant
     * is associated with a specific Unicode character that visually corresponds to its purpose.
     * <p>
     * The connecting pipes support various orientations, including horizontal, vertical,
     * and corner junctions, as well as decorations such as bullets and labels.
     */
    private enum ConnectingPipe {
        CORNER_TOP_LEFT("╭"),
        CORNER_TOP_RIGHT("╮"),
        CORNER_BOTTOM_LEFT("╰"),
        CORNER_BOTTOM_RIGHT("╯"),
        HORIZONTAL("─"),
        VERTICAL("│"),
        JUNCTION("┬"),
        ELBOW("╰"),
        LABEL_BAR("─"),
        BULLET("•");

        final String character;

        ConnectingPipe(final String character) {
            this.character = character;
        }

        @Override
        public String toString() {
            return character;
        }

        /**
         * Retrieves the first character of the string representation associated with the enum constant.
         *
         * @return the first character of the string representation of this enum constant
         */
        public char getChar() {
            return character.charAt(0);
        }
    }

    /**
     * Represents a range where underlining should occur within a source line.
     * Instances of this record define the starting position and the length
     * of the underline.
     * <p>
     * The start position is zero-based and indicates where the underline begins,
     * while the length specifies the number of characters to be underlined.
     */
    private record UnderlineRange(int start, int len) {}
}