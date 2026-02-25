package cloth.error.type;

import cloth.error.SpannedDiagnostic;
import cloth.token.span.SourceSpan;

public class SpannedCompileError extends CompileError implements SpannedDiagnostic {

    private final SourceSpan span;
    private final String label;
    private final String help;

    public SpannedCompileError(String message, SourceSpan span) {
        this(message, span, "here", null);
    }

    public SpannedCompileError(String message, SourceSpan span, String label, String help) {
        super(message);
        this.span = span;
        this.label = label;
        this.help = help;
    }

    @Override
    public SourceSpan getSpan() {
        return span;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getHelp() {
        return help;
    }
}