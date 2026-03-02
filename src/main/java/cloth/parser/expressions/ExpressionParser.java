package cloth.parser.expressions;

import cloth.error.CommonErrors;
import cloth.file.SourceFile;
import cloth.lexer.Lexer;
import cloth.parser.ParserPart;
import cloth.parser.statements.TypeReferenceParser;
import cloth.token.IToken;
import cloth.token.Token;
import cloth.token.TokenKind;
import cloth.token.Tokens;
import cloth.token.span.SourceSpan;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;

/**
 * Pratt (top-down operator precedence) parser for Cloth expressions.
 * <p>
 * Binding powers (higher = tighter):
 * <pre>
 *   110  postfix: () [] .
 *   100  prefix:  new
 *    90  prefix:  - !
 *    80  infix:   * / %         (left)
 *    70  infix:   + -           (left)
 *    60  infix:   as            (left, rhs is type)
 *    50  infix:   &lt; &lt;= &gt; &gt;=     (left)
 *    40  infix:   == !=         (left)
 *    30  infix:   and           (left)
 *    20  infix:   or            (left)
 *    10  infix:   ?:            (right)
 * </pre>
 *
 * @author Wylan Shoemaker
 * @since 1.0.0
 */
public class ExpressionParser extends ParserPart<Expression> {

    public ExpressionParser(Lexer lexer, SourceFile file) {
        super(lexer, file);
    }

    @Override
    public Expression parse() {
        return parseExpression(0);
    }

    // region Pratt core

    @SneakyThrows
    public Expression parseExpression(int minBP) {
        Expression left = parsePrefix();

        while (true) {
            int leftBP = getInfixLeftBP();
            if (leftBP < 0 || leftBP < minBP) break;

            left = parseInfix(left, leftBP);
        }

        return left;
    }

    // endregion

    // region Prefix

    @SneakyThrows
    private Expression parsePrefix() {
        // Unary minus
        if (is(Tokens.Operator.Minus)) {
            IToken op = advance();
            Expression operand = parseExpression(90);
            return new Expression.Unary(op, operand,
                new SourceSpan(op.span().start(), operand.span().end()));
        }

        // Logical NOT
        if (is(Tokens.Operator.Bang)) {
            IToken op = advance();
            Expression operand = parseExpression(90);
            return new Expression.Unary(op, operand,
                new SourceSpan(op.span().start(), operand.span().end()));
        }

        // new Type(args)
        if (is(Tokens.Keyword.New)) {
            return parseNewExpression();
        }

        // Parenthesized group
        if (is(Tokens.Operator.LeftParen)) {
            IToken open = advance();
            Expression inner = parseExpression(0);
            IToken close = expect(Tokens.Operator.RightParen, CommonErrors.EXPECTED_CLOSE_PAREN, "Unclosed parenthesized expression.");
            return new Expression.Group(inner,
                new SourceSpan(open.span().start(), close.span().end()));
        }

        // this
        if (is(Tokens.Keyword.This)) {
            IToken token = advance();
            return new Expression.ThisExpr(token, token.span());
        }

        // Boolean literals
        if (is(Tokens.Keyword.True) || is(Tokens.Keyword.False)) {
            IToken token = advance();
            return new Expression.Literal(token, token.span());
        }

        // Null literal
        if (is(Tokens.Keyword.Null)) {
            IToken token = advance();
            return new Expression.Literal(token, token.span());
        }

        // Numeric / string literals
        if (is(TokenKind.Number) || is(TokenKind.String)) {
            IToken token = advance();
            return new Expression.Literal(token, token.span());
        }

        // Identifier
        if (is(TokenKind.Identifier)) {
            IToken token = advance();
            return new Expression.Identifier(token, token.span());
        }

        throw CommonErrors.EXPECTED_EXPRESSION.toError(peek().span(),
            "Expected a literal, identifier, or prefix operator.");
    }

    @SneakyThrows
    private Expression parseNewExpression() {
        IToken newToken = advance(); // consume 'new'
        TypeReferenceParser.TypeReference type =
            new TypeReferenceParser(getLexer(), getFile()).parse();

        List<Expression> args = parseArgumentList();
        IToken last = previous();
        return new Expression.NewExpr(type, args,
            new SourceSpan(newToken.span().start(), last.span().end()));
    }

