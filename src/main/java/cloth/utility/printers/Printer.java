package cloth.utility.printers;

import java.io.PrintStream;

/**
 * Represents a generic printer interface that defines a contract
 * for printing objects of a specified type.
 *
 * @param <T> the type of object that this printer is capable of printing
 */
public interface Printer<T> {

    /**
     * Prints the specified object to the appropriate output stream.
     *
     * @param obj the object to be printed; must not be null
     */
    void print(T obj);

    /**
     * A PrintStream instance that represents the standard error output stream.
     * Typically used to display error messages or diagnostics to the user.
     * This stream outputs characters to the console or to the destination specified
     * by the underlying system configuration for standard error.
     */
    PrintStream error = System.err;
    PrintStream out = System.out;

}
