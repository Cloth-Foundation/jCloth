package cloth.parser.flags;

import cloth.token.Token;
import lombok.Getter;

/**
 * Represents the visibility specifications of a programming construct, encapsulating its type
 * and its associated metadata as a token.
 * <p>
 * The class is designed to capture the visibility details like public, private, or internal,
 * along with additional context encapsulated within a {@link Token} instance.
 * <p>
 * Components:
 * - {@link Type}: Enum representing the visibility type.
 * - {@link Token}: Represents the token in the source code that is associated with the visibility construct.
 * <p>
 * Features:
 * This class is immutable and exposes its data via the getter methods for {@link #getType()}
 * and {@link #getToken()}.
 */
public class Visibility {

    @Getter
    private final Type type;

    @Getter
    private final Token token;

    public Visibility(Type type, Token token) {
        this.type = type;
        this.token = token;
    }

    public enum Type {
        PUBLIC,
        PRIVATE,
        INTERNAL
    }

}