    // endregion

    // region Infix / Postfix

    /**
     * Returns the left binding power for the current token when in infix position,
     * or {@code -1} if it is not an infix/postfix operator.
     */
    private int getInfixLeftBP() {
        if (is(TokenKind.Operator) || is(TokenKind.Punctuation)) {
            Token tok = (Token) peek();
            return switch (tok.operator()) {
                case Dot, LeftParen, LeftBracket -> 110;    // postfix
                case Star, Slash, Percent -> 80;            // multiplicative
                case Plus, Minus -> 70;                     // additive
                case Less, LessEqual, Greater, GreaterEqual -> 50;  // relational
                case Equal, NotEqual -> 40;                 // equality
                case Question -> 10;                        // ternary
                default -> -1;
            };
        }

        if (is(TokenKind.Keyword)) {
            Token tok = (Token) peek();
            return switch (tok.keyword()) {
                case As -> 60;
                case And -> 30;
                case Or -> 20;
                default -> -1;
            };
        }

        return -1;
    }

    @SneakyThrows
    private Expression parseInfix(Expression left, int leftBP) {
        // Postfix: member access
        if (is(Tokens.Operator.Dot)) {
            advance();
            IToken member = expect(TokenKind.Identifier, CommonErrors.EXPECTED_IDENTIFIER, "Expected member name after '.'.");
            return new Expression.MemberAccess(left, member,
                new SourceSpan(left.span().start(), member.span().end()));
        }

        // Postfix: function call
        if (is(Tokens.Operator.LeftParen)) {
            List<Expression> args = parseArgumentList();
            IToken last = previous();
            return new Expression.Call(left, args,
                new SourceSpan(left.span().start(), last.span().end()));
        }

        // Postfix: array index
        if (is(Tokens.Operator.LeftBracket)) {
            advance();
            Expression index = parseExpression(0);
            IToken close = expect(Tokens.Operator.RightBracket, CommonErrors.EXPECTED_CLOSE_BRACKET, "Unclosed array index expression.");
            return new Expression.Index(left, index,
                new SourceSpan(left.span().start(), close.span().end()));
        }

        // Ternary: cond ? then : else  (right-associative, rightBP = leftBP)
        if (is(Tokens.Operator.Question)) {
            advance();
            Expression thenExpr = parseExpression(0);
            expect(Tokens.Operator.Colon, CommonErrors.EXPECTED_COLON, "Ternary requires 'condition ? then : else'.");
            Expression elseExpr = parseExpression(leftBP); // right-assoc
            return new Expression.Ternary(left, thenExpr, elseExpr,
                new SourceSpan(left.span().start(), elseExpr.span().end()));
        }

        // Cast: expr as Type
        if (is(Tokens.Keyword.As)) {
            advance();
            TypeReferenceParser.TypeReference targetType =
                new TypeReferenceParser(getLexer(), getFile()).parse();
            return new Expression.Cast(left, targetType,
                new SourceSpan(left.span().start(), targetType.span().end()));
        }

        // Binary operators: consume operator, parse right side
        IToken op = advance();
        int rightBP = leftBP + 1; // left-associative
        Expression right = parseExpression(rightBP);
        return new Expression.Binary(left, op, right, new SourceSpan(left.span().start(), right.span().end()));
    }

    /**
     * Parses a parenthesized, comma-separated list of expression arguments.
     * Consumes the opening and closing parentheses.
     */
    @SneakyThrows
    private List<Expression> parseArgumentList() {
        expect(Tokens.Operator.LeftParen, CommonErrors.EXPECTED_OPEN_PAREN);

        var args = new ArrayList<Expression>();
        if (!is(Tokens.Operator.RightParen)) {
            do {
                args.add(parseExpression(0));
            } while (match(Tokens.Operator.Comma));
        }

        expect(Tokens.Operator.RightParen, CommonErrors.EXPECTED_CLOSE_PAREN);

        return args;
    }

}
