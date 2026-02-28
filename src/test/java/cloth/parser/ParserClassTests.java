package cloth.parser;

import cloth.error.DiagnosticSink;
import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.lexer.LexerOptions;
import cloth.lexer.SourceBuffer;
import cloth.parser.expressions.Expression;
import cloth.parser.flags.Visibility;
import cloth.parser.statements.ClassParser;
import cloth.parser.statements.FieldParser;
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
        assertInstanceOf(Expression.Literal.class, params.getFirst().defaultValue());
        assertEquals("\"Unnamed\"", ((Expression.Literal) params.getFirst().defaultValue()).value().lexeme());
    }

    @Test
    public void testDefaultNumericValue() throws IOException {
        var params = parseParams("(age: i32 = 0)");

        assertEquals(1, params.size());
        assertNotNull(params.getFirst().defaultValue());
        assertInstanceOf(Expression.Literal.class, params.getFirst().defaultValue());
        assertEquals("0", ((Expression.Literal) params.getFirst().defaultValue()).value().lexeme());
    }

    @Test
    public void testDefaultNullValue() throws IOException {
        var params = parseParams("(name: string? = null)");

        assertEquals(1, params.size());
        assertTrue(params.getFirst().type().nullable());
        assertNotNull(params.getFirst().defaultValue());
        assertEquals("null", ((Expression.Literal) params.getFirst().defaultValue()).value().lexeme());
    }

    @Test
    public void testMixedDefaultAndRequired() throws IOException {
        var params = parseParams("(name: string, age: i32 = 0)");

        assertEquals(2, params.size());

        assertNull(params.get(0).defaultValue());
        assertEquals("name", params.get(0).name().lexeme());

        assertNotNull(params.get(1).defaultValue());
        assertEquals("age", params.get(1).name().lexeme());
        assertEquals("0", ((Expression.Literal) params.get(1).defaultValue()).value().lexeme());
    }

    @Test
    public void testMultipleDefaults() throws IOException {
        var params = parseParams("(name: string = \"Unnamed\", age: i32 = 0)");

        assertEquals(2, params.size());
        assertNotNull(params.get(0).defaultValue());
        assertEquals("\"Unnamed\"", ((Expression.Literal) params.get(0).defaultValue()).value().lexeme());
        assertNotNull(params.get(1).defaultValue());
        assertEquals("0", ((Expression.Literal) params.get(1).defaultValue()).value().lexeme());
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
        assertEquals("null", ((Expression.Literal) params.get(0).defaultValue()).value().lexeme());

        assertEquals("i32", params.get(1).type().baseName().lexeme());
        assertEquals("3", ((Expression.Literal) params.get(1).defaultValue()).value().lexeme());
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
        assertEquals("null", ((Expression.Literal) config.defaultValue()).value().lexeme());
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

    // region Field Declarations (standalone)

    private FieldParser.FieldDeclaration parseField(String source) throws IOException {
        Path testFile = tempDir.resolve("test.co");
        Files.writeString(testFile, source);

        SourceFile sourceFile = new SourceFile(testFile.toString());
        SourceBuffer buffer = new SourceBuffer(sourceFile, source);
        DiagnosticSink diagnostics = new DiagnosticSink();
        LexerOptions options = new LexerOptions();

        Lexer lexer = new Lexer(buffer, diagnostics, options);
        return new FieldParser(lexer, sourceFile).parse();
    }

    @Test
    public void testSimpleVarField() throws IOException {
        var result = parseField("var x: i32;");

        assertEquals(FieldParser.BindingKind.VAR, result.binding());
        assertEquals("x", result.name().lexeme());
        assertEquals("i32", result.type().baseName().lexeme());
        assertFalse(result.type().nullable());
        assertEquals(0, result.type().arrayDepth());
        assertNull(result.initializer());
        assertFalse(result.flags().hasFlags());
    }

    @Test
    public void testVarFieldWithInitializer() throws IOException {
        var result = parseField("var x: i32 = 0;");

        assertEquals(FieldParser.BindingKind.VAR, result.binding());
        assertEquals("x", result.name().lexeme());
        assertEquals("i32", result.type().baseName().lexeme());
        assertNotNull(result.initializer());
        assertInstanceOf(Expression.Literal.class, result.initializer());
        assertEquals("0", ((Expression.Literal) result.initializer()).value().lexeme());
    }

    @Test
    public void testLetFieldWithInitializer() throws IOException {
        var result = parseField("let name: string = \"Cloth\";");

        assertEquals(FieldParser.BindingKind.LET, result.binding());
        assertEquals("name", result.name().lexeme());
        assertEquals("string", result.type().baseName().lexeme());
        assertNotNull(result.initializer());
    }

    @Test
    public void testConstField() throws IOException {
        var result = parseField("const LIMIT: i32 = 100;");

        assertEquals(FieldParser.BindingKind.CONST, result.binding());
        assertEquals("LIMIT", result.name().lexeme());
        assertEquals("i32", result.type().baseName().lexeme());
        assertNotNull(result.initializer());
        assertEquals("100", ((Expression.Literal) result.initializer()).value().lexeme());
    }

    @Test
    public void testFinalVarField() throws IOException {
        var result = parseField("final var id: i64 = 123;");

        assertEquals(FieldParser.BindingKind.VAR, result.binding());
        assertEquals("id", result.name().lexeme());
        assertEquals("i64", result.type().baseName().lexeme());
        assertTrue(result.flags().isFinal());
        assertNotNull(result.initializer());
    }

    @Test
    public void testPublicStaticFinalVarField() throws IOException {
        var result = parseField("public static final var PI: f32 = 3.14159;");

        assertEquals(FieldParser.BindingKind.VAR, result.binding());
        assertEquals("PI", result.name().lexeme());
        assertEquals("f32", result.type().baseName().lexeme());
        assertEquals(Visibility.Type.PUBLIC, result.flags().getVisibility());
        assertTrue(result.flags().isStatic());
        assertTrue(result.flags().isFinal());
        assertNotNull(result.initializer());
    }

    @Test
    public void testNullableTypeField() throws IOException {
        var result = parseField("var user: User?;");

        assertEquals("User", result.type().baseName().lexeme());
        assertTrue(result.type().nullable());
    }

    @Test
    public void testArrayTypeField() throws IOException {
        var result = parseField("var items: string[];");

        assertEquals("string", result.type().baseName().lexeme());
        assertEquals(1, result.type().arrayDepth());
    }

    @Test
    public void testNullableArrayField() throws IOException {
        var result = parseField("var data: i32?[];");

        assertEquals("i32", result.type().baseName().lexeme());
        assertTrue(result.type().nullable());
        assertEquals(1, result.type().arrayDepth());
    }

    @Test
    public void testMultiDimensionalArrayField() throws IOException {
        var result = parseField("var matrix: f64[][];");

        assertEquals("f64", result.type().baseName().lexeme());
        assertEquals(2, result.type().arrayDepth());
    }

    @Test
    public void testFieldWithComplexInitializer() throws IOException {
        var result = parseField("var x: i32 = 2 * 3 + 1;");

        assertNotNull(result.initializer());
        assertInstanceOf(Expression.Binary.class, result.initializer());
    }

    @Test
    public void testFieldReturnsNullWhenNotField() throws IOException {
        var result = parseField("class Foo {}");
        assertNull(result);
    }

    @Test
    public void testFieldWithModifiersButNoBinding() {
        var ex = assertThrows(CompileError.class, () -> parseField("public 42;"));
        assertTrue(ex.getMessage().contains("Expected a declaration after modifiers"));
    }

    @Test
    public void testPrivateVarField() throws IOException {
        var result = parseField("private var secret: string = \"hidden\";");

        assertEquals(Visibility.Type.PRIVATE, result.flags().getVisibility());
        assertEquals("secret", result.name().lexeme());
    }

    @Test
    public void testInternalLetField() throws IOException {
        var result = parseField("internal let config: string = \"default\";");

        assertEquals(Visibility.Type.INTERNAL, result.flags().getVisibility());
        assertEquals(FieldParser.BindingKind.LET, result.binding());
    }

    // endregion

    // region Fields Inside Class Body

    @Test
    public void testClassWithSingleField() throws IOException {
        var result = parseClass("class Foo { var x: i32 = 0; }");

        assertEquals("Foo", result.name().lexeme());
        assertEquals(1, result.fields().size());

        var field = result.fields().getFirst();
        assertEquals("x", field.name().lexeme());
        assertEquals("i32", field.type().baseName().lexeme());
        assertNotNull(field.initializer());
    }

    @Test
    public void testClassWithMultipleFields() throws IOException {
        var result = parseClass("class Point { var x: f64 = 0.0; var y: f64 = 0.0; }");

        assertEquals(2, result.fields().size());
        assertEquals("x", result.fields().get(0).name().lexeme());
        assertEquals("y", result.fields().get(1).name().lexeme());
    }

    @Test
    public void testClassWithMixedFieldBindings() throws IOException {
        var result = parseClass(
            "class Config { let name: string = \"app\"; var count: i32 = 0; const LIMIT: i32 = 100; }");

        assertEquals(3, result.fields().size());
        assertEquals(FieldParser.BindingKind.LET, result.fields().get(0).binding());
        assertEquals(FieldParser.BindingKind.VAR, result.fields().get(1).binding());
        assertEquals(FieldParser.BindingKind.CONST, result.fields().get(2).binding());
    }

    @Test
    public void testClassWithModifiedFields() throws IOException {
        var result = parseClass(
            "class Math { public static final var PI: f64 = 3.14; private var count: i32 = 0; }");

        assertEquals(2, result.fields().size());

        var pi = result.fields().getFirst();
        assertEquals(Visibility.Type.PUBLIC, pi.flags().getVisibility());
        assertTrue(pi.flags().isStatic());
        assertTrue(pi.flags().isFinal());
        assertEquals("PI", pi.name().lexeme());

        var count = result.fields().get(1);
        assertEquals(Visibility.Type.PRIVATE, count.flags().getVisibility());
        assertEquals("count", count.name().lexeme());
    }

    @Test
    public void testEmptyClassStillHasNoFields() throws IOException {
        var result = parseClass("class Empty {}");
        assertTrue(result.fields().isEmpty());
    }

    // endregion
}
