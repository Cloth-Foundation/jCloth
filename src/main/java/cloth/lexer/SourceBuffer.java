package cloth.lexer;

import cloth.file.SourceFile;
import lombok.Getter;

/**
 * The SourceBuffer represent a tokens position in a given source file.
 * The input {@link SourceFile} is provided along with the text of the file.
 * This is useful for providing contextual information to the lexer.
 * <p/>
 * An object representation of the Source can be represented by the {@link SourceBuffer}
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 * @see SourceFile
 */
public class SourceBuffer {

    @Getter
    private final SourceFile file;

    @Getter
    private final String text;

    @Getter
    private final String fileName;

    public SourceBuffer(final SourceFile file, final String text) {
        this.file = file;
        this.text = text;
        this.fileName = null;
    }

}
