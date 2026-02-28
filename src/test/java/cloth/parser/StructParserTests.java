package cloth.parser;

import cloth.error.DiagnosticSink;
import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.lexer.LexerOptions;
import cloth.lexer.SourceBuffer;
import cloth.parser.flags.Visibility;
import cloth.parser.statements.StructParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class StructParserTests {

    @TempDir
    Path tempDir;

    private StructParser.StructDeclaration parseStruct(String source) throws IOException {
        Path testFile = tempDir.resolve("test.co");
        Files.writeString(testFile, source);

        SourceFile sourceFile = new SourceFile(testFile.toString());
        SourceBuffer buffer = new SourceBuffer(sourceFile, source);
        DiagnosticSink diagnostics = new DiagnosticSink();
        LexerOptions options = new LexerOptions();

        Lexer lexer = new Lexer(buffer, diagnostics, options);
        return new StructParser(lexer, sourceFile).parse();
    }

    // region Simple Structs

    @Test
    public void testEmptyStruct() throws IOException {
        var result = parseStruct("struct Empty {}");

        assertEquals("Empty", result.name().lexeme());
        assertNull(result.primaryConstructor());
        assertTrue(result.fields().isEmpty());
        assertFalse(result.flags().hasFlags());
    }

    @Test
    public void testStructWithPrimaryParams() throws IOException {
        var result = parseStruct("struct Vec2(x: f32, y: f32) {}");

        assertEquals("Vec2", result.name().lexeme());
        assertNotNull(result.primaryConstructor());
        assertEquals(2, result.primaryConstructor().size());
        assertEquals("x", result.primaryConstructor().get(0).name().lexeme());
        assertEquals("f32", result.primaryConstructor().get(0).type().baseName().lexeme());
        assertEquals("y", result.primaryConstructor().get(1).name().lexeme());
    }

    @Test
    public void testStructWithEmptyParens() throws IOException {
        var result = parseStruct("struct Unit() {}");

        assertNotNull(result.primaryConstructor());
        assertTrue(result.primaryConstructor().isEmpty());
    }

    @Test
    public void testStructSingleParam() throws IOException {
        var result = parseStruct("struct Wrapper(value: i32) {}");

        assertNotNull(result.primaryConstructor());
        assertEquals(1, result.primaryConstructor().size());
        assertEquals("value", result.primaryConstructor().getFirst().name().lexeme());
        assertEquals("i32", result.primaryConstructor().getFirst().type().baseName().lexeme());
    }

    // endregion

    // region Visibility and Modifiers

    @Test
    public void testPublicStruct() throws IOException {
        var result = parseStruct("public struct Point(x: f64, y: f64) {}");

        assertEquals(Visibility.Type.PUBLIC, result.flags().getVisibility());
        assertEquals("Point", result.name().lexeme());
        assertNotNull(result.primaryConstructor());
    }

    @Test
    public void testPrivateStruct() throws IOException {
        var result = parseStruct("private struct Internal {}");

        assertEquals(Visibility.Type.PRIVATE, result.flags().getVisibility());
    }

    @Test
    public void testInternalStruct() throws IOException {
        var result = parseStruct("internal struct Scoped(n: i32) {}");

        assertEquals(Visibility.Type.INTERNAL, result.flags().getVisibility());
    }

    @Test
    public void testFinalStruct() throws IOException {
        var result = parseStruct("final struct Immutable {}");

        assertTrue(result.flags().isFinal());
    }

    // endregion

    // region Body Fields

    @Test
    public void testStructWithFields() throws IOException {
        var result = parseStruct("struct Config { var timeout: i32 = 30; var retries: i32 = 3; }");

        assertEquals(2, result.fields().size());
        assertEquals("timeout", result.fields().get(0).name().lexeme());
        assertEquals("retries", result.fields().get(1).name().lexeme());
    }

    @Test
    public void testStructWithParamsAndFields() throws IOException {
        var result = parseStruct(
            "struct Vec3(x: f32, y: f32, z: f32) { var length: f32 = 0.0; }");

        assertNotNull(result.primaryConstructor());
        assertEquals(3, result.primaryConstructor().size());
        assertEquals(1, result.fields().size());
        assertEquals("length", result.fields().getFirst().name().lexeme());
    }

    @Test
    public void testStructWithModifiedFields() throws IOException {
        var result = parseStruct(
            "struct Counter { static var total: i32 = 0; final var id: i64 = 0; }");

        assertEquals(2, result.fields().size());

        var total = result.fields().get(0);
        assertTrue(total.flags().isStatic());
        assertEquals("total", total.name().lexeme());

        var id = result.fields().get(1);
        assertTrue(id.flags().isFinal());
        assertEquals("id", id.name().lexeme());
    }

    @Test
    public void testStructWithMethodSkipped() throws IOException {
        var result = parseStruct("""
            struct Vec2(x: f32, y: f32) {
                func lengthSquared(): f32 {
                    return x * x + y * y;
                }
            }""");

        assertEquals("Vec2", result.name().lexeme());
        assertNotNull(result.primaryConstructor());
        assertTrue(result.fields().isEmpty());
    }

    // endregion

    // region Nullable and Array Types in Params

    @Test
    public void testStructWithNullableParam() throws IOException {
        var result = parseStruct("struct Maybe(value: i32?) {}");

        assertNotNull(result.primaryConstructor());
        assertTrue(result.primaryConstructor().getFirst().type().nullable());
    }

    @Test
    public void testStructWithArrayParam() throws IOException {
        var result = parseStruct("struct Buffer(data: u8[]) {}");

        assertNotNull(result.primaryConstructor());
        assertEquals(1, result.primaryConstructor().getFirst().type().arrayDepth());
    }

    // endregion

    // region Full Declarations

    @Test
    public void testPublicStructWithParamsAndFields() throws IOException {
        var result = parseStruct(
            "public struct Entity(id: i64, name: string) { var active: bool = true; }");

        assertEquals(Visibility.Type.PUBLIC, result.flags().getVisibility());
        assertEquals("Entity", result.name().lexeme());
        assertNotNull(result.primaryConstructor());
        assertEquals(2, result.primaryConstructor().size());
        assertEquals(1, result.fields().size());
        assertEquals("active", result.fields().getFirst().name().lexeme());
    }

    // endregion

    // region Error Cases

    @Test
    public void testMissingStructName() {
        assertThrows(CompileError.class, () -> parseStruct("struct {}"));
    }

    @Test
    public void testMissingOpenBrace() {
        assertThrows(CompileError.class, () -> parseStruct("struct Foo var x: i32; }"));
    }

    @Test
    public void testAbstractStructRejected() {
        var ex = assertThrows(CompileError.class, () -> parseStruct("abstract struct Foo {}"));
        assertTrue(ex.getMessage().contains("Structs cannot be abstract"));
    }

    @Test
    public void testOverrideStructRejected() {
        var ex = assertThrows(CompileError.class, () -> parseStruct("override struct Foo {}"));
        assertTrue(ex.getMessage().contains("'override' is not valid on a struct"));
    }

    @Test
    public void testStructWithInterfaceRejected() {
        assertThrows(CompileError.class, () -> parseStruct("struct Foo is Printable {}"));
    }

    // endregion
}
