package cloth.lexer.trivia;

import java.util.ArrayList;

/**
 * Represents a collection of leading and trailing trivia associated with a token
 * during the lexical analysis process. Trivia is non-essential contextual information,
 * such as whitespace or comments, that surround tokens in a source file.
 * <p>
 * This class provides a structured way to manage and manipulate the leading and
 * trailing trivia pieces, which can include operations such as clearing all trivia.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public record Trivia(ArrayList<TriviaPiece> leading, ArrayList<TriviaPiece> trailing) {

    public Trivia() {
        this(new ArrayList<>(), new ArrayList<>());
    }

    public void clear() {
        leading.clear();
        trailing.clear();
    }

}
