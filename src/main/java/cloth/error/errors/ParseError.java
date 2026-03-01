package cloth.error.errors;

import cloth.error.ErrorType;
import cloth.error.Error;
import cloth.token.span.SourceSpan;

/**
 * Represents a specific type of error that occurs during the parsing phase of the program.
 * This error is used to indicate issues related to the syntax or structure of the source code
 * that prevent successful parsing. Instances of this class provide details about the nature,
 * location, and resolution of parsing errors encountered during this phase.
 * <p>
 * This class extends {@link java.lang.Error}, inheriting properties like the error message, source span,
 * descriptive label, and resolution help. Additionally, the type of this error is always
 * {@link ErrorType#PARSE_ERROR}.
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public class ParseError extends Error {

    public ParseError(String message, SourceSpan span, String label, String help) {
        super(message, span, label, help);
    }

    @Override
    public ErrorType getType() {
        return ErrorType.PARSE_ERROR;
    }

}
