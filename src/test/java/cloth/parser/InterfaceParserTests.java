package cloth.parser;

import cloth.error.DiagnosticSink;
import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.lexer.LexerOptions;
import cloth.lexer.SourceBuffer;
import cloth.parser.flags.Visibility;
import cloth.parser.statements.InterfaceParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class InterfaceParserTests {

    @TempDir
    Path tempDir;

    private InterfaceParser.InterfaceDeclaration parseInterface(String source) throws IOException {
        Path testFile = tempDir.resolve("test.co");
        Files.writeString(testFile, source);

        SourceFile sourceFile = new SourceFile(testFile.toString());
        SourceBuffer buffer = new SourceBuffer(sourceFile, source);
        DiagnosticSink diagnostics = new DiagnosticSink();
        LexerOptions options = new LexerOptions();

        Lexer lexer = new Lexer(buffer, diagnostics, options);
        return new InterfaceParser(lexer, sourceFile).parse();
    }

    // region Simple Interfaces

    @Test
    public void testEmptyInterface() throws IOException {
        var result = parseInterface("interface Empty {}");

        assertEquals("Empty", result.name().lexeme());
        assertTrue(result.methods().isEmpty());
        assertFalse(result.flags().hasFlags());
    }

    @Test
    public void testInterfaceWithSingleMethod() throws IOException {
        var result = parseInterface("interface Drawable { func draw(): void; }");

        assertEquals("Drawable", result.name().lexeme());
        assertEquals(1, result.methods().size());

        var method = result.methods().getFirst();
        assertEquals("draw", method.name().lexeme());
        assertTrue(method.parameters().isEmpty());
        assertEquals("void", method.returnType().baseName().lexeme());
    }

    @Test
    public void testInterfaceWithMultipleMethods() throws IOException {
        var result = parseInterface("""
            interface Shape {
                func area(): f64;
                func perimeter(): f64;
                func name(): string;
            }""");

        assertEquals("Shape", result.name().lexeme());
        assertEquals(3, result.methods().size());
        assertEquals("area", result.methods().get(0).name().lexeme());
        assertEquals("perimeter", result.methods().get(1).name().lexeme());
        assertEquals("name", result.methods().get(2).name().lexeme());
    }

    // endregion

    // region Visibility

    @Test
    public void testPublicInterface() throws IOException {
        var result = parseInterface("public interface Serializable { func serialize(): string; }");

        assertEquals(Visibility.Type.PUBLIC, result.flags().getVisibility());
        assertEquals("Serializable", result.name().lexeme());
        assertEquals(1, result.methods().size());
    }

    @Test
    public void testPrivateInterface() throws IOException {
        var result = parseInterface("private interface Internal { func check(): bool; }");

        assertEquals(Visibility.Type.PRIVATE, result.flags().getVisibility());
    }

    @Test
    public void testInternalInterface() throws IOException {
        var result = parseInterface("internal interface Scoped { func scope(): string; }");

        assertEquals(Visibility.Type.INTERNAL, result.flags().getVisibility());
    }

    // endregion

    // region Method Parameters

    @Test
    public void testMethodWithParameters() throws IOException {
        var result = parseInterface("interface Comparator { func compare(a: i32, b: i32): i32; }");

        assertEquals(1, result.methods().size());
        var method = result.methods().getFirst();
        assertEquals("compare", method.name().lexeme());
        assertEquals(2, method.parameters().size());
        assertEquals("a", method.parameters().get(0).name().lexeme());
        assertEquals("i32", method.parameters().get(0).type().baseName().lexeme());
        assertEquals("b", method.parameters().get(1).name().lexeme());
    }

    @Test
    public void testMethodWithNullableReturnType() throws IOException {
        var result = parseInterface("interface Lookup { func find(key: string): i32?; }");

        var method = result.methods().getFirst();
        assertEquals("find", method.name().lexeme());
        assertTrue(method.returnType().nullable());
        assertEquals("i32", method.returnType().baseName().lexeme());
    }

    @Test
    public void testMethodWithArrayReturnType() throws IOException {
        var result = parseInterface("interface Collection { func toArray(): i32[]; }");

        var method = result.methods().getFirst();
        assertEquals("toArray", method.name().lexeme());
        assertEquals(1, method.returnType().arrayDepth());
    }

    @Test
    public void testMethodWithNullableParam() throws IOException {
        var result = parseInterface("interface Handler { func handle(input: string?): void; }");

        var param = result.methods().getFirst().parameters().getFirst();
        assertTrue(param.type().nullable());
    }

    @Test
    public void testMethodWithArrayParam() throws IOException {
        var result = parseInterface("interface Processor { func process(data: u8[]): void; }");

        var param = result.methods().getFirst().parameters().getFirst();
        assertEquals(1, param.type().arrayDepth());
    }

    @Test
    public void testMethodWithDefaultParam() throws IOException {
        var result = parseInterface("interface Logger { func log(level: i32 = 0): void; }");

        var param = result.methods().getFirst().parameters().getFirst();
        assertNotNull(param.defaultValue());
    }

    @Test
    public void testMethodWithNoParameters() throws IOException {
        var result = parseInterface("interface Runnable { func run(): void; }");

        var method = result.methods().getFirst();
        assertEquals("run", method.name().lexeme());
        assertTrue(method.parameters().isEmpty());
        assertEquals("void", method.returnType().baseName().lexeme());
    }

    // endregion

    // region Complex Interfaces

    @Test
    public void testInterfaceMultipleMethodsWithParams() throws IOException {
        var result = parseInterface("""
            public interface Repository {
                func findById(id: i64): string?;
                func save(entity: string): void;
                func remove(id: i64): bool;
                func count(): i64;
            }""");

        assertEquals(Visibility.Type.PUBLIC, result.flags().getVisibility());
        assertEquals("Repository", result.name().lexeme());
        assertEquals(4, result.methods().size());

        assertEquals("findById", result.methods().get(0).name().lexeme());
        assertTrue(result.methods().get(0).returnType().nullable());

        assertEquals("save", result.methods().get(1).name().lexeme());
        assertEquals("void", result.methods().get(1).returnType().baseName().lexeme());

        assertEquals("remove", result.methods().get(2).name().lexeme());
        assertEquals("bool", result.methods().get(2).returnType().baseName().lexeme());

        assertEquals("count", result.methods().get(3).name().lexeme());
        assertEquals("i64", result.methods().get(3).returnType().baseName().lexeme());
    }

    @Test
    public void testInterfaceMethodWithMultipleArrayParams() throws IOException {
        var result = parseInterface(
            "interface Merger { func merge(left: i32[], right: i32[]): i32[]; }");

        var method = result.methods().getFirst();
        assertEquals(2, method.parameters().size());
        assertEquals(1, method.parameters().get(0).type().arrayDepth());
        assertEquals(1, method.parameters().get(1).type().arrayDepth());
        assertEquals(1, method.returnType().arrayDepth());
    }

    // endregion

    // region Rejected Modifiers

    @Test
    public void testStaticInterfaceRejected() {
        var ex = assertThrows(CompileError.class, () ->
            parseInterface("static interface Foo {}"));
        assertTrue(ex.getMessage().contains("'static' is not valid on an interface"));
    }

    @Test
    public void testFinalInterfaceRejected() {
        var ex = assertThrows(CompileError.class, () ->
            parseInterface("final interface Foo {}"));
        assertTrue(ex.getMessage().contains("'final' is not valid on an interface"));
    }

    @Test
    public void testAbstractInterfaceRejected() {
        var ex = assertThrows(CompileError.class, () ->
            parseInterface("abstract interface Foo {}"));
        assertTrue(ex.getMessage().contains("'abstract' is not valid on an interface"));
    }

    @Test
    public void testOverrideInterfaceRejected() {
        var ex = assertThrows(CompileError.class, () ->
            parseInterface("override interface Foo {}"));
        assertTrue(ex.getMessage().contains("'override' is not valid on an interface"));
    }

    // endregion

    // region Invalid Members

    @Test
    public void testFieldInInterfaceRejected() {
        assertThrows(CompileError.class, () ->
            parseInterface("interface Bad { var x: i32 = 0; }"));
    }

    @Test
    public void testLetFieldInInterfaceRejected() {
        assertThrows(CompileError.class, () ->
            parseInterface("interface Bad { let x: i32 = 0; }"));
    }

    @Test
    public void testConstFieldInInterfaceRejected() {
        assertThrows(CompileError.class, () ->
            parseInterface("interface Bad { const x: i32 = 0; }"));
    }

    @Test
    public void testMethodWithBodyRejected() {
        assertThrows(CompileError.class, () ->
            parseInterface("interface Bad { func doStuff(): void { } }"));
    }

    // endregion

    // region Syntax Errors

    @Test
    public void testMissingInterfaceName() {
        assertThrows(CompileError.class, () ->
            parseInterface("interface {}"));
    }

    @Test
    public void testMissingOpenBrace() {
        assertThrows(CompileError.class, () ->
            parseInterface("interface Foo func bar(): void; }"));
    }

    @Test
    public void testMissingSemicolonAfterSignature() {
        assertThrows(CompileError.class, () ->
            parseInterface("interface Foo { func bar(): void }"));
    }

    @Test
    public void testMissingReturnType() {
        assertThrows(CompileError.class, () ->
            parseInterface("interface Foo { func bar(); }"));
    }

    // endregion
}
