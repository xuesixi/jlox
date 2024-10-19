import java.util.ArrayList;
import java.util.List;

public class LoxParser {

    private final List<Token> tokens;
    private int current = 0;

    public LoxParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Stmt> parse() {
        ArrayList<Stmt> statements = new ArrayList<>();
        while (!isEnd()) {
            Stmt next = declaration();
            statements.add(next);
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(TokenType.VAR)) {
                return varDeclaration();
            } else {
                return statement();
            }
        } catch (ParseError e) {
            return null;
        }
    }

    private Stmt varDeclaration() {
        Token variable = consume(TokenType.IDENTIFIER, "An IDENTIFIER is needed after var" );
        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }
        consume(TokenType.SEMICOLON, "A semicolon is needed to terminate a statement");
        return new Stmt.Var(variable, initializer);
    }

    private Stmt statement() {
        if (match(TokenType.PRINT)) {
            return printStatement();
        } else {
            return expressionStatement();
        }
    }

    private Stmt printStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "A semicolon is needed to terminate a statement");
        return new Stmt.Print(expr);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "A semicolon is needed to terminate a statement");
        return new Stmt.Expression(expr);
    }

    /**
     * 從 expression 到 primary，所有這些函數都是做：已知接下來的 token 們可以被解析為那個東西，進行解析，並返回
     *
     * @return
     */
    private Expr expression() {
        return equality();
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }


    private Expr primary() {
        if (match(TokenType.TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(TokenType.FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(TokenType.NIL)) {
            return new Expr.Literal(null);
        }
        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Missing right parenthesis when parsing");
            return new Expr.Grouping(expr);
        }
        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        // unrecognized token
        throw error(peek(), "unrecognized token when parsing");
    }


    /**
     * 實際上，這才是那個真正推進匹配的函數。
     *
     * @param types
     * @return
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (peek().type == type) {
                current++;
                return true;
            }
        }
        return false;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token consume(TokenType type, String message) {
        if (match(type)) {
            return previous();
        }
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private boolean isEnd() {
        return tokens.get(current).type == TokenType.EOF;
    }

    private static class ParseError extends RuntimeException {

    }
}
