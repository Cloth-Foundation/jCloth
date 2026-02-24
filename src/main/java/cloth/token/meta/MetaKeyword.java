package cloth.token.meta;

import java.util.Locale;

/**
 * Represents meta-programming keywords used in language constructs.
 * Each keyword is an enum constant that defines a specific meta-programming operation
 * or query relevant in the context of the language.
 *
 * MetaKeyword provides operations such as querying the properties of types or data,
 * converting values, or accessing default or boundary values for various types.
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
    DEFAULT, // Default initializer value for a type (e.g. DEFAULT(int) == 0, DEFAULT(bool) == false, etc.)

    // These are for integer types only and can be used to query the max/min values of a type (e.g. LENGTH(int) == 32, MAX(int) == 2147483647, MIN(int) == -2147483648)
    MAX,
    MIN;

    public String getKeyword() {
        return super.name().toUpperCase(Locale.ROOT);
    }

}
