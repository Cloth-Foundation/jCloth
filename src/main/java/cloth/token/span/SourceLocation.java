package cloth.token.span;

import cloth.file.SourceFile;
import lombok.Getter;

public class SourceLocation {

    @Getter
    private final SourceFile file;

    @Getter
    private final int offset;

    @Getter
    private final int line;

    @Getter
    private final int column;

    public SourceLocation(SourceFile file, int offset, int line, int column) {
        this.file = file;
        this.offset = offset;
        this.line = line;
        this.column = column;
    }

}
