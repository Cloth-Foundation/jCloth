package cloth.error;

import cloth.token.span.SourceSpan;

public interface SpannedDiagnostic {
    SourceSpan getSpan();

    /** text shown next to the underline */
    default String getLabel() { return "here"; }

    /** optional help line */
    default String getHelp() { return null; }
}
