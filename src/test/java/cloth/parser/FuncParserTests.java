package cloth.parser;

import cloth.error.DiagnosticSink;
import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.lexer.LexerOptions;
import cloth.lexer.SourceBuffer;
import cloth.parser.flags.Visibility;
import cloth.parser.statements.ClassParser;
import cloth.parser.statements.EnumParser;
import cloth.parser.statements.FuncParser;
import cloth.parser.statements.StructParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FuncParserTests {

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

    // region Simple Methods

    @Test
    public void testSimpleMethod() throws IOException {
        var cls = parseClass("class Foo { func greet(): void { } }");

        assertEquals(1, cls.methods().size());
        var method = cls.methods().getFirst();
        assertEquals("greet", method.name().lexeme());
        assertTrue(method.parameters().isEmpty());
        assertEquals("void", method.returnType().baseName().lexeme());
        assertNotNull(method.body());
        assertTrue(method.body().isEmpty());
    }

    @Test
    public void testMethodWithReturnType() throws IOException {
        var cls = parseClass("class Calc { func compute(): i32 { return 42; } }");

        var method = cls.methods().getFirst();
        assertEquals("compute", method.name().lexeme());
        assertEquals("i32", method.returnType().baseName().lexeme());
        assertNotNull(method.body());
        assertFalse(method.body().isEmpty());
    }

    @Test
    public void testMethodWithParameters() throws IOException {
        var cls = parseClass("class Math { func add(a: i32, b: i32): i32 { return a + b; } }");

        var method = cls.methods().getFirst();
        assertEquals("add", method.name().lexeme());
        assertEquals(2, method.parameters().size());
        assertEquals("a", method.parameters().get(0).name().lexeme());
        assertEquals("i32", method.parameters().get(0).type().baseName().lexeme());
        assertEquals("b", method.parameters().get(1).name().lexeme());
        assertEquals("i32", method.returnType().baseName().lexeme());
    }

    @Test
    public void testMethodWithNoParams() throws IOException {
        var cls = parseClass("class Clock { func tick(): void { } }");

        var method = cls.methods().getFirst();
        assertEquals("tick", method.name().lexeme());
        assertTrue(method.parameters().isEmpty());
    }

    @Test
    public void testMethodNullableReturnType() throws IOException {
        var cls = parseClass("class Lookup { func find(key: string): i32? { return 0; } }");

        var method = cls.methods().getFirst();
        assertTrue(method.returnType().nullable());
        assertEquals("i32", method.returnType().baseName().lexeme());
    }

    @Test
    public void testMethodArrayReturnType() throws IOException {
        var cls = parseClass("class Collector { func items(): string[] { return x; } }");

        var method = cls.methods().getFirst();
        assertEquals(1, method.returnType().arrayDepth());
    }

    // endregion

    // region Modifiers

    @Test
    public void testPublicMethod() throws IOException {
        var cls = parseClass("class Svc { public func run(): void { } }");

        var method = cls.methods().getFirst();
        assertEquals(Visibility.Type.PUBLIC, method.flags().getVisibility());
    }

    @Test
    public void testPrivateMethod() throws IOException {
        var cls = parseClass("class Svc { private func helper(): void { } }");

        var method = cls.methods().getFirst();
        assertEquals(Visibility.Type.PRIVATE, method.flags().getVisibility());
    }

    @Test
    public void testStaticMethod() throws IOException {
        var cls = parseClass("class Factory { static func create(): i32 { return 0; } }");

        var method = cls.methods().getFirst();
        assertTrue(method.flags().isStatic());
    }

    @Test
    public void testFinalMethod() throws IOException {
        var cls = parseClass("class Base { final func locked(): void { } }");

        var method = cls.methods().getFirst();
        assertTrue(method.flags().isFinal());
    }

    @Test
    public void testOverrideMethod() throws IOException {
        var cls = parseClass("class Child { override func act(): void { } }");

        var method = cls.methods().getFirst();
        assertTrue(method.flags().isOverride());
    }

    @Test
    public void testPublicStaticMethod() throws IOException {
        var cls = parseClass("class App { public static func main(): void { } }");

        var method = cls.methods().getFirst();
        assertEquals(Visibility.Type.PUBLIC, method.flags().getVisibility());
        assertTrue(method.flags().isStatic());
    }

    @Test
    public void testPublicFinalMethod() throws IOException {
        var cls = parseClass("class Core { public final func seal(): void { } }");

        var method = cls.methods().getFirst();
        assertEquals(Visibility.Type.PUBLIC, method.flags().getVisibility());
        assertTrue(method.flags().isFinal());
    }

    // endregion

    // region Abstract Methods

    @Test
    public void testAbstractMethod() throws IOException {
        var cls = parseClass("class Shape { abstract func area(): f64; }");

        var method = cls.methods().getFirst();
        assertTrue(method.flags().isAbstract());
        assertEquals("area", method.name().lexeme());
        assertEquals("f64", method.returnType().baseName().lexeme());
        assertNull(method.body());
    }

    @Test
    public void testAbstractMethodWithParams() throws IOException {
        var cls = parseClass("class Codec { abstract func encode(data: u8[]): string; }");

        var method = cls.methods().getFirst();
        assertTrue(method.flags().isAbstract());
        assertNull(method.body());
        assertEquals(1, method.parameters().size());
        assertEquals("data", method.parameters().getFirst().name().lexeme());
    }

    @Test
    public void testPublicAbstractMethod() throws IOException {
        var cls = parseClass("class Renderer { public abstract func render(): void; }");

        var method = cls.methods().getFirst();
        assertEquals(Visibility.Type.PUBLIC, method.flags().getVisibility());
        assertTrue(method.flags().isAbstract());
        assertNull(method.body());
    }

    @Test
    public void testAbstractMethodWithBodyRejected() {
        var ex = assertThrows(CompileError.class, () ->
            parseClass("class Bad { abstract func go(): void { } }"));
        assertTrue(ex.getMessage().contains("Abstract methods must not have a body"));
    }

    @Test
    public void testNonAbstractMethodWithoutBodyRejected() {
        var ex = assertThrows(CompileError.class, () ->
            parseClass("class Bad { func go(): void; }"));
        assertTrue(ex.getMessage().contains("Non-abstract methods must have a body"));
    }

    // endregion

    // region Multiple Members

    @Test
    public void testClassWithFieldsAndMethods() throws IOException {
        var cls = parseClass("""
            class Entity {
                var name: string = "";
                let id: i64 = 0;
                func getName(): string { return name; }
                func setName(n: string): void { name = n; }
            }""");

        assertEquals(2, cls.fields().size());
        assertEquals(2, cls.methods().size());
        assertEquals("getName", cls.methods().get(0).name().lexeme());
        assertEquals("setName", cls.methods().get(1).name().lexeme());
    }

    @Test
    public void testClassWithMixedModifiers() throws IOException {
        var cls = parseClass("""
            class Service {
                public static func getInstance(): i32 { return 0; }
                private func init(): void { }
                override func toString(): string { return ""; }
            }""");

        assertEquals(3, cls.methods().size());

        var m1 = cls.methods().get(0);
        assertTrue(m1.flags().isStatic());
        assertEquals(Visibility.Type.PUBLIC, m1.flags().getVisibility());

        var m2 = cls.methods().get(1);
        assertEquals(Visibility.Type.PRIVATE, m2.flags().getVisibility());

        var m3 = cls.methods().get(2);
        assertTrue(m3.flags().isOverride());
    }

    @Test
    public void testClassWithAbstractAndConcrete() throws IOException {
        var cls = parseClass("""
            class Shape {
                abstract func area(): f64;
                abstract func perimeter(): f64;
                func describe(): string { return "shape"; }
            }""");

        assertEquals(3, cls.methods().size());
        assertNull(cls.methods().get(0).body());
        assertNull(cls.methods().get(1).body());
        assertNotNull(cls.methods().get(2).body());
    }

    // endregion

    // region Nested Braces in Body

    @Test
    public void testMethodWithNestedBraces() throws IOException {
        var cls = parseClass("""
            class Logic {
                func process(): void {
                    if (true) {
                        while (false) {
                            x = 1;
                        }
                    }
                }
            }""");

        assertEquals(1, cls.methods().size());
        assertNotNull(cls.methods().getFirst().body());
        assertFalse(cls.methods().getFirst().body().isEmpty());
    }

    @Test
    public void testMultipleMethodsWithBodies() throws IOException {
        var cls = parseClass("""
            class Ops {
                func first(): void { if (true) { } }
                func second(): i32 { return 1 + 2; }
            }""");

        assertEquals(2, cls.methods().size());
        assertEquals("first", cls.methods().get(0).name().lexeme());
        assertEquals("second", cls.methods().get(1).name().lexeme());
    }

    // endregion

    // region Methods in Structs

    @Test
    public void testStructWithMethod() throws IOException {
        var st = parseStruct("struct Vec2(x: f32, y: f32) { func length(): f32 { return 0.0; } }");

        assertEquals(1, st.methods().size());
        assertEquals("length", st.methods().getFirst().name().lexeme());
        assertEquals("f32", st.methods().getFirst().returnType().baseName().lexeme());
    }

    @Test
    public void testStructWithFieldsAndMethods() throws IOException {
        var st = parseStruct("""
            struct Config {
                var timeout: i32 = 30;
                func isValid(): bool { return timeout > 0; }
            }""");

        assertEquals(1, st.fields().size());
        assertEquals(1, st.methods().size());
    }

    // endregion

    // region Methods in Enums

    @Test
    public void testEnumWithMethod() throws IOException {
        var en = parseEnum("""
            enum Direction {
                North, South, East, West;
                func opposite(): i32 { return 0; }
            }""");

        assertEquals(4, en.cases().size());
        assertEquals(1, en.methods().size());
        assertEquals("opposite", en.methods().getFirst().name().lexeme());
    }

    @Test
    public void testEnumWithFieldsAndMethods() throws IOException {
        var en = parseEnum("""
            enum Color {
                Red, Green, Blue;
                var code: i32 = 0;
                func label(): string { return "color"; }
            }""");

        assertEquals(3, en.cases().size());
        assertEquals(1, en.fields().size());
        assertEquals(1, en.methods().size());
    }

    // endregion

    // region Default Parameters

    @Test
    public void testMethodWithDefaultParam() throws IOException {
        var cls = parseClass(
            "class Logger { func log(msg: string, level: i32 = 0): void { } }");

        var method = cls.methods().getFirst();
        assertEquals(2, method.parameters().size());
        assertNull(method.parameters().get(0).defaultValue());
        assertNotNull(method.parameters().get(1).defaultValue());
    }

    // endregion

    // region Syntax Errors

    @Test
    public void testMissingFuncName() {
        assertThrows(CompileError.class, () ->
            parseClass("class Bad { func (): void { } }"));
    }

    @Test
    public void testMissingReturnTypeColon() {
        assertThrows(CompileError.class, () ->
            parseClass("class Bad { func go() void { } }"));
    }

    @Test
    public void testMissingOpenBrace() {
        assertThrows(CompileError.class, () ->
            parseClass("class Bad { func go(): void return 1; } }"));
    }

    // endregion
}
