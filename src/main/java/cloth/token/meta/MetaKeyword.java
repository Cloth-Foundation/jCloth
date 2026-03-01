package cloth.token.meta;

import java.util.Locale;

/**
 * Represents meta-programming keywords used in language constructs.
 * Each keyword is an enum constant that defines a specific meta-programming operation
 * or query relevant in the context of the language.
 * <p>
 * MetaKeyword provides operations such as querying the properties of types or data,
 * converting values, or accessing default or boundary values for various types.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public enum MetaKeyword {

    NONE,

    LENGTH,
    SIZEOF,
    TO_STRING,
    TYPEOF,
    ALIGNOF,
    MEMSPACE,

    TO_BYTES,
    TO_BITS,
    DEFAULT, // Default initializer value for a type (e.g. i32::DEFAULT == 0, bool::DEFAULT == null, etc.)

    // These are for integer types only and can be used to query the max/min values of a type (e.g. int::LENGTH == 32, i32::MAX == 2147483647, i32::MIN == -2147483648)
    MAX,
    MIN;

    /**
     * Retrieves the keyword associated with the current enum constant.
     * The keyword is the uppercase string representation of the name of the constant,
     * formatted to conform to a constant naming convention specific to meta-programming constructs.
     *
     * @return the uppercase string representation of the enum constant's name.
     */
    public String getKeyword() {
        return super.name().toUpperCase(Locale.ROOT);
    }

}
