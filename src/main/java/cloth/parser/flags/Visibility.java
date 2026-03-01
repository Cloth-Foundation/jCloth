package cloth.parser.flags;

import cloth.token.Token;

/**
 * Represents the visibility specifications of a programming construct, encapsulating its type
 * and its associated metadata as a token.
 * <p>
 * The class is designed to capture the visibility details like public, private, or internal,
 * along with additional context encapsulated within a {@link Token} instance.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public record Visibility(Type type, Token token) {

    public enum Type {
        PUBLIC,
        PRIVATE,
        INTERNAL
    }

}
