package cloth.parser;

/**
 * Represents a contract for parsing an object of type {@code T}.
 * <p>
 * Implementations of this interface should provide parsing logic
 * to convert input data into an instance of type {@code T}.
 *
 * @param <T> The type of object that will be produced as a result of parsing.
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public interface Parsable<T> {

    /**
     * Parses and returns an instance of type {@code T}.
     * The implementation defines the logic for interpreting input data
     * and converting it into the appropriate object representation
     * of the specified type.
     *
     * @return An instance of type {@code T} resulting from the parsing process.
     */
    T parse();

}
