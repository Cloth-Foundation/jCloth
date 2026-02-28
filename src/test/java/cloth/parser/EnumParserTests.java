package cloth.parser;

import cloth.error.DiagnosticSink;
import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.lexer.LexerOptions;
import cloth.lexer.SourceBuffer;
import cloth.parser.expressions.Expression;
import cloth.parser.flags.Visibility;
import cloth.parser.statements.EnumParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class EnumParserTests {

    @TempDir
    Path tempDir;

    private EnumParser.EnumDeclaration parseEnum(String source) throws IOException {
        Path testFile = tempDir.resolve("test.co");
        Files.writeString(testFile, source);

        SourceFile sourceFile = new SourceFile(testFile.toString());
        SourceBuffer buffer = new SourceBuffer(sourceFile, source);
        DiagnosticSink diagnostics = new DiagnosticSink();
        LexerOptions options = new LexerOptions();

        Lexer lexer = new Lexer(buffer, diagnostics, options);
        return new EnumParser(lexer, sourceFile).parse();
    }

    // region Simple Enums

    @Test
    public void testSimpleEnum() throws IOException {
        var result = parseEnum("enum Color { Red, Green, Blue }");

        assertEquals("Color", result.name().lexeme());
        assertNull(result.primaryConstructor());
        assertEquals(3, result.cases().size());
        assertEquals("Red", result.cases().get(0).name().lexeme());
        assertEquals("Green", result.cases().get(1).name().lexeme());
        assertEquals("Blue", result.cases().get(2).name().lexeme());
        assertFalse(result.flags().hasFlags());
        assertTrue(result.fields().isEmpty());
    }

    @Test
    public void testEmptyEnum() throws IOException {
        var result = parseEnum("enum Empty {}");

        assertEquals("Empty", result.name().lexeme());
        assertTrue(result.cases().isEmpty());
        assertTrue(result.fields().isEmpty());
    }

    @Test
    public void testSingleCaseEnum() throws IOException {
        var result = parseEnum("enum Singleton { Only }");

        assertEquals(1, result.cases().size());
        assertEquals("Only", result.cases().getFirst().name().lexeme());
    }

    @Test
    public void testTrailingCommaEnum() throws IOException {
        var result = parseEnum("enum Direction { North, South, East, West, }");

        assertEquals(4, result.cases().size());
        assertEquals("West", result.cases().get(3).name().lexeme());
    }

    // endregion

    // region Visibility and Modifiers

    @Test
    public void testPublicEnum() throws IOException {
        var result = parseEnum("public enum Status { Active, Inactive }");

        assertEquals(Visibility.Type.PUBLIC, result.flags().getVisibility());
        assertEquals("Status", result.name().lexeme());
        assertEquals(2, result.cases().size());
    }

    @Test
    public void testPrivateEnum() throws IOException {
        var result = parseEnum("private enum Internal { On, Off }");

        assertEquals(Visibility.Type.PRIVATE, result.flags().getVisibility());
    }

    @Test
    public void testInternalEnum() throws IOException {
        var result = parseEnum("internal enum Scope { Local, Global }");

        assertEquals(Visibility.Type.INTERNAL, result.flags().getVisibility());
    }

    // endregion

    // region Data Cases (Payloads)

    @Test
    public void testDataCase() throws IOException {
        var result = parseEnum("enum Shape { Circle(radius: f64), Rectangle(w: f64, h: f64) }");

        assertEquals(2, result.cases().size());

        var circle = result.cases().getFirst();
        assertEquals("Circle", circle.name().lexeme());
        assertNotNull(circle.payload());
        assertEquals(1, circle.payload().size());
        assertEquals("radius", circle.payload().getFirst().name().lexeme());
        assertEquals("f64", circle.payload().getFirst().type().baseName().lexeme());

        var rect = result.cases().get(1);
        assertEquals("Rectangle", rect.name().lexeme());
        assertNotNull(rect.payload());
        assertEquals(2, rect.payload().size());
        assertEquals("w", rect.payload().get(0).name().lexeme());
        assertEquals("h", rect.payload().get(1).name().lexeme());
    }

    @Test
    public void testMixedUnitAndDataCases() throws IOException {
        var result = parseEnum("enum Token { Eof, Number(value: i32), Ident(name: string) }");

        assertEquals(3, result.cases().size());

        assertNull(result.cases().get(0).payload());
        assertNotNull(result.cases().get(1).payload());
        assertEquals(1, result.cases().get(1).payload().size());
        assertNotNull(result.cases().get(2).payload());
        assertEquals(1, result.cases().get(2).payload().size());
    }

    @Test
    public void testDataCaseWithNullableType() throws IOException {
        var result = parseEnum("enum Maybe { None, Some(value: i32?) }");

        var someCase = result.cases().get(1);
        assertNotNull(someCase.payload());
        assertTrue(someCase.payload().getFirst().type().nullable());
    }

    @Test
    public void testDataCaseWithArrayType() throws IOException {
        var result = parseEnum("enum Container { Empty, Items(data: i32[]) }");

        var items = result.cases().get(1);
        assertNotNull(items.payload());
        assertEquals(1, items.payload().getFirst().type().arrayDepth());
    }

    // endregion

    // region Explicit Discriminants

    @Test
    public void testExplicitDiscriminant() throws IOException {
        var result = parseEnum("enum HttpStatus { Ok = 200, NotFound = 404, ServerError = 500 }");

        assertEquals(3, result.cases().size());

        var ok = result.cases().getFirst();
        assertEquals("Ok", ok.name().lexeme());
        assertNotNull(ok.discriminant());
        assertEquals("200", ((Expression.Literal) ok.discriminant()).value().lexeme());

        var notFound = result.cases().get(1);
        assertNotNull(notFound.discriminant());
        assertEquals("404", ((Expression.Literal) notFound.discriminant()).value().lexeme());

        var serverError = result.cases().get(2);
        assertNotNull(serverError.discriminant());
        assertEquals("500", ((Expression.Literal) serverError.discriminant()).value().lexeme());
    }

    @Test
    public void testMixedDiscriminants() throws IOException {
        var result = parseEnum("enum Level { Low, Medium = 5, High }");

        assertNull(result.cases().get(0).discriminant());
        assertNotNull(result.cases().get(1).discriminant());
        assertEquals("5", ((Expression.Literal) result.cases().get(1).discriminant()).value().lexeme());
        assertNull(result.cases().get(2).discriminant());
    }

    @Test
    public void testDataCaseWithDiscriminant() throws IOException {
        var result = parseEnum("enum Http { Ok(body: string) = 200, Error(msg: string) = 500 }");

        var ok = result.cases().getFirst();
        assertEquals("Ok", ok.name().lexeme());
        assertNotNull(ok.payload());
        assertEquals(1, ok.payload().size());
        assertNotNull(ok.discriminant());
        assertEquals("200", ((Expression.Literal) ok.discriminant()).value().lexeme());

        var error = result.cases().get(1);
        assertNotNull(error.payload());
        assertNotNull(error.discriminant());
        assertEquals("500", ((Expression.Literal) error.discriminant()).value().lexeme());
    }

    // endregion

    // region Members After Semicolon

    @Test
    public void testEnumWithFieldMembers() throws IOException {
        var result = parseEnum(
            "enum Color { Red, Green, Blue; static final var count: i32 = 3; }");

        assertEquals(3, result.cases().size());
        assertEquals(1, result.fields().size());

        var field = result.fields().getFirst();
        assertEquals("count", field.name().lexeme());
        assertTrue(field.flags().isStatic());
        assertTrue(field.flags().isFinal());
    }

    @Test
    public void testEnumWithMethodSkipped() throws IOException {
        var result = parseEnum("""
            enum Color {
                Red, Green, Blue;
                func isPrimary(): bool {
                    return true;
                }
            }""");

        assertEquals(3, result.cases().size());
        assertTrue(result.fields().isEmpty());
    }

    @Test
    public void testEnumNoCasesWithMembers() throws IOException {
        var result = parseEnum("enum Constants { ; static var total: i32 = 0; }");

        assertTrue(result.cases().isEmpty());
        assertEquals(1, result.fields().size());
    }

    // endregion

    // region Unit Cases Have No Payload

    @Test
    public void testUnitCasesHaveNoPayloadOrDiscriminant() throws IOException {
        var result = parseEnum("enum Bool { True, False }");

        for (var c : result.cases()) {
            assertNull(c.payload());
            assertNull(c.discriminant());
        }
    }

    // endregion

    // region Primary Constructor

    @Test
    public void testPrimaryConstructorSingleParam() throws IOException {
        var result = parseEnum("enum Color(hex: string) { Red(\"#FF0000\"), Green(\"#00FF00\"), Blue(\"#0000FF\") }");

        assertEquals("Color", result.name().lexeme());
        assertNotNull(result.primaryConstructor());
        assertEquals(1, result.primaryConstructor().size());
        assertEquals("hex", result.primaryConstructor().getFirst().name().lexeme());
        assertEquals("string", result.primaryConstructor().getFirst().type().baseName().lexeme());

        assertEquals(3, result.cases().size());
        for (var c : result.cases()) {
            assertNull(c.payload());
            assertNotNull(c.constructorArgs());
            assertEquals(1, c.constructorArgs().size());
        }

        assertEquals("\"#FF0000\"", ((Expression.Literal) result.cases().get(0).constructorArgs().getFirst()).value().lexeme());
        assertEquals("\"#00FF00\"", ((Expression.Literal) result.cases().get(1).constructorArgs().getFirst()).value().lexeme());
        assertEquals("\"#0000FF\"", ((Expression.Literal) result.cases().get(2).constructorArgs().getFirst()).value().lexeme());
    }

    @Test
    public void testPrimaryConstructorMultipleParams() throws IOException {
        var result = parseEnum(
            "enum Planet(mass: f64, radius: f64) { Earth(5.976, 6.378), Mars(6.421, 3.390) }");

        assertNotNull(result.primaryConstructor());
        assertEquals(2, result.primaryConstructor().size());
        assertEquals("mass", result.primaryConstructor().get(0).name().lexeme());
        assertEquals("radius", result.primaryConstructor().get(1).name().lexeme());

        assertEquals(2, result.cases().size());

        var earth = result.cases().getFirst();
        assertEquals("Earth", earth.name().lexeme());
        assertNotNull(earth.constructorArgs());
        assertEquals(2, earth.constructorArgs().size());
        assertEquals("5.976", ((Expression.Literal) earth.constructorArgs().get(0)).value().lexeme());
        assertEquals("6.378", ((Expression.Literal) earth.constructorArgs().get(1)).value().lexeme());
    }

    @Test
    public void testPrimaryConstructorWithDiscriminant() throws IOException {
        var result = parseEnum(
            "enum Status(label: string) { Ok(\"ok\") = 200, Error(\"error\") = 500 }");

        assertNotNull(result.primaryConstructor());
        assertEquals(1, result.primaryConstructor().size());

        var ok = result.cases().getFirst();
        assertNotNull(ok.constructorArgs());
        assertEquals(1, ok.constructorArgs().size());
        assertNotNull(ok.discriminant());
        assertEquals("200", ((Expression.Literal) ok.discriminant()).value().lexeme());

        var error = result.cases().get(1);
        assertNotNull(error.constructorArgs());
        assertNotNull(error.discriminant());
        assertEquals("500", ((Expression.Literal) error.discriminant()).value().lexeme());
    }

    @Test
    public void testPrimaryConstructorWithMembers() throws IOException {
        var result = parseEnum(
            "enum Color(hex: string) { Red(\"#FF0000\"), Blue(\"#0000FF\"); static var count: i32 = 2; }");

        assertNotNull(result.primaryConstructor());
        assertEquals(2, result.cases().size());
        assertEquals(1, result.fields().size());
        assertEquals("count", result.fields().getFirst().name().lexeme());
    }

    @Test
    public void testPrimaryConstructorEmptyCases() throws IOException {
        var result = parseEnum("enum Tag(id: i32) {}");

        assertNotNull(result.primaryConstructor());
        assertEquals(1, result.primaryConstructor().size());
        assertTrue(result.cases().isEmpty());
    }

    @Test
    public void testPrimaryConstructorWithExpressionArg() throws IOException {
        var result = parseEnum("enum Offset(val: i32) { A(1 + 2), B(3 * 4) }");

        assertNotNull(result.primaryConstructor());

        var a = result.cases().getFirst();
        assertNotNull(a.constructorArgs());
        assertEquals(1, a.constructorArgs().size());
        assertInstanceOf(Expression.Binary.class, a.constructorArgs().getFirst());

        var b = result.cases().get(1);
        assertNotNull(b.constructorArgs());
        assertInstanceOf(Expression.Binary.class, b.constructorArgs().getFirst());
    }

    @Test
    public void testNoPrimaryConstructorCasesHaveNoArgs() throws IOException {
        var result = parseEnum("enum Direction { North, South }");

        assertNull(result.primaryConstructor());
        for (var c : result.cases()) {
            assertNull(c.constructorArgs());
        }
    }

    @Test
    public void testPublicEnumWithPrimaryConstructor() throws IOException {
        var result = parseEnum("public enum Level(rank: i32) { Low(1), High(10) }");

        assertEquals(Visibility.Type.PUBLIC, result.flags().getVisibility());
        assertNotNull(result.primaryConstructor());
        assertEquals(2, result.cases().size());
    }

    // endregion

    // region Error Cases

    @Test
    public void testMissingEnumName() {
        assertThrows(CompileError.class, () -> parseEnum("enum { Red }"));
    }

    @Test
    public void testMissingOpenBrace() {
        assertThrows(CompileError.class, () -> parseEnum("enum Color Red, Green }"));
    }

    // endregion
}
