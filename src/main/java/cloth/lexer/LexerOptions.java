package cloth.lexer;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

/**
 * Configuration options for tuning the behavior of a lexer.
 * This class provides various settings that influence the process
 * of tokenizing input, including handling of whitespace, comments,
 * trivia, and character encoding.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public class LexerOptions {

    @Getter
    @Setter
    private boolean emitWhitespace = false;

    @Getter
    @Setter
    private boolean emitComments = false;

    @Getter
    @Setter
    private boolean keepTrivia = false;

    @Getter
    @Setter
    private boolean allowUnicodeCharacters = false;

    @Getter
    @Setter
    private int maxStringLiteralBytes = 1 << 20;

    public LexerOptions() {
        this(false, false, false, false);
    }

    public LexerOptions(boolean emitWhitespace, boolean emitComments, boolean emitTrivia, boolean allowUnicodeCharacters) {
        this.emitWhitespace = emitWhitespace;
        this.emitComments = emitComments;
        this.keepTrivia = emitTrivia;
        this.allowUnicodeCharacters = allowUnicodeCharacters;
    }

    /**
     * Creates a new instance of {@code LexerOptions} and configures it based on the provided arguments.
     * The method interprets a list of command-line-style arguments to set various options
     * such as emitting whitespace, comments, keeping trivia, and allowing Unicode characters.
     *
     * @param args A list of strings representing command-line arguments that determine
     *             the configuration of the resulting {@code LexerOptions} object.
     *             Recognized arguments include:
     *             - {@code --emit-whitespace}: Enables emission of whitespace tokens.
     *             - {@code --emit-comments}: Enables emission of comment tokens.
     *             - {@code --keep-trivia}: Retains trivia information in the token stream.
     *             - {@code --allow-unicode}: Allows the use of Unicode characters.
     * @return A configured {@code LexerOptions} instance based on the provided arguments.
     */
    public static LexerOptions createFromArgs(ArrayList<String> args) {
        LexerOptions options = new LexerOptions();
        for (String arg : args) {
            if (arg.equals("--emit-whitespace")) options.emitWhitespace = true;
            if (arg.equals("--emit-comments")) options.emitComments = true;
            if (arg.equals("--keep-trivia")) options.keepTrivia = true;
            if (arg.equals("--allow-unicode")) options.allowUnicodeCharacters = true;
        }
        return options;
    }

}
