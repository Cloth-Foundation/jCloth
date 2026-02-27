package cloth.parser;

import cloth.error.DiagnosticSink;
import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.lexer.LexerOptions;
import cloth.lexer.SourceBuffer;
import cloth.parser.flags.Visibility;
import cloth.parser.statements.ClassParser;
import cloth.parser.statements.ParameterListParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ParserClassTests {

    @TempDir
    Path tempDir;

    private ClassParser.ClassDeclaration parseClass(String source) throws IOException {
        Path testFile = tempDir.resolve("test.co");
        Files.writeString(testFile, source);

        SourceFile sourceFile = new SourceFile(testFile.toString());
        SourceBuffer buffer = new SourceBuffer(sourceFile, source);
        DiagnosticSink diagnostics = new DiagnosticSink();
        LexerOptions options = new LexerOptions();

        Lexer lexer = new Lexer(buffer, diagnostics, options);
        return new ClassParser(lexer, sourceFile).parse();
    }

    private List<ParameterListParser.Parameter> parseParams(String source) throws IOException {
        Path testFile = tempDir.resolve("test.co");
        Files.writeString(testFile, source);

        SourceFile sourceFile = new SourceFile(testFile.toString());
        SourceBuffer buffer = new SourceBuffer(sourceFile, source);
        DiagnosticSink diagnostics = new DiagnosticSink();
        LexerOptions options = new LexerOptions();

        Lexer lexer = new Lexer(buffer, diagnostics, options);
        return new ParameterListParser(lexer, sourceFile).parse();
    }

    // region Simple Class Declarations

    @Test
    public void testSimpleClass() throws IOException {
        var result = parseClass("class Foo {}");

        assertEquals("Foo", result.name().lexeme());
        assertNull(result.primaryConstructor());
        assertNull(result.baseClass());
        assertTrue(result.interfaces().isEmpty());
        assertFalse(result.flags().hasFlags());
    }

    @Test
    public void testClassWithEmptyConstructor() throws IOException {
        var result = parseClass("class Foo() {}");

        assertEquals("Foo", result.name().lexeme());
        assertNotNull(result.primaryConstructor());
        assertTrue(result.primaryConstructor().isEmpty());
    }

    // endregion

    // region Primary Constructors (via ClassParser)

    @Test
    public void testClassWithSingleParam() throws IOException {
        var result = parseClass("class Animal(food: string) {}");

        assertEquals("Animal", result.name().lexeme());
        assertNotNull(result.primaryConstructor());
        assertEquals(1, result.primaryConstructor().size());

        var param = result.primaryConstructor().getFirst();
        assertEquals("food", param.name().lexeme());
        assertEquals("string", param.type().baseName().lexeme());
        assertFalse(param.type().nullable());
        assertEquals(0, param.type().arrayDepth());
        assertNull(param.defaultValue());
    }

    @Test
    public void testClassWithMultipleParams() throws IOException {
        var result = parseClass("class Point(x: i32, y: i32) {}");

        assertEquals(2, result.primaryConstructor().size());
        assertEquals("x", result.primaryConstructor().get(0).name().lexeme());
        assertEquals("i32", result.primaryConstructor().get(0).type().baseName().lexeme());
        assertEquals("y", result.primaryConstructor().get(1).name().lexeme());
        assertEquals("i32", result.primaryConstructor().get(1).type().baseName().lexeme());
    }

    // endregion

    // region ParameterListParser — Type References

    @Test
    public void testSimpleType() throws IOException {
        var params = parseParams("(x: i32)");

        assertEquals(1, params.size());
        assertEquals("x", params.getFirst().name().lexeme());
        assertEquals("i32", params.getFirst().type().baseName().lexeme());
        assertFalse(params.getFirst().type().nullable());
        assertEquals(0, params.getFirst().type().arrayDepth());
    }

    @Test
    public void testNullableType() throws IOException {
        var params = parseParams("(name: string?)");

        assertEquals(1, params.size());
        assertEquals("string", params.getFirst().type().baseName().lexeme());
        assertTrue(params.getFirst().type().nullable());
        assertEquals(0, params.getFirst().type().arrayDepth());
    }

    @Test
    public void testArrayType() throws IOException {
        var params = parseParams("(args: string[])");

        assertEquals(1, params.size());
        assertEquals("string", params.getFirst().type().baseName().lexeme());
        assertFalse(params.getFirst().type().nullable());
        assertEquals(1, params.getFirst().type().arrayDepth());
    }

    @Test
    public void testMultiDimensionalArrayType() throws IOException {
        var params = parseParams("(grid: i32[][])");

        assertEquals(1, params.size());
        assertEquals("i32", params.getFirst().type().baseName().lexeme());
        assertEquals(2, params.getFirst().type().arrayDepth());
    }

    @Test
    public void testNullableArrayType() throws IOException {
        var params = parseParams("(items: string?[])");

        assertEquals(1, params.size());
        assertEquals("string", params.getFirst().type().baseName().lexeme());
        assertTrue(params.getFirst().type().nullable());
        assertEquals(1, params.getFirst().type().arrayDepth());
    }

    @Test
    public void testArrayOfNullableWithTrailingQuestion() throws IOException {
        var params = parseParams("(items: string[]?)");

        assertEquals(1, params.size());
        assertEquals("string", params.getFirst().type().baseName().lexeme());
        assertTrue(params.getFirst().type().nullable());
        assertEquals(1, params.getFirst().type().arrayDepth());
    }

    @Test
    public void testUserDefinedType() throws IOException {
        var params = parseParams("(pet: Animal)");

        assertEquals(1, params.size());
        assertEquals("Animal", params.getFirst().type().baseName().lexeme());
        assertFalse(params.getFirst().type().nullable());
    }

    // endregion

    // region ParameterListParser — Default Values

    @Test
    public void testDefaultStringValue() throws IOException {
        var params = parseParams("(name: string = \"Unnamed\")");

        assertEquals(1, params.size());
        assertEquals("name", params.getFirst().name().lexeme());
        assertEquals("string", params.getFirst().type().baseName().lexeme());
        assertNotNull(params.getFirst().defaultValue());
        assertEquals(1, params.getFirst().defaultValue().size());
        assertEquals("\"Unnamed\"", params.getFirst().defaultValue().getFirst().lexeme());
    }

    @Test
    public void testDefaultNumericValue() throws IOException {
        var params = parseParams("(age: i32 = 0)");

        assertEquals(1, params.size());
        assertNotNull(params.getFirst().defaultValue());
        assertEquals(1, params.getFirst().defaultValue().size());
        assertEquals("0", params.getFirst().defaultValue().getFirst().lexeme());
    }

    @Test
    public void testDefaultNullValue() throws IOException {
        var params = parseParams("(name: string? = null)");

        assertEquals(1, params.size());
        assertTrue(params.getFirst().type().nullable());
        assertNotNull(params.getFirst().defaultValue());
        assertEquals("null", params.getFirst().defaultValue().getFirst().lexeme());
    }

    @Test
    public void testMixedDefaultAndRequired() throws IOException {
        var params = parseParams("(name: string, age: i32 = 0)");

        assertEquals(2, params.size());

        assertNull(params.get(0).defaultValue());
        assertEquals("name", params.get(0).name().lexeme());

        assertNotNull(params.get(1).defaultValue());
        assertEquals("age", params.get(1).name().lexeme());
        assertEquals("0", params.get(1).defaultValue().getFirst().lexeme());
    }

    @Test
    public void testMultipleDefaults() throws IOException {
        var params = parseParams("(name: string = \"Unnamed\", age: i32 = 0)");

        assertEquals(2, params.size());
        assertNotNull(params.get(0).defaultValue());
        assertEquals("\"Unnamed\"", params.get(0).defaultValue().getFirst().lexeme());
        assertNotNull(params.get(1).defaultValue());
        assertEquals("0", params.get(1).defaultValue().getFirst().lexeme());
    }

    @Test
    public void testEmptyParameterList() throws IOException {
        var params = parseParams("()");

        assertTrue(params.isEmpty());
    }

    // endregion

    // region ParameterListParser — Complex Combinations

    @Test
    public void testNullableWithDefault() throws IOException {
        var params = parseParams("(callback: Runnable? = null, retries: i32 = 3)");

        assertEquals(2, params.size());

        assertEquals("Runnable", params.getFirst().type().baseName().lexeme());
        assertTrue(params.get(0).type().nullable());
        assertEquals("null", params.get(0).defaultValue().getFirst().lexeme());

        assertEquals("i32", params.get(1).type().baseName().lexeme());
        assertEquals("3", params.get(1).defaultValue().getFirst().lexeme());
    }

    @Test
    public void testArrayWithDefault() throws IOException {
        var params = parseParams("(tags: string[] = null)");

        assertEquals(1, params.size());
        assertEquals("string", params.getFirst().type().baseName().lexeme());
        assertEquals(1, params.getFirst().type().arrayDepth());
        assertNotNull(params.getFirst().defaultValue());
    }

    // endregion

    // region Base Class Inheritance

    @Test
    public void testClassWithBaseClass() throws IOException {
        var result = parseClass("class Dog(): Animal() {}");

        assertEquals("Dog", result.name().lexeme());
        assertNotNull(result.baseClass());
        assertEquals("Animal", result.baseClass().toString());
        assertTrue(result.interfaces().isEmpty());
    }

    @Test
    public void testClassWithBaseClassAndArgs() throws IOException {
        var result = parseClass("class Dog(): Animal(\"Kibble\") {}");

        assertEquals("Dog", result.name().lexeme());
        assertNotNull(result.baseClass());
        assertEquals("Animal", result.baseClass().toString());
    }

    // endregion

    // region Interface Implementation

    @Test
    public void testClassWithSingleInterface() throws IOException {
        var result = parseClass("class Foo() is Serializable {}");

        assertNull(result.baseClass());
        assertEquals(1, result.interfaces().size());
        assertEquals("Serializable", result.interfaces().getFirst().toString());
    }

    @Test
    public void testClassWithMultipleInterfaces() throws IOException {
        var result = parseClass("class Foo() is Serializable, Printable {}");

        assertEquals(2, result.interfaces().size());
        assertEquals("Serializable", result.interfaces().get(0).toString());
        assertEquals("Printable", result.interfaces().get(1).toString());
    }

    @Test
    public void testClassWithBaseAndInterfaces() throws IOException {
        var result = parseClass("class Dog(): Animal(\"Kibble\") is Serializable, Printable {}");

        assertNotNull(result.baseClass());
        assertEquals("Animal", result.baseClass().toString());
        assertEquals(2, result.interfaces().size());
    }

    // endregion

    // region Visibility Flags

    @Test
    public void testPublicClass() throws IOException {
        var result = parseClass("public class Foo {}");
        assertEquals(Visibility.Type.PUBLIC, result.flags().getVisibility());
    }

    @Test
    public void testPrivateClass() throws IOException {
        var result = parseClass("private class Foo {}");
        assertEquals(Visibility.Type.PRIVATE, result.flags().getVisibility());
    }

    @Test
    public void testInternalClass() throws IOException {
        var result = parseClass("internal class Foo {}");
        assertEquals(Visibility.Type.INTERNAL, result.flags().getVisibility());
    }

    // endregion

    // region Modifier Flags

    @Test
    public void testAbstractClass() throws IOException {
        var result = parseClass("abstract class Foo {}");
        assertTrue(result.flags().isAbstract());
    }

    @Test
    public void testFinalClass() throws IOException {
        var result = parseClass("final class Foo {}");
        assertTrue(result.flags().isFinal());
    }

    @Test
    public void testStaticClass() throws IOException {
        var result = parseClass("static class Foo {}");
        assertTrue(result.flags().isStatic());
    }

    @Test
    public void testPublicStaticFinalClass() throws IOException {
        var result = parseClass("public static final class Foo {}");
        assertEquals(Visibility.Type.PUBLIC, result.flags().getVisibility());
        assertTrue(result.flags().isStatic());
        assertTrue(result.flags().isFinal());
    }

    // endregion

    // region Full Declarations

    @Test
    public void testFullClassDeclaration() throws IOException {
        var result = parseClass("public final class Dog(name: string, age: i32 = 0): Animal(\"Kibble\") is Serializable {}");

        assertEquals("Dog", result.name().lexeme());
        assertEquals(Visibility.Type.PUBLIC, result.flags().getVisibility());
        assertTrue(result.flags().isFinal());

        assertEquals(2, result.primaryConstructor().size());
        assertEquals("name", result.primaryConstructor().getFirst().name().lexeme());
        assertEquals("string", result.primaryConstructor().get(0).type().baseName().lexeme());
        assertNull(result.primaryConstructor().get(0).defaultValue());

        assertEquals("age", result.primaryConstructor().get(1).name().lexeme());
        assertEquals("i32", result.primaryConstructor().get(1).type().baseName().lexeme());
        assertNotNull(result.primaryConstructor().get(1).defaultValue());

        assertNotNull(result.baseClass());
        assertEquals("Animal", result.baseClass().toString());
        assertEquals(1, result.interfaces().size());
        assertEquals("Serializable", result.interfaces().getFirst().toString());
    }

    @Test
    public void testClassWithNullableAndArrayParams() throws IOException {
        var result = parseClass("class Main(args: string[], config: Config? = null) {}");

        assertEquals(2, result.primaryConstructor().size());

        var args = result.primaryConstructor().getFirst();
        assertEquals("args", args.name().lexeme());
        assertEquals("string", args.type().baseName().lexeme());
        assertEquals(1, args.type().arrayDepth());
        assertFalse(args.type().nullable());
        assertNull(args.defaultValue());

        var config = result.primaryConstructor().get(1);
        assertEquals("config", config.name().lexeme());
        assertEquals("Config", config.type().baseName().lexeme());
        assertTrue(config.type().nullable());
        assertNotNull(config.defaultValue());
        assertEquals("null", config.defaultValue().getFirst().lexeme());
    }

    // endregion

    // region Modifier Validation Errors

    @Test
    public void testDuplicateStaticModifier() {
        var ex = assertThrows(CompileError.class, () -> parseClass("static static class Foo {}"));
        assertTrue(ex.getMessage().contains("Duplicate modifier 'static'"));
    }

    @Test
    public void testDuplicateFinalModifier() {
        var ex = assertThrows(CompileError.class, () -> parseClass("final final class Foo {}"));
        assertTrue(ex.getMessage().contains("Duplicate modifier 'final'"));
    }

    @Test
    public void testDuplicateAbstractModifier() {
        var ex = assertThrows(CompileError.class, () -> parseClass("abstract abstract class Foo {}"));
        assertTrue(ex.getMessage().contains("Duplicate modifier 'abstract'"));
    }

    @Test
    public void testDuplicateOverrideModifier() {
        var ex = assertThrows(CompileError.class, () -> parseClass("override override class Foo {}"));
        assertTrue(ex.getMessage().contains("Duplicate modifier 'override'"));
    }

    @Test
    public void testDuplicateVisibilityModifier() {
        var ex = assertThrows(CompileError.class, () -> parseClass("public public class Foo {}"));
        assertTrue(ex.getMessage().contains("Duplicate modifier 'public'"));
    }

    @Test
    public void testConflictingVisibilityPublicPrivate() {
        var ex = assertThrows(CompileError.class, () -> parseClass("public private class Foo {}"));
        assertTrue(ex.getMessage().contains("Conflicting visibility modifiers"));
    }

    @Test
    public void testConflictingVisibilityPrivateInternal() {
        var ex = assertThrows(CompileError.class, () -> parseClass("private internal class Foo {}"));
        assertTrue(ex.getMessage().contains("Conflicting visibility modifiers"));
    }

    @Test
    public void testConflictingVisibilityInternalPublic() {
        var ex = assertThrows(CompileError.class, () -> parseClass("internal public class Foo {}"));
        assertTrue(ex.getMessage().contains("Conflicting visibility modifiers"));
    }

    @Test
    public void testAbstractFinalConflict() {
        var ex = assertThrows(CompileError.class, () -> parseClass("abstract final class Foo {}"));
        assertTrue(ex.getMessage().contains("'abstract' and 'final' cannot be combined"));
    }

    @Test
    public void testFinalAbstractConflict() {
        var ex = assertThrows(CompileError.class, () -> parseClass("final abstract class Foo {}"));
        assertTrue(ex.getMessage().contains("'abstract' and 'final' cannot be combined"));
    }

    // endregion
}
