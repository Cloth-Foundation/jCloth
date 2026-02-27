package cloth.parser.flags;

import cloth.token.IToken;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class DeclarationFlags {

    @Nullable private Visibility.Type visibility;
    @Nullable private IToken visibilityToken;

    private boolean isStatic;
    @Nullable private IToken staticToken;

    private boolean isFinal;
    @Nullable private IToken finalToken;

    private boolean isAbstract;
    @Nullable private IToken abstractToken;

    private boolean isOverride;
    @Nullable private IToken overrideToken;

    public boolean hasFlags() {
        return visibility != null || isStatic || isFinal || isAbstract || isOverride;
    }

    /**
     * Returns the first flag token in source order, or null if no flags were set.
     * Useful for computing the span of a declaration that includes leading modifiers.
     */
    public @Nullable IToken firstToken() {
        IToken first = null;
        for (IToken t : new IToken[]{visibilityToken, staticToken, finalToken, abstractToken, overrideToken}) {
            if (t != null && (first == null || t.span().start().offset() < first.span().start().offset())) {
                first = t;
            }
        }
        return first;
    }
}
