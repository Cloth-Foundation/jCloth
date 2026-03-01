package cloth.args;

import lombok.Getter;

import java.util.List;

/**
 * Represents a set of predefined command-line argument flags used for parsing and handling
 * arguments in the application. Each flag defines its structure, including the number of
 * leading dashes, a primary identifier, aliases, a default value, and a description of its usage.
 * <p>
 * The enum is designed to standardize and simplify argument handling logic by providing a central
 * definition of supported flags and their properties.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public enum ArgFlags {

    // This is mostly only for file linking for errors.
    // JetBrains requires a file path to state its full qualified path (e.g. C:/my/file/path/myFile.co).
    // This does not seem to be an issue in Visual Studio IDEs. This flag forces error printing
    // to print in the JetBrains style.
    JETBRAINS_TERMINAL(Flags.JETBRAINS_TERMINAL),
    FILE(Flags.FILE);

    @Getter
    private final ArgFlag<?> def;

    ArgFlags(ArgFlag<?> def) {
        this.def = def;
    }

    public static final class Flags {

        public static final ArgFlag<Boolean> JETBRAINS_TERMINAL = new ArgFlag<>(2, "jetbrains", List.of("jb"), false, Boolean::parseBoolean, false, "Use JetBrains Terminal");
        public static final ArgFlag<String> FILE = new ArgFlag<>(1, "file", List.of("f"), "", s -> s, true, "Specify the file to compile");

    }

}
