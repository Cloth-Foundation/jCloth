package cloth.parser;

import cloth.error.DiagnosticSink;
import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.lexer.LexerOptions;
import cloth.lexer.SourceBuffer;
import cloth.parser.expressions.Expression;
import cloth.parser.statements.FieldParser;
import cloth.parser.statements.Statement;
import cloth.parser.statements.StatementParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class StatementParserTests {

    @TempDir
    Path tempDir;

    private Statement.Block parseBlock(String source) throws IOException {
        Path testFile = tempDir.resolve("test.co");
        Files.writeString(testFile, "{ " + source + " }");

        SourceFile sourceFile = new SourceFile(testFile.toString());
        SourceBuffer buffer = new SourceBuffer(sourceFile, "{ " + source + " }");
        DiagnosticSink diagnostics = new DiagnosticSink();
        LexerOptions options = new LexerOptions();

        Lexer lexer = new Lexer(buffer, diagnostics, options);
        return new StatementParser(lexer, sourceFile).parse();
    }

    // region Empty block

    @Test
    public void testEmptyBlock() throws IOException {
        var block = parseBlock("");
        assertTrue(block.statements().isEmpty());
    }

    // endregion

    // region Variable declarations

    @Test
    public void testVarDeclaration() throws IOException {
        var block = parseBlock("var x: i32 = 10;");

        assertEquals(1, block.statements().size());
        assertInstanceOf(Statement.VarDeclaration.class, block.statements().getFirst());

        var decl = (Statement.VarDeclaration) block.statements().getFirst();
        assertEquals(FieldParser.BindingKind.VAR, decl.binding());
        assertEquals("x", decl.name().lexeme());
        assertEquals("i32", decl.type().baseName().lexeme());
        assertNotNull(decl.initializer());
        assertInstanceOf(Expression.Literal.class, decl.initializer());
        assertEquals("10", ((Expression.Literal) decl.initializer()).value().lexeme());
    }

    @Test
    public void testLetDeclaration() throws IOException {
        var block = parseBlock("let name: string = \"hello\";");

        var decl = (Statement.VarDeclaration) block.statements().getFirst();
        assertEquals(FieldParser.BindingKind.LET, decl.binding());
        assertEquals("name", decl.name().lexeme());
        assertEquals("string", decl.type().baseName().lexeme());
        assertNotNull(decl.initializer());
    }

    @Test
    public void testConstDeclaration() throws IOException {
        var block = parseBlock("const limit: i32 = 100;");

        var decl = (Statement.VarDeclaration) block.statements().getFirst();
        assertEquals(FieldParser.BindingKind.CONST, decl.binding());
        assertEquals("limit", decl.name().lexeme());
    }

    @Test
    public void testVarDeclarationWithoutInitializer() throws IOException {
        var block = parseBlock("var count: i32;");

        var decl = (Statement.VarDeclaration) block.statements().getFirst();
        assertEquals("count", decl.name().lexeme());
        assertNull(decl.initializer());
    }

    @Test
    public void testVarNullableType() throws IOException {
        var block = parseBlock("var name: string?;");

        var decl = (Statement.VarDeclaration) block.statements().getFirst();
        assertTrue(decl.type().nullable());
    }

    @Test
    public void testVarArrayType() throws IOException {
        var block = parseBlock("var items: i32[];");

        var decl = (Statement.VarDeclaration) block.statements().getFirst();
        assertEquals(1, decl.type().arrayDepth());
    }

    // endregion

    // region Expression statements

    @Test
    public void testExpressionStatement() throws IOException {
        var block = parseBlock("foo();");

        assertEquals(1, block.statements().size());
        assertInstanceOf(Statement.ExpressionStmt.class, block.statements().getFirst());

        var stmt = (Statement.ExpressionStmt) block.statements().getFirst();
        assertInstanceOf(Expression.Call.class, stmt.expression());
    }

    @Test
    public void testMethodCallExpression() throws IOException {
        var block = parseBlock("obj.method();");

        var stmt = (Statement.ExpressionStmt) block.statements().getFirst();
        assertInstanceOf(Expression.Call.class, stmt.expression());
        var call = (Expression.Call) stmt.expression();
        assertInstanceOf(Expression.MemberAccess.class, call.callee());
    }

    @Test
    public void testChainedMethodCall() throws IOException {
        var block = parseBlock("a.b().c();");

        var stmt = (Statement.ExpressionStmt) block.statements().getFirst();
        assertInstanceOf(Expression.Call.class, stmt.expression());
    }

    // endregion

    // region Assignment

    @Test
    public void testAssignmentToIdentifier() throws IOException {
        var block = parseBlock("x = 42;");

        assertEquals(1, block.statements().size());
        assertInstanceOf(Statement.Assignment.class, block.statements().getFirst());

        var assign = (Statement.Assignment) block.statements().getFirst();
        assertInstanceOf(Expression.Identifier.class, assign.target());
        assertEquals("x", ((Expression.Identifier) assign.target()).name().lexeme());
        assertInstanceOf(Expression.Literal.class, assign.value());
        assertEquals("=", assign.operator().lexeme());
    }

    @Test
    public void testAssignmentToMemberAccess() throws IOException {
        var block = parseBlock("obj.field = 10;");

        var assign = (Statement.Assignment) block.statements().getFirst();
        assertInstanceOf(Expression.MemberAccess.class, assign.target());
    }

    @Test
    public void testAssignmentToArrayIndex() throws IOException {
        var block = parseBlock("arr[0] = 99;");

        var assign = (Statement.Assignment) block.statements().getFirst();
        assertInstanceOf(Expression.Index.class, assign.target());
    }

    @Test
    public void testAssignmentWithExpression() throws IOException {
        var block = parseBlock("x = a + b * c;");

        var assign = (Statement.Assignment) block.statements().getFirst();
        assertInstanceOf(Expression.Binary.class, assign.value());
    }

    // endregion

    // region Return

    @Test
    public void testReturnWithValue() throws IOException {
        var block = parseBlock("return 42;");

        assertEquals(1, block.statements().size());
        assertInstanceOf(Statement.Return.class, block.statements().getFirst());

        var ret = (Statement.Return) block.statements().getFirst();
        assertEquals("return", ret.keyword().lexeme());
        assertNotNull(ret.value());
        assertInstanceOf(Expression.Literal.class, ret.value());
    }

    @Test
    public void testReturnWithoutValue() throws IOException {
        var block = parseBlock("return;");

        var ret = (Statement.Return) block.statements().getFirst();
        assertNull(ret.value());
    }

    @Test
    public void testReturnExpression() throws IOException {
        var block = parseBlock("return a + b;");

        var ret = (Statement.Return) block.statements().getFirst();
        assertInstanceOf(Expression.Binary.class, ret.value());
    }

    // endregion

    // region If / else / else-if

    @Test
    public void testSimpleIf() throws IOException {
        var block = parseBlock("if x { }");

        assertEquals(1, block.statements().size());
        assertInstanceOf(Statement.If.class, block.statements().getFirst());

        var ifStmt = (Statement.If) block.statements().getFirst();
        assertInstanceOf(Expression.Identifier.class, ifStmt.condition());
        assertNotNull(ifStmt.thenBlock());
        assertTrue(ifStmt.thenBlock().statements().isEmpty());
        assertNull(ifStmt.elseBlock());
    }

    @Test
    public void testIfWithBody() throws IOException {
        var block = parseBlock("if x { return 1; }");

        var ifStmt = (Statement.If) block.statements().getFirst();
        assertEquals(1, ifStmt.thenBlock().statements().size());
        assertInstanceOf(Statement.Return.class, ifStmt.thenBlock().statements().getFirst());
    }

    @Test
    public void testIfElse() throws IOException {
        var block = parseBlock("if x { return 1; } else { return 2; }");

        var ifStmt = (Statement.If) block.statements().getFirst();
        assertEquals(1, ifStmt.thenBlock().statements().size());
        assertNotNull(ifStmt.elseBlock());
        assertInstanceOf(Statement.Block.class, ifStmt.elseBlock());
        var elseBlock = (Statement.Block) ifStmt.elseBlock();
        assertEquals(1, elseBlock.statements().size());
    }

    @Test
    public void testElseIf() throws IOException {
        var block = parseBlock("if a { return 1; } else if b { return 2; } else { return 3; }");

        var ifStmt = (Statement.If) block.statements().getFirst();
        assertNotNull(ifStmt.elseBlock());
        assertInstanceOf(Statement.If.class, ifStmt.elseBlock());

        var elseIf = (Statement.If) ifStmt.elseBlock();
        assertInstanceOf(Expression.Identifier.class, elseIf.condition());
        assertEquals("b", ((Expression.Identifier) elseIf.condition()).name().lexeme());

        assertNotNull(elseIf.elseBlock());
        assertInstanceOf(Statement.Block.class, elseIf.elseBlock());
    }

    @Test
    public void testIfWithComparisonCondition() throws IOException {
        var block = parseBlock("if x > 0 { return x; }");

        var ifStmt = (Statement.If) block.statements().getFirst();
        assertInstanceOf(Expression.Binary.class, ifStmt.condition());
    }

    @Test
    public void testIfParenthesizedCondition() throws IOException {
        var block = parseBlock("if (x > 0) { return x; }");

        var ifStmt = (Statement.If) block.statements().getFirst();
        assertInstanceOf(Expression.Binary.class, ifStmt.condition());
    }

    @Test
    public void testIfParenthesizedSimpleCondition() throws IOException {
        var block = parseBlock("if (ready) { return 1; }");

        var ifStmt = (Statement.If) block.statements().getFirst();
        assertInstanceOf(Expression.Identifier.class, ifStmt.condition());
        assertEquals("ready", ((Expression.Identifier) ifStmt.condition()).name().lexeme());
    }

    @Test
    public void testIfParenthesizedWithElse() throws IOException {
        var block = parseBlock("if (x) { return 1; } else { return 2; }");

        var ifStmt = (Statement.If) block.statements().getFirst();
        assertInstanceOf(Expression.Identifier.class, ifStmt.condition());
        assertNotNull(ifStmt.elseBlock());
    }

    @Test
    public void testIfParenthesizedElseIfChain() throws IOException {
        var block = parseBlock("if (a) { return 1; } else if (b) { return 2; } else { return 3; }");

        var ifStmt = (Statement.If) block.statements().getFirst();
        assertInstanceOf(Expression.Identifier.class, ifStmt.condition());

        assertInstanceOf(Statement.If.class, ifStmt.elseBlock());
        var elseIf = (Statement.If) ifStmt.elseBlock();
        assertInstanceOf(Expression.Identifier.class, elseIf.condition());
        assertEquals("b", ((Expression.Identifier) elseIf.condition()).name().lexeme());

        assertNotNull(elseIf.elseBlock());
        assertInstanceOf(Statement.Block.class, elseIf.elseBlock());
    }

    @Test
    public void testIfParenthesizedCompoundCondition() throws IOException {
        var block = parseBlock("if (x > 0 and y > 0) { return 1; }");

        var ifStmt = (Statement.If) block.statements().getFirst();
        assertInstanceOf(Expression.Binary.class, ifStmt.condition());
    }

    // endregion

    // region While

    @Test
    public void testSimpleWhile() throws IOException {
        var block = parseBlock("while running { }");

        assertEquals(1, block.statements().size());
        assertInstanceOf(Statement.While.class, block.statements().getFirst());

        var whileStmt = (Statement.While) block.statements().getFirst();
        assertInstanceOf(Expression.Identifier.class, whileStmt.condition());
        assertTrue(whileStmt.body().statements().isEmpty());
    }

    @Test
    public void testWhileWithBody() throws IOException {
        var block = parseBlock("while i > 0 { i = i - 1; }");

        var whileStmt = (Statement.While) block.statements().getFirst();
        assertInstanceOf(Expression.Binary.class, whileStmt.condition());
        assertEquals(1, whileStmt.body().statements().size());
        assertInstanceOf(Statement.Assignment.class, whileStmt.body().statements().getFirst());
    }

    @Test
    public void testWhileWithMultipleStatements() throws IOException {
        var block = parseBlock("while x { foo(); x = false; }");

        var whileStmt = (Statement.While) block.statements().getFirst();
        assertEquals(2, whileStmt.body().statements().size());
    }

    @Test
    public void testWhileParenthesizedCondition() throws IOException {
        var block = parseBlock("while (i > 0) { i = i - 1; }");

        var whileStmt = (Statement.While) block.statements().getFirst();
        assertInstanceOf(Expression.Binary.class, whileStmt.condition());
        assertEquals(1, whileStmt.body().statements().size());
        assertInstanceOf(Statement.Assignment.class, whileStmt.body().statements().getFirst());
    }

    @Test
    public void testWhileParenthesizedSimpleCondition() throws IOException {
        var block = parseBlock("while (running) { foo(); }");

        var whileStmt = (Statement.While) block.statements().getFirst();
        assertInstanceOf(Expression.Identifier.class, whileStmt.condition());
        assertEquals("running", ((Expression.Identifier) whileStmt.condition()).name().lexeme());
    }

    @Test
    public void testWhileParenthesizedCompoundCondition() throws IOException {
        var block = parseBlock("while (x > 0 and y > 0) { x = x - 1; }");

        var whileStmt = (Statement.While) block.statements().getFirst();
        assertInstanceOf(Expression.Binary.class, whileStmt.condition());
    }

    @Test
    public void testWhileParenthesizedEmpty() throws IOException {
        var block = parseBlock("while (true) { }");

        var whileStmt = (Statement.While) block.statements().getFirst();
        assertInstanceOf(Expression.Literal.class, whileStmt.condition());
        assertEquals("true", ((Expression.Literal) whileStmt.condition()).value().lexeme());
        assertTrue(whileStmt.body().statements().isEmpty());
    }

    // endregion

    // region Delete

    @Test
    public void testDeleteIdentifier() throws IOException {
        var block = parseBlock("delete obj;");

        assertEquals(1, block.statements().size());
        assertInstanceOf(Statement.Delete.class, block.statements().getFirst());

        var del = (Statement.Delete) block.statements().getFirst();
        assertInstanceOf(Expression.Identifier.class, del.target());
        assertEquals("obj", ((Expression.Identifier) del.target()).name().lexeme());
    }

    @Test
    public void testDeleteMemberAccess() throws IOException {
        var block = parseBlock("delete this.data;");

        var del = (Statement.Delete) block.statements().getFirst();
        assertInstanceOf(Expression.MemberAccess.class, del.target());
    }

    // endregion

    // region Nested blocks

    @Test
    public void testNestedBlock() throws IOException {
        var block = parseBlock("{ var x: i32 = 1; }");

        assertEquals(1, block.statements().size());
        assertInstanceOf(Statement.Block.class, block.statements().getFirst());

        var inner = (Statement.Block) block.statements().getFirst();
        assertEquals(1, inner.statements().size());
        assertInstanceOf(Statement.VarDeclaration.class, inner.statements().getFirst());
    }

    @Test
    public void testDeeplyNestedBlocks() throws IOException {
        var block = parseBlock("{ { var x: i32 = 1; } }");

        var inner1 = (Statement.Block) block.statements().getFirst();
        var inner2 = (Statement.Block) inner1.statements().getFirst();
        assertEquals(1, inner2.statements().size());
    }

    // endregion

    // region Mixed sequences

    @Test
    public void testMixedStatements() throws IOException {
        var block = parseBlock("""
            var x: i32 = 0;
            x = x + 1;
            if x > 0 {
                return x;
            }
            """);

        assertEquals(3, block.statements().size());
        assertInstanceOf(Statement.VarDeclaration.class, block.statements().get(0));
        assertInstanceOf(Statement.Assignment.class, block.statements().get(1));
        assertInstanceOf(Statement.If.class, block.statements().get(2));
    }

    @Test
    public void testWhileWithVarAndReturn() throws IOException {
        var block = parseBlock("""
            var sum: i32 = 0;
            while i > 0 {
                sum = sum + i;
                i = i - 1;
            }
            return sum;
            """);

        assertEquals(3, block.statements().size());
        assertInstanceOf(Statement.VarDeclaration.class, block.statements().get(0));
        assertInstanceOf(Statement.While.class, block.statements().get(1));
        assertInstanceOf(Statement.Return.class, block.statements().get(2));
    }

    @Test
    public void testIfElseWithAssignments() throws IOException {
        var block = parseBlock("""
            var result: i32;
            if cond {
                result = 1;
            } else {
                result = 2;
            }
            return result;
            """);

        assertEquals(3, block.statements().size());
        assertInstanceOf(Statement.VarDeclaration.class, block.statements().get(0));
        assertInstanceOf(Statement.If.class, block.statements().get(1));
        assertInstanceOf(Statement.Return.class, block.statements().get(2));
    }

    // endregion

    // region Error cases

    @Test
    public void testMissingSemicolonOnVar() {
        assertThrows(CompileError.class, () ->
            parseBlock("var x: i32 = 10"));
    }

    @Test
    public void testMissingSemicolonOnReturn() {
        assertThrows(CompileError.class, () ->
            parseBlock("return 42"));
    }

    @Test
    public void testMissingSemicolonOnExpression() {
        assertThrows(CompileError.class, () ->
            parseBlock("foo()"));
    }

    @Test
    public void testMissingSemicolonOnAssignment() {
        assertThrows(CompileError.class, () ->
            parseBlock("x = 10"));
    }

    @Test
    public void testMissingSemicolonOnDelete() {
        assertThrows(CompileError.class, () ->
            parseBlock("delete obj"));
    }

    @Test
    public void testMissingClosingBraceOnIf() {
        assertThrows(CompileError.class, () ->
            parseBlock("if x { return 1;"));
    }

    @Test
    public void testMissingOpeningBraceOnIf() {
        assertThrows(CompileError.class, () ->
            parseBlock("if x return 1; }"));
    }

    @Test
    public void testMissingClosingBraceOnWhile() {
        assertThrows(CompileError.class, () ->
            parseBlock("while x { foo();"));
    }

    @Test
    public void testVarMissingColon() {
        assertThrows(CompileError.class, () ->
            parseBlock("var x i32;"));
    }

    @Test
    public void testVarMissingName() {
        assertThrows(CompileError.class, () ->
            parseBlock("var : i32;"));
    }

    // endregion
}
