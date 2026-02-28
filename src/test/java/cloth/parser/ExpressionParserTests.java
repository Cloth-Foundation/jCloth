package cloth.parser;

import cloth.error.DiagnosticSink;
import cloth.error.errors.CompileError;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.lexer.LexerOptions;
import cloth.lexer.SourceBuffer;
import cloth.parser.expressions.Expression;
import cloth.parser.expressions.ExpressionParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ExpressionParserTests {

    @TempDir
    Path tempDir;

    private Expression parseExpr(String source) throws IOException {
        Path testFile = tempDir.resolve("test.co");
        Files.writeString(testFile, source);

        SourceFile sourceFile = new SourceFile(testFile.toString());
        SourceBuffer buffer = new SourceBuffer(sourceFile, source);
        DiagnosticSink diagnostics = new DiagnosticSink();
        LexerOptions options = new LexerOptions();

        Lexer lexer = new Lexer(buffer, diagnostics, options);
        return new ExpressionParser(lexer, sourceFile).parse();
    }

    // region Literals

    @Test
    public void testIntegerLiteral() throws IOException {
        var expr = parseExpr("42");
        assertInstanceOf(Expression.Literal.class, expr);
        assertEquals("42", ((Expression.Literal) expr).value().lexeme());
    }

    @Test
    public void testFloatLiteral() throws IOException {
        var expr = parseExpr("3.14");
        assertInstanceOf(Expression.Literal.class, expr);
        assertEquals("3.14", ((Expression.Literal) expr).value().lexeme());
    }

    @Test
    public void testStringLiteral() throws IOException {
        var expr = parseExpr("\"hello\"");
        assertInstanceOf(Expression.Literal.class, expr);
    }

    @Test
    public void testTrueLiteral() throws IOException {
        var expr = parseExpr("true");
        assertInstanceOf(Expression.Literal.class, expr);
        assertEquals("true", ((Expression.Literal) expr).value().lexeme());
    }

    @Test
    public void testFalseLiteral() throws IOException {
        var expr = parseExpr("false");
        assertInstanceOf(Expression.Literal.class, expr);
        assertEquals("false", ((Expression.Literal) expr).value().lexeme());
    }

    @Test
    public void testNullLiteral() throws IOException {
        var expr = parseExpr("null");
        assertInstanceOf(Expression.Literal.class, expr);
        assertEquals("null", ((Expression.Literal) expr).value().lexeme());
    }

    // endregion

    // region Identifiers and this

    @Test
    public void testIdentifier() throws IOException {
        var expr = parseExpr("foo");
        assertInstanceOf(Expression.Identifier.class, expr);
        assertEquals("foo", ((Expression.Identifier) expr).name().lexeme());
    }

    @Test
    public void testThisExpr() throws IOException {
        var expr = parseExpr("this");
        assertInstanceOf(Expression.ThisExpr.class, expr);
    }

    // endregion

    // region Unary operators

    @Test
    public void testUnaryMinus() throws IOException {
        var expr = parseExpr("-x");
        assertInstanceOf(Expression.Unary.class, expr);
        var unary = (Expression.Unary) expr;
        assertEquals("-", unary.operator().lexeme());
        assertInstanceOf(Expression.Identifier.class, unary.operand());
    }

    @Test
    public void testLogicalNot() throws IOException {
        var expr = parseExpr("!flag");
        assertInstanceOf(Expression.Unary.class, expr);
        var unary = (Expression.Unary) expr;
        assertEquals("!", unary.operator().lexeme());
        assertInstanceOf(Expression.Identifier.class, unary.operand());
    }

    @Test
    public void testDoubleNegation() throws IOException {
        var expr = parseExpr("- -x");
        assertInstanceOf(Expression.Unary.class, expr);
        var outer = (Expression.Unary) expr;
        assertInstanceOf(Expression.Unary.class, outer.operand());
    }

    @Test
    public void testNegationOfLiteral() throws IOException {
        var expr = parseExpr("-42");
        assertInstanceOf(Expression.Unary.class, expr);
        var unary = (Expression.Unary) expr;
        assertInstanceOf(Expression.Literal.class, unary.operand());
        assertEquals("42", ((Expression.Literal) unary.operand()).value().lexeme());
    }

    // endregion

    // region Binary arithmetic

    @Test
    public void testAddition() throws IOException {
        var expr = parseExpr("a + b");
        assertInstanceOf(Expression.Binary.class, expr);
        var bin = (Expression.Binary) expr;
        assertEquals("+", bin.operator().lexeme());
        assertInstanceOf(Expression.Identifier.class, bin.left());
        assertInstanceOf(Expression.Identifier.class, bin.right());
    }

    @Test
    public void testSubtraction() throws IOException {
        var expr = parseExpr("a - b");
        assertInstanceOf(Expression.Binary.class, expr);
        assertEquals("-", ((Expression.Binary) expr).operator().lexeme());
    }

    @Test
    public void testMultiplication() throws IOException {
        var expr = parseExpr("a * b");
        assertInstanceOf(Expression.Binary.class, expr);
        assertEquals("*", ((Expression.Binary) expr).operator().lexeme());
    }

    @Test
    public void testDivision() throws IOException {
        var expr = parseExpr("a / b");
        assertInstanceOf(Expression.Binary.class, expr);
        assertEquals("/", ((Expression.Binary) expr).operator().lexeme());
    }

    @Test
    public void testModulus() throws IOException {
        var expr = parseExpr("a % b");
        assertInstanceOf(Expression.Binary.class, expr);
        assertEquals("%", ((Expression.Binary) expr).operator().lexeme());
    }

    // endregion

    // region Precedence

    @Test
    public void testMulBeforeAdd() throws IOException {
        var expr = parseExpr("a + b * c");
        assertInstanceOf(Expression.Binary.class, expr);
        var add = (Expression.Binary) expr;
        assertEquals("+", add.operator().lexeme());
        assertInstanceOf(Expression.Identifier.class, add.left());
        assertInstanceOf(Expression.Binary.class, add.right());
        var mul = (Expression.Binary) add.right();
        assertEquals("*", mul.operator().lexeme());
    }

    @Test
    public void testMulBeforeAddReversed() throws IOException {
        var expr = parseExpr("a * b + c");
        assertInstanceOf(Expression.Binary.class, expr);
        var add = (Expression.Binary) expr;
        assertEquals("+", add.operator().lexeme());
        assertInstanceOf(Expression.Binary.class, add.left());
        assertInstanceOf(Expression.Identifier.class, add.right());
    }

    @Test
    public void testAddLeftAssociative() throws IOException {
        var expr = parseExpr("a + b + c");
        assertInstanceOf(Expression.Binary.class, expr);
        var outer = (Expression.Binary) expr;
        assertEquals("+", outer.operator().lexeme());
        assertInstanceOf(Expression.Binary.class, outer.left());
        assertInstanceOf(Expression.Identifier.class, outer.right());
        assertEquals("c", ((Expression.Identifier) outer.right()).name().lexeme());
    }

    @Test
    public void testMulLeftAssociative() throws IOException {
        var expr = parseExpr("a * b * c");
        assertInstanceOf(Expression.Binary.class, expr);
        var outer = (Expression.Binary) expr;
        assertInstanceOf(Expression.Binary.class, outer.left());
        assertInstanceOf(Expression.Identifier.class, outer.right());
    }

    @Test
    public void testComparisonBelowArithmetic() throws IOException {
        var expr = parseExpr("a + b > c");
        assertInstanceOf(Expression.Binary.class, expr);
        var cmp = (Expression.Binary) expr;
        assertInstanceOf(Expression.Binary.class, cmp.left());
        assertInstanceOf(Expression.Identifier.class, cmp.right());
    }

    @Test
    public void testEqualityBelowComparison() throws IOException {
        var expr = parseExpr("a > b == c > d");
        assertInstanceOf(Expression.Binary.class, expr);
        var eq = (Expression.Binary) expr;
        assertInstanceOf(Expression.Binary.class, eq.left());
        assertInstanceOf(Expression.Binary.class, eq.right());
    }

    @Test
    public void testAndBelowEquality() throws IOException {
        var expr = parseExpr("a == b and c == d");
        assertInstanceOf(Expression.Binary.class, expr);
        var andExpr = (Expression.Binary) expr;
        assertEquals("and", andExpr.operator().lexeme());
        assertInstanceOf(Expression.Binary.class, andExpr.left());
        assertInstanceOf(Expression.Binary.class, andExpr.right());
    }

    @Test
    public void testOrBelowAnd() throws IOException {
        var expr = parseExpr("a and b or c and d");
        assertInstanceOf(Expression.Binary.class, expr);
        var orExpr = (Expression.Binary) expr;
        assertEquals("or", orExpr.operator().lexeme());
        assertInstanceOf(Expression.Binary.class, orExpr.left());
        assertInstanceOf(Expression.Binary.class, orExpr.right());
    }

    @Test
    public void testUnaryBindsTighterThanBinary() throws IOException {
        var expr = parseExpr("-a + b");
        assertInstanceOf(Expression.Binary.class, expr);
        var add = (Expression.Binary) expr;
        assertInstanceOf(Expression.Unary.class, add.left());
        assertInstanceOf(Expression.Identifier.class, add.right());
    }

    // endregion

    // region Comparison and equality

    @Test
    public void testEqualOperator() throws IOException {
        var expr = parseExpr("a == b");
        assertInstanceOf(Expression.Binary.class, expr);
        assertEquals("==", ((Expression.Binary) expr).operator().lexeme());
    }

    @Test
    public void testNotEqualOperator() throws IOException {
        var expr = parseExpr("a != b");
        assertInstanceOf(Expression.Binary.class, expr);
        assertEquals("!=", ((Expression.Binary) expr).operator().lexeme());
    }

    @Test
    public void testLessOperator() throws IOException {
        var expr = parseExpr("a < b");
        assertInstanceOf(Expression.Binary.class, expr);
    }

    @Test
    public void testGreaterOperator() throws IOException {
        var expr = parseExpr("a > b");
        assertInstanceOf(Expression.Binary.class, expr);
    }

    // endregion

    // region Logical

    @Test
    public void testAndOperator() throws IOException {
        var expr = parseExpr("a and b");
        assertInstanceOf(Expression.Binary.class, expr);
        assertEquals("and", ((Expression.Binary) expr).operator().lexeme());
    }

    @Test
    public void testOrOperator() throws IOException {
        var expr = parseExpr("a or b");
        assertInstanceOf(Expression.Binary.class, expr);
        assertEquals("or", ((Expression.Binary) expr).operator().lexeme());
    }

    // endregion

    // region Ternary

    @Test
    public void testSimpleTernary() throws IOException {
        var expr = parseExpr("a ? b : c");
        assertInstanceOf(Expression.Ternary.class, expr);
        var tern = (Expression.Ternary) expr;
        assertInstanceOf(Expression.Identifier.class, tern.condition());
        assertInstanceOf(Expression.Identifier.class, tern.thenExpr());
        assertInstanceOf(Expression.Identifier.class, tern.elseExpr());
    }

    @Test
    public void testTernaryRightAssociative() throws IOException {
        var expr = parseExpr("a ? b : c ? d : e");
        assertInstanceOf(Expression.Ternary.class, expr);
        var outer = (Expression.Ternary) expr;
        assertInstanceOf(Expression.Ternary.class, outer.elseExpr());
    }

    @Test
    public void testTernaryWithExpressions() throws IOException {
        var expr = parseExpr("x > 0 ? x : -x");
        assertInstanceOf(Expression.Ternary.class, expr);
        var tern = (Expression.Ternary) expr;
        assertInstanceOf(Expression.Binary.class, tern.condition());
        assertInstanceOf(Expression.Identifier.class, tern.thenExpr());
        assertInstanceOf(Expression.Unary.class, tern.elseExpr());
    }

    // endregion

    // region Cast (as)

    @Test
    public void testSimpleCast() throws IOException {
        var expr = parseExpr("x as i32");
        assertInstanceOf(Expression.Cast.class, expr);
        var cast = (Expression.Cast) expr;
        assertInstanceOf(Expression.Identifier.class, cast.expression());
        assertEquals("i32", cast.targetType().baseName().lexeme());
    }

    @Test
    public void testCastPrecedence() throws IOException {
        var expr = parseExpr("a + b as i32");
        assertInstanceOf(Expression.Cast.class, expr);
        var cast = (Expression.Cast) expr;
        assertInstanceOf(Expression.Binary.class, cast.expression());
    }

    @Test
    public void testCastNullableType() throws IOException {
        var expr = parseExpr("x as i32?");
        assertInstanceOf(Expression.Cast.class, expr);
        assertTrue(((Expression.Cast) expr).targetType().nullable());
    }

    // endregion

    // region New expressions

    @Test
    public void testNewNoArgs() throws IOException {
        var expr = parseExpr("new Foo()");
        assertInstanceOf(Expression.NewExpr.class, expr);
        var n = (Expression.NewExpr) expr;
        assertEquals("Foo", n.type().baseName().lexeme());
        assertTrue(n.arguments().isEmpty());
    }

    @Test
    public void testNewWithArgs() throws IOException {
        var expr = parseExpr("new Dog(42, x)");
        assertInstanceOf(Expression.NewExpr.class, expr);
        var n = (Expression.NewExpr) expr;
        assertEquals("Dog", n.type().baseName().lexeme());
        assertEquals(2, n.arguments().size());
    }

    @Test
    public void testNewWithExpressionArg() throws IOException {
        var expr = parseExpr("new Pair(a + b, c * d)");
        assertInstanceOf(Expression.NewExpr.class, expr);
        var n = (Expression.NewExpr) expr;
        assertEquals(2, n.arguments().size());
        assertInstanceOf(Expression.Binary.class, n.arguments().get(0));
        assertInstanceOf(Expression.Binary.class, n.arguments().get(1));
    }

    // endregion

    // region Member access

    @Test
    public void testSimpleMemberAccess() throws IOException {
        var expr = parseExpr("a.b");
        assertInstanceOf(Expression.MemberAccess.class, expr);
        var ma = (Expression.MemberAccess) expr;
        assertInstanceOf(Expression.Identifier.class, ma.receiver());
        assertEquals("b", ma.member().lexeme());
    }

    @Test
    public void testChainedMemberAccess() throws IOException {
        var expr = parseExpr("a.b.c");
        assertInstanceOf(Expression.MemberAccess.class, expr);
        var outer = (Expression.MemberAccess) expr;
        assertEquals("c", outer.member().lexeme());
        assertInstanceOf(Expression.MemberAccess.class, outer.receiver());
        var inner = (Expression.MemberAccess) outer.receiver();
        assertEquals("b", inner.member().lexeme());
    }

    @Test
    public void testMemberAccessOnCall() throws IOException {
        var expr = parseExpr("foo().bar");
        assertInstanceOf(Expression.MemberAccess.class, expr);
        var ma = (Expression.MemberAccess) expr;
        assertInstanceOf(Expression.Call.class, ma.receiver());
        assertEquals("bar", ma.member().lexeme());
    }

    // endregion

    // region Function calls

    @Test
    public void testSimpleCall() throws IOException {
        var expr = parseExpr("foo()");
        assertInstanceOf(Expression.Call.class, expr);
        var call = (Expression.Call) expr;
        assertInstanceOf(Expression.Identifier.class, call.callee());
        assertTrue(call.arguments().isEmpty());
    }

    @Test
    public void testCallWithArgs() throws IOException {
        var expr = parseExpr("add(1, 2)");
        assertInstanceOf(Expression.Call.class, expr);
        var call = (Expression.Call) expr;
        assertEquals(2, call.arguments().size());
    }

    @Test
    public void testChainedCalls() throws IOException {
        var expr = parseExpr("a()()");
        assertInstanceOf(Expression.Call.class, expr);
        var outer = (Expression.Call) expr;
        assertInstanceOf(Expression.Call.class, outer.callee());
    }

    @Test
    public void testMethodCall() throws IOException {
        var expr = parseExpr("obj.method(arg)");
        assertInstanceOf(Expression.Call.class, expr);
        var call = (Expression.Call) expr;
        assertInstanceOf(Expression.MemberAccess.class, call.callee());
        assertEquals(1, call.arguments().size());
    }

    // endregion

    // region Array indexing

    @Test
    public void testSimpleIndex() throws IOException {
        var expr = parseExpr("arr[0]");
        assertInstanceOf(Expression.Index.class, expr);
        var idx = (Expression.Index) expr;
        assertInstanceOf(Expression.Identifier.class, idx.receiver());
        assertInstanceOf(Expression.Literal.class, idx.index());
    }

    @Test
    public void testChainedIndex() throws IOException {
        var expr = parseExpr("matrix[row][col]");
        assertInstanceOf(Expression.Index.class, expr);
        var outer = (Expression.Index) expr;
        assertInstanceOf(Expression.Index.class, outer.receiver());
    }

    @Test
    public void testIndexWithExpression() throws IOException {
        var expr = parseExpr("arr[i + 1]");
        assertInstanceOf(Expression.Index.class, expr);
        assertInstanceOf(Expression.Binary.class, ((Expression.Index) expr).index());
    }

    // endregion

    // region Parenthesized grouping

    @Test
    public void testParenthesizedExpression() throws IOException {
        var expr = parseExpr("(a)");
        assertInstanceOf(Expression.Group.class, expr);
        assertInstanceOf(Expression.Identifier.class, ((Expression.Group) expr).inner());
    }

    @Test
    public void testGroupOverridesPrecedence() throws IOException {
        var expr = parseExpr("(a + b) * c");
        assertInstanceOf(Expression.Binary.class, expr);
        var mul = (Expression.Binary) expr;
        assertEquals("*", mul.operator().lexeme());
        assertInstanceOf(Expression.Group.class, mul.left());
    }

    // endregion

    // region Complex mixed expressions

    @Test
    public void testComplexPrecedence() throws IOException {
        var expr = parseExpr("a + b * c == d and e or f");
        assertInstanceOf(Expression.Binary.class, expr);
        var orExpr = (Expression.Binary) expr;
        assertEquals("or", orExpr.operator().lexeme());
    }

    @Test
    public void testCallOnNewExpr() throws IOException {
        var expr = parseExpr("new Foo().bar()");

        assertInstanceOf(Expression.Call.class, expr);
        var call = (Expression.Call) expr;
        assertInstanceOf(Expression.MemberAccess.class, call.callee());
        var ma = (Expression.MemberAccess) call.callee();
        assertInstanceOf(Expression.NewExpr.class, ma.receiver());
    }

    @Test
    public void testUnaryInsideBinaryInsideTernary() throws IOException {
        var expr = parseExpr("a > 0 ? -a : a * 2");
        assertInstanceOf(Expression.Ternary.class, expr);
        var tern = (Expression.Ternary) expr;
        assertInstanceOf(Expression.Binary.class, tern.condition());
        assertInstanceOf(Expression.Unary.class, tern.thenExpr());
        assertInstanceOf(Expression.Binary.class, tern.elseExpr());
    }

    // endregion

    // region Error cases

    @Test
    public void testEmptyExpressionFails() {
        assertThrows(CompileError.class, () -> parseExpr(""));
    }

    @Test
    public void testUnclosedParenFails() {
        assertThrows(CompileError.class, () -> parseExpr("(a + b"));
    }

    @Test
    public void testUnclosedBracketFails() {
        assertThrows(CompileError.class, () -> parseExpr("arr[0"));
    }

    @Test
    public void testMissingTernaryColonFails() {
        assertThrows(CompileError.class, () -> parseExpr("a ? b c"));
    }

    // endregion
}
