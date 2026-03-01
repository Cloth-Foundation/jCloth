package cloth.args;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Represents a command-line argument flag for parsing arguments. This record encapsulates the
 * characteristics of a flag such as its name, aliases, the number of leading dashes, optional
 * default value, parsing logic, and whether it requires an associated value.
 *
 * @param <T> The type of the value associated with the flag.
 *
 * @param dashCount    The number of leading dashes in the flag representation (e.g., 1 for `-flag`
 *                     or 2 for `--flag`).
 * @param name         The primary identifier/name of the flag.
 * @param aliases      A list of alternative names (aliases) for the flag.
 * @param defaultValue The default value to use if the flag is not provided in the arguments.
 * @param parse        A parser function to convert a string representation into the appropriate type.
 * @param takesValue   A flag indicating whether this argument requires an associated value.
 * @param description  A brief description of the flag's purpose or usage.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public record ArgFlag<T>(int dashCount, String name, List<String> aliases, T defaultValue, Function<String, T> parse, boolean takesValue, String description) {

    /**
     * Constructs a list of tokens representing the flag and its aliases, each prefixed by a
     * specific number of dashes determined by the flag's `dashCount`.
     *
     * @return A list of strings where the first entry corresponds to the primary name of the flag,
     *         and subsequent entries correspond to its aliases, all formatted with the appropriate dashes.
     */
    public List<String> tokens() {
        String prefix = "-".repeat(Math.max(0, dashCount));
        var out = new ArrayList<String>();
        out.add(prefix + name);
        for (String a : aliases) out.add(prefix + a);
        return out;
    }

}
