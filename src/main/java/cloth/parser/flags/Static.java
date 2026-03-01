package cloth.parser.flags;

import cloth.token.Token;

/**
 * Represents a static declaration in a programming context, encapsulating its type
 * and associated metadata.
 * <p>
 * The class is designed to handle information about static constructs in the source
 * code, where "static" refers to entities that are associated with the class rather
 * than an instance. Examples include static methods, fields, or imports.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public record Static(Type type, Token token) {

    public enum Type {
        STATIC,
        NOT_STATIC
    }

}
