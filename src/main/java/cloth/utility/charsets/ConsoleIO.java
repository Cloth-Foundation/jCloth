package cloth.utility.charsets;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for configuring console output streams to use UTF-8 encoding.
 * <p>
 * This class provides functionality to ensure that the standard output and standard error
 * streams are set to use UTF-8 encoding, which is commonly necessary for correct handling
 * of special characters and international text in console applications.
 * <p>
 * This class is not intended to be instantiated.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public final class ConsoleIO {

    /**
     * Private constructor to prevent instantiation of the utility class.
     * <p>
     * This constructor is intentionally declared private to enforce the
     * non-instantiable nature of the {@code ConsoleIO} class. As a utility class,
     * {@code ConsoleIO} is designed to provide static methods only and does not
     * support object creation.
     */
    private ConsoleIO() {
    }

    /**
     * Redirects the standard output and standard error streams to use UTF-8 encoding.
     * <p>
     * This method ensures that the console's output and error streams are set to use UTF-8,
     * which can prevent encoding issues with special characters and international text
     * in console-based applications.
     * <p>
     * It updates {@code System.out} and {@code System.err} to new PrintStream instances
     * that are configured to write in UTF-8 encoding.
     */
    public static void forceUtf8() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
    }
}
