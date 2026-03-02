package cloth.error;

import lombok.Getter;

/**
 * Represents the different types of errors that may occur in the program.
 * This enumeration is used to categorize errors based on their nature or
 * the phase of execution in which they are encountered.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public enum ErrorType {

    COMPILE_ERROR("Compile Error"),
    PARSE_ERROR("Parse Error"),
    RUNTIME_ERROR("Runtime Error"),
    SYNTAX_ERROR("Syntax Error"),
    MODIFIER_ERROR("Modifier Error"),
    DECLARATION_ERROR("Declaration Error"),
    ;

    @Getter
    private final String displayName;

    ErrorType(String displayName) {
        this.displayName = displayName;
    }

}
