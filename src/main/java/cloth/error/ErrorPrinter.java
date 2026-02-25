package cloth.error;

import cloth.error.type.Error;
import cloth.file.SourceFile;
import cloth.token.span.SourceLocation;
import cloth.token.span.SourceSpan;
import cloth.utility.Ansi;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ErrorPrinter {

    private static final int CONTEXT = 1;
    private static final int TAB_WIDTH = 4;

    private static final PrintStream out = new PrintStream(System.err, true, StandardCharsets.UTF_8);

    public static void print(Error error) {
        if (error instanceof SpannedDiagnostic sp) {
            printSpanned(error, sp);
            return;
        }

        out.println(Ansi.RED + "Error: " + reset() + error.getMessage());
    }

    private static void printSpanned(Error error, SpannedDiagnostic sp) {
        SourceSpan span = sp.getSpan();
        if (span == null || !span.isValid()) {
            out.println(Ansi.RED + "Error: " + reset() + error.getMessage());
            return;
        }

        SourceLocation start = span.start();
        SourceLocation end = span.end();
        SourceFile file = start.getFile();

        List<String> rawLines = splitLines(file.getSourceText());

        int startLine = start.getLine();
        int startCol  = start.getColumn();
        int endLine   = end.getLine();
        int endCol    = end.getColumn();

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

    private record UnderlineRange(int start, int len) {}

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

    private static void printUnderline(int gutterWidth, int uStart, int uLen, int dropCol) {
        // Build underline as characters so we can inject the junction.
        char[] line = new char[uStart + uLen];
        for (int i = 0; i < line.length; i++) line[i] = ' ';
        for (int i = uStart; i < uStart + uLen; i++) line[i] = '─';

        if (dropCol >= uStart && dropCol < uStart + uLen) {
            line[dropCol] = ConnectingPipe.JUNCTION.toString().charAt(0);
        }

        out.println("  " + " ".repeat(gutterWidth) + " " + ConnectingPipe.VERTICAL + " " + Ansi.MAGENTA + new String(line) + reset());
    }

    private static void printLabel(int gutterWidth, int drop, String label) {
        // Prints vertical line
        // out.println("  " + " ".repeat(gutterWidth) + " " + ConnectingPipe.VERTICAL + " " + " ".repeat(drop) + Ansi.MAGENTA + ConnectingPipe.VERTICAL + reset());

        // Prints elbow + label
        out.println("  " + " ".repeat(gutterWidth) + " " + ConnectingPipe.VERTICAL + " " + " ".repeat(drop) + Ansi.MAGENTA + ConnectingPipe.ELBOW + ConnectingPipe.LABEL_BAR + " " + reset() + Ansi.MAGENTA + Ansi.HIGH_INTENSITY + label + reset());
    }

    private static void header(Error error, SourceFile file, int startLine, int startCol) throws MalformedURLException {
        var fullPath = file.getFile().toURI().toURL();
        out.println(Ansi.RED + Ansi.HIGH_INTENSITY + "Error: " + reset() + error.getMessage());
        out.println("    " + ConnectingPipe.CORNER_TOP_LEFT + ConnectingPipe.HORIZONTAL + " [" + fullPath + ":" + startLine + ":" + startCol + "]");
    }

    // ---------- Helpers ----------
    private static int clamp(int v, int min, int max) {
        if (v < min) return min;
        return Math.min(v, max);
    }

    private static List<String> splitLines(String src) {
        src = src.replace("\r\n", "\n").replace('\r', '\n');
        String[] arr = src.split("\n", -1);
        List<String> out = new ArrayList<>(arr.length);
        for (String s : arr) out.add(s);
        return out;
    }

    private static String getLine(List<String> lines, int oneBased) {
        int idx = oneBased - 1;
        if (idx < 0 || idx >= lines.size()) return "";
        return lines.get(idx);
    }

    private static String padLeft(int n, int width) {
        String s = String.valueOf(n);
        return " ".repeat(Math.max(0, width - s.length())) + s;
    }

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

    private enum ConnectingPipe {
        CORNER_TOP_LEFT("╭"),
        CORNER_TOP_RIGHT("╮"),
        CORNER_BOTTOM_LEFT("╰"),
        CORNER_BOTTOM_RIGHT("╯"),
        HORIZONTAL("─"),
        VERTICAL("│"),
        JUNCTION("┬"),
        ELBOW("╰"),
        LABEL_BAR("─");

        final String character;

        ConnectingPipe(final String character) {
            this.character = character;
        }

        @Override
        public String toString() {
            return character;
        }
    }
}