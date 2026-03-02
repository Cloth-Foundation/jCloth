package cloth.error;

import cloth.error.errors.CompileError;
import cloth.token.span.SourceSpan;

/**
 * Factory for constructing a specific {@link CompileError} subtype.
 * <p>
 * Because every error subclass shares the same four-arg constructor
 * ({@code message, span, label, help}), a constructor reference such as
 * {@code SyntaxError::new} or {@code ModifierError::new} satisfies this
 * interface out of the box.
 *
 * @param <E> the concrete error type to build
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
@FunctionalInterface
public interface ErrorBuilder<E extends CompileError> {

    E build(String message, SourceSpan span, String label, String help);

}
