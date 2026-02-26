package cloth.args;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility class for parsing and managing command-line arguments based on predefined flags.
 * It maps input arguments to corresponding flags and stores their associated values, either
 * default or provided by the user.
 * <p>
 * The ArgParser is designed to work with a set of argument flags (defined as {@code ArgFlags})
 * and applies the parsing logic defined by their respective definitions ({@code ArgFlag}).
 * <p>
 * This class allows for the resolution of argument values at runtime and makes them accessible
 * via their associated flags.
 * <p>
 * This class is immutable after construction.
 */
public final class ArgParser {

   private final Map<String, ArgFlags> tokenToFlag = new HashMap<>();
   private final EnumMap<ArgFlags, Object> values = new EnumMap<>(ArgFlags.class);

   public ArgParser() {
      for (ArgFlags f : ArgFlags.values()) {
         for (String t : f.getDef().tokens()) tokenToFlag.put(t, f);
         values.put(f, f.getDef().defaultValue());
      }
   }

   /**
    * Parses the given array of command-line arguments and maps them to their corresponding flags
    * and values. Flags without associated values are treated as boolean switches, and their
    * presence in the input arguments sets their value to `true`. For flags requiring values, the
    * method extracts and parses the subsequent argument. If a flag requiring a value is missing its
    * value, an exception is thrown.
    *
    * @param args An array of command-line arguments to be parsed. Each element is expected to
    *             represent either a flag (identified by its tokens) or a value associated with a flag.
    *             Flags must match the tokens defined in {@code ArgFlags}.
    *
    * @throws IllegalArgumentException If a flag requiring a value is missing its associated value
    *                                  in the provided arguments.
    */
   public void parse(String[] args) {
      for (int i = 0; i < args.length; i++) {
         String tok = args[i];
         ArgFlags flag = tokenToFlag.get(tok);
         if (flag == null) continue;

         ArgFlag<?> def = flag.getDef();

         if (!def.takesValue()) {
            // boolean switch: presence means true
            values.put(flag, true);
            continue;
         }

         if (i + 1 >= args.length) {
            throw new IllegalArgumentException("Missing value for " + tok);
         }

         String raw = args[++i];
         values.put(flag, parse(def, raw));
      }
   }

   /**
    * Parses a raw string representation of an argument based on the definition provided
    * by the specified {@code ArgFlag}.
    *
    * @param <T>  The type of the value produced by the parsing process.
    * @param def  The {@code ArgFlag} defining the parsing logic, default value, and expected type.
    * @param raw  The raw string input to be parsed into the appropriate type.
    * @return The parsed value of type {@code T} based on the provided {@code ArgFlag}.
    */
   private static <T> T parse(ArgFlag<T> def, String raw) {
      return def.parse().apply(raw);
   }

   /**
    * Retrieves the value associated with the given flag and casts it to the specified type.
    *
    * @param <T>  The type to which the value should be cast.
    * @param flag The flag whose associated value is to be retrieved. It must correspond to a valid
    *             entry within the parsed arguments.
    * @param type The class object representing the type to which the value should be cast.
    * @return The value associated with the specified flag, cast to the desired type.
    * @throws ClassCastException If the value associated with the flag cannot be cast to the specified type.
    * @throws NullPointerException If the specified flag is {@code null} or if the associated value is {@code null}.
    */
   public <T> T get(ArgFlags flag, Class<T> type) {
      Object v = values.get(flag);
      return type.cast(v);
   }

}
