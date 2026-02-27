package cloth.parser.flags;

import cloth.token.Token;
import lombok.Getter;

/**
 * Represents a static declaration in a programming context, encapsulating its type
 * and associated metadata.
 * <p>
 * The class is designed to handle information about static constructs in the source
 * code, where "static" refers to entities that are associated with the class rather
 * than an instance. Examples include static methods, fields, or imports.
 * <p>
 * Components:
 * - {@link Type}: Enum representing whether a construct is static or not.
 * - {@link Token}: Represents the token in the source code associated with the static construct.
 * <p>
 * Usage:
 * This class is immutable and provides its data via getter methods. Use the {@link #getType()}
 * and {@link #getToken()} methods to access the associated type and token respectively.
 */
public class Static {

    @Getter
    private final Type type;

    @Getter
    private final Token token;

    public Static(Type type, Token token) {
        this.type = type;
        this.token = token;
    }

    public enum Type {
        STATIC,
        NOT_STATIC
    }

}
