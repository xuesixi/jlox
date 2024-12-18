import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoxParser {

    private final List<Token> tokens;
    private int current = 0; // 该指针指向了当前正在解析的那个 token。目前，它只会由 match 和 synchronize 两个函数移动

    public LoxParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * 从 tokens 中匹配语句。将所有语句添加到一个列表之中，并返回
     *
     * @return 从 tokens 解析出的中的所有语句列表。这个语句列表然后会被 interpreter 执行
     */
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
                if (match(TokenType.LEFT_PAREN)) {
                    return varTuple();
                } else {
                    return varDeclaration();
                }
            } else if (match(TokenType.CLASS)) {
                return classDeclaration();
            } else if (match(TokenType.FUN)) {
                return functionDeclaration();
            } else if (match(TokenType.IMPORT)) {
                return importDeclaration();
            } else {
                return statement();
            }
        } catch (ParseError e) {
            if (Lox.repl) {
                // repl 模式下，不再继续解析，而是直接出错。
                throw e;
            } else {
                synchronize(); // 如果出现了错误，那么 synchronize 函数会试图寻找到下一个语句的开头，然后继续匹配
                return null;
            }
        }
    }

    private Stmt importDeclaration() {
        Token path = consume(TokenType.STRING, "The module path is missing for import statement");
        List<Token> items = new ArrayList<>();
        HashMap<Token, String > aliasMap = new HashMap<>();
        String moduleAlias = null;
        if (match(TokenType.COLON)) {
            do {
                Token item = consume(TokenType.IDENTIFIER, "Here should be an identifier in the module");
                items.add(item);
                if (match(TokenType.AS)) {
                    Token alias = consume(TokenType.IDENTIFIER, "An identifier is needed after as");
                    aliasMap.put(item, alias.lexeme);
                }
            } while (!isEnd() && match(TokenType.COMMA));
        } else if (match(TokenType.AS)) {
            Token alias = consume(TokenType.IDENTIFIER, "An identifier is needed after as");
            moduleAlias = alias.lexeme;
        }
        consume(TokenType.SEMICOLON, " A semicolon is needed to terminate import statement");
        return new Stmt.Import(path, items, aliasMap, moduleAlias);
    }

    private Stmt.Var varDeclaration() {
        Token variable = consume(TokenType.IDENTIFIER, "An IDENTIFIER is needed after var");
        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }
        consume(TokenType.SEMICOLON, "Here is expected to be a semicolon to terminate a statement");
        return new Stmt.Var(variable, initializer);
    }

    private Stmt varTuple() {
        Expr.TupleExpr tuple = identifierTuple();
        Token equal = consume(TokenType.EQUAL, "tuple var needs to be initialized");
        Expr right = expression();
        return new Stmt.VarTuple(tuple, right, equal);
    }

    private Stmt classDeclaration() {
        Token className = consume(TokenType.IDENTIFIER, "A class expects a name");
        Expr.Variable superName = null;
        if (match(TokenType.COLON)) {
            Token sn = consume(TokenType.IDENTIFIER, "A super class is expected after : ");
            superName = new Expr.Variable(sn);
        }
        consume(TokenType.LEFT_BRACE, "A class expects a { after class name");
        List<Stmt.Function> methods = new ArrayList<>();
        List<Stmt.Function> staticMethods = new ArrayList<>();
        List<Stmt.Var> staticVariables = new ArrayList<>();
        while (!isEnd() && peek().type != TokenType.RIGHT_BRACE) {
            if (match(TokenType.STATIC)) {
                consume(TokenType.IDENTIFIER, "An IDENTIFIER is needed after static");
                if (peek().type == TokenType.LEFT_PAREN) {
                    current--;
                    staticMethods.add(functionDeclaration());
                } else {
                    current--;
                    staticVariables.add(varDeclaration());
                }
            } else {
                methods.add(functionDeclaration());
            }
        }
        consume(TokenType.RIGHT_BRACE, "A class is terminated by a }");
        return new Stmt.Class(className, methods, staticMethods, staticVariables, superName);
    }

    private Stmt statement() {
        // 如果此处应该有某个语句，但所有输入已经结束，且这是 REPL 模式，那么我们视为 repl pending
        if (isEnd() && Lox.repl) {
            throw new ReplPending();
        }
        if (match(TokenType.PRINT)) {
            return printStatement();
        } else if (match(TokenType.LEFT_BRACE)) {
            return new Stmt.Block(block());
        } else if (match(TokenType.IF)) {
            return ifStatement();
        } else if (match(TokenType.WHILE)) {
            return whileStatement();
        } else if (match(TokenType.FOR)) {
            return forStatement();
        } else if (match(TokenType.RETURN)) {
            return returnStatement();
        } else if (match(TokenType.WITH)) {
            return withEachStatement();
        } else {
            return expressionStatement();
        }
    }

    private Stmt returnStatement() {
        Token token = previous();
        Expr returnExpr;
        if (!match(TokenType.SEMICOLON)) {
            returnExpr = expression();
            consume(TokenType.SEMICOLON, "A semicolon is needed here");
        } else {
            returnExpr = null;
        }
        return new Stmt.Return(token, returnExpr);
    }

    private Stmt.Function functionDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "A function name is required");
        consume(TokenType.LEFT_PAREN, "A ( is required for function");
        List<Token> parameters = new ArrayList<>();
        if (!match(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    parseError(peek(), "More than 255 parameters");
                }
                parameters.add(consume(TokenType.IDENTIFIER, "a parameter is expected here"));
            } while (match(TokenType.COMMA));
            consume(TokenType.RIGHT_PAREN, "A ) is needed for function");
        }
        consume(TokenType.LEFT_BRACE, "A { is needed for function body");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    /**
     * 将
     * <pre>
     * for (initializer; condition; increment) {
     *      body
     * }
     * </pre>
     * 转化为:
     * <pre>
     * {
     *     initializer
     *     while (condition) {
     *         body
     *         increment
     *     }
     * }
     * </pre>
     *
     * @return while statement from for
     */
    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "A left parenthesis is required for for statement");
        Stmt initializer;
        if (match(TokenType.VAR)) {
            initializer = varDeclaration();
        } else if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else {
            initializer = expressionStatement();
        }

        Expr condition;
        if (match(TokenType.SEMICOLON)) {
            condition = new Expr.Literal(true);
        } else {
            condition = expression();
            consume(TokenType.SEMICOLON, "The middle semicolon for for statement");
        }

        Expr increment;
        if (match(TokenType.RIGHT_PAREN)) {
            increment = null;
        } else {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "A right parenthesis is required for for statement");

        Stmt body = statement();
        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        Stmt loop = new Stmt.While(condition, body);
        if (initializer != null) {
            return new Stmt.Block(Arrays.asList(initializer, loop));
        } else {
            return new Stmt.Block(List.of(loop));
        }
    }

    /**
     * transform
     * <pre>
     *     with num in arr
     *         body
     * </pre>
     * to
     * <pre>
     *     {
     *         var iter = arr.iter();
     *         while (iter.hasNext()) {
     *             var num = iter.next();
     *             body
     *         }
     *     }
     * </pre>
     */
    private Stmt withEachStatement() {
        Expr.TupleExpr leftTuple = null;
        Token leftToken = null;

        if (match(TokenType.LEFT_PAREN)) {
            leftTuple = identifierTuple();
        } else {
            leftToken = consume(TokenType.IDENTIFIER, "An identifier is needed for with statement");
        }

        Token tokenIN = consume(TokenType.IN, "An in is needed for with statement");
        Expr arr = expression();
        Stmt body = statement();

        Token iter_fun = new Token(TokenType.IDENTIFIER, "iter", null, tokenIN.line);
        Expr arr_iter = new Expr.Get(arr, iter_fun); // arr.iter

        Expr arr_iter_call = new Expr.Call(arr_iter, tokenIN, new ArrayList<>()); // arr.iter()

        Token var_iter = new Token(TokenType.IDENTIFIER, "iter", null, tokenIN.line);
        Stmt var_iter_stmt = new Stmt.Var(var_iter, arr_iter_call); // var iter = arr.iter()

        Expr iter_1 = new Expr.Variable(var_iter);
        Token hasNext = new Token(TokenType.IDENTIFIER, "hasNext", null, tokenIN.line);
        Expr iter_hasNext = new Expr.Get(iter_1, hasNext); // iter.hasNext
        Expr condition = new Expr.Call(iter_hasNext, tokenIN, new ArrayList<>()); // iter.hasNext()

        Expr iter_2 = new Expr.Variable(var_iter);
        Token next = new Token(TokenType.IDENTIFIER, "next", null, tokenIN.line);
        Expr iter_next = new Expr.Get(iter_2, next); // iter.next
        Expr iter_next_call = new Expr.Call(iter_next, tokenIN, new ArrayList<>()); // iter.next()

        Stmt loop_variable;
        if (leftToken != null) {
            loop_variable = new Stmt.Var(leftToken, iter_next_call);
        } else {
            loop_variable = new Stmt.VarTuple(leftTuple, iter_next_call, tokenIN);
        }

        Stmt realBody = new Stmt.Block(List.of(loop_variable, body));

        Stmt whileStmt = new Stmt.While(condition, realBody);

        return new Stmt.Block(List.of(var_iter_stmt, whileStmt));
    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "A left parenthesis is required for while statement");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "A right parenthesis is required for while statement");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "A left parenthesis is required for if statement");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "A right parenthesis is required for if statement");
        Stmt thenStmt = statement();
        Stmt elseStatement = null;
        if (match(TokenType.ELSE)) {
            elseStatement = statement();
        }
        return new Stmt.If(condition, thenStmt, elseStatement);
    }

    private List<Stmt> block() {
        List<Stmt> stmts = new ArrayList<>();
        while (peek().type != TokenType.RIGHT_BRACE && !isEnd()) {
            stmts.add(declaration());
        }
        consume(TokenType.RIGHT_BRACE, "The closing brace is not found");
        return stmts;
    }

    private Stmt printStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Here is expected to be a semicolon to terminate a statement");
        return new Stmt.Print(expr);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Here is expected to be a semicolon to terminate a statement");
        return new Stmt.Expression(expr);
    }

    /**
     * 從 expression 到 primary，所有這些函數都是做：已知接下來的 token 們可以被解析為那個東西，進行解析，並返回
     *
     * @return any expression
     */
    private Expr expression() {
        return assignment();
    }

    /**
     * 所有赋值语句
     *
     * @return 变量赋值语句，对象字段赋值语句，数组赋值语句
     */
    private Expr assignment() {
        Expr expr = or();
        if (match(TokenType.EQUAL)) {
            Token equal = previous();
            if (expr instanceof Expr.Variable) {
                Expr value = assignment();
                return new Expr.Assign(((Expr.Variable) expr).name, value);
            } else if (expr instanceof Expr.Get) {
                Expr value = assignment();
                Expr.Get get = (Expr.Get) expr;
                return new Expr.Set(get.object, get.name, value);
            } else if (expr instanceof Expr.ArrayGetExpr) {
                Expr value = assignment();
                Expr.ArrayGetExpr access = (Expr.ArrayGetExpr) expr;
                return new Expr.ArraySetExpr(access.array, access.index, value, access.rightBracket);
            } else if (expr instanceof Expr.TupleExpr) {
                Expr right = assignment();
                return new Expr.TupleUnpackExpr((Expr.TupleExpr) expr, right, equal);
            }
            parseError(equal, "Invalid assignment target");
        } else if (match(TokenType.PLUS_EQUAL, TokenType.MINUS_EQUAL, TokenType.STAR_EQUAL, TokenType.SLASH_EQUAL)) {
            Token just = previous();
            Token operator = switch (just.type) {
                case PLUS_EQUAL -> new Token(TokenType.PLUS, "+", null, just.line);
                case MINUS_EQUAL -> new Token(TokenType.MINUS, "-", null, just.line);
                case STAR_EQUAL -> new Token(TokenType.STAR, "*", null, just.line);
                case SLASH_EQUAL -> new Token(TokenType.SLASH, "/", null, just.line);
                default -> null;
            };
            Expr right = expression();
            Expr value = new Expr.Binary(expr, operator, right);
            if (expr instanceof Expr.Variable) {
                return new Expr.Assign(((Expr.Variable) expr).name, value);
            } else if (expr instanceof Expr.Get) {
                return new Expr.Set(((Expr.Get) expr).object, ((Expr.Get) expr).name, value);
            } else if (expr instanceof Expr.ArrayGetExpr) {
                return new Expr.ArraySetExpr(((Expr.ArrayGetExpr) expr).array, ((Expr.ArrayGetExpr) expr).index, value, ((Expr.ArrayGetExpr) expr).rightBracket);
            }
        } else if (match(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)) {
            Token just = previous();
            Token operator = switch (just.type) {
                case PLUS_PLUS -> new Token(TokenType.PLUS, "+", null, just.line);
                case MINUS_MINUS -> new Token(TokenType.MINUS, "-", null, just.line);
                default -> null;
            };
            Expr value = new Expr.Binary(expr, operator, new Expr.Literal(1.0));
            if (expr instanceof Expr.Variable) {
                return new Expr.Assign(((Expr.Variable) expr).name, value);
            } else if (expr instanceof Expr.Get) {
                return new Expr.Set(((Expr.Get) expr).object, ((Expr.Get) expr).name, value);
            } else if (expr instanceof Expr.ArrayGetExpr) {
                return new Expr.ArraySetExpr(((Expr.ArrayGetExpr) expr).array, ((Expr.ArrayGetExpr) expr).index, value, ((Expr.ArrayGetExpr) expr).rightBracket);
            }
        }
        return expr;
    }

    private Expr or() {
        Expr expr = and();
        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();
        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
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
        return call();
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(TokenType.DOT)) {
                Token property = consume(TokenType.IDENTIFIER, "you need to specify a property");
                expr = new Expr.Get(expr, property);
            } else if (match(TokenType.LEFT_BRACKET)) {
                Expr index = expression();
                Token rightBracket = consume(TokenType.RIGHT_BRACKET, "] is missing for array access");
                expr = new Expr.ArrayGetExpr(expr, index, rightBracket);
            } else {
                break;
            }
        }
        return expr;
    }

    /**
     * @param callee 函数的本体. 比如 abc()() 中的 abc
     * @return 包含连续的()的函数调用。最后一个右括号已被消费。
     */
    private Expr finishCall(Expr callee) {
        ArrayList<Expr> arguments = new ArrayList<>();
        if (peek().type != TokenType.RIGHT_PAREN) {
            do {
                if (arguments.size() >= 255) {
                    parseError(peek(), "More than 255 arguments are passed into a function");
                }
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }
        Token paren = consume(TokenType.RIGHT_PAREN, "a function call needs to end with a )");
        return new Expr.Call(callee, paren, arguments);
    }


    /**
     * 匹配并消耗最基础的表达式单位。大部分错误由这里产生。
     *
     * @return 下一个最基础的表达式单位
     */
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
            if (match(TokenType.COMMA)) {
                List<Expr> exprList = new ArrayList<>();
                exprList.add(expr);
                return generalTuple(exprList);
            }
            consume(TokenType.RIGHT_PAREN, "Missing right parenthesis");
            return new Expr.Grouping(expr);
        }
        if (match(TokenType.IDENTIFIER)) {
            if (previous().lexeme.equalsIgnoreCase("f") && peek().type == TokenType.STRING) {
                return fstring();
            }
            return new Expr.Variable(previous());
        }
        if (match(TokenType.THIS)) {
            return new Expr.This(previous());
        }
        if (match(TokenType.LEFT_BRACKET)) {
            List<Expr> exprList = arrayCreation();
            return new Expr.ArrayCreationExpr(exprList, previous());
        }
        if (match(TokenType.NATIVE)) {
            return new Expr.Native(previous());
        }
        if (match(TokenType.SUPER)) {
            Token superKeyword = previous();
            consume(TokenType.DOT, "A dot is always needed after super");
            Token methodName = consume(TokenType.IDENTIFIER, "An method name is always needed after super");
            return new Expr.Super(superKeyword, methodName);
        }
        // unrecognized token
        throw parseError(peek(), "The token is at the inappropriate position");
    }

    /**
     * 匹配形如 a, b+c, d(), (e)) 的元组。
     *
     * @return 一个包含可以包含任何表达式的元祖。
     */
    private Expr.TupleExpr generalTuple(List<Expr> exprList) {
        do {
            exprList.add(expression());
        } while (!isEnd() && match(TokenType.COMMA));
        consume(TokenType.RIGHT_PAREN, "Tuple needs to have a )");
        return new Expr.TupleExpr(exprList);
    }

    /**
     * 匹配形如 a, (b, c) ) 的元祖
     *
     * @return 仅仅包含标识符的 tuple 表达式。用在变量定义之中
     */
    private Expr.TupleExpr identifierTuple() {
        List<Expr> exprList = new ArrayList<>();
        do {
            if (peek().type == TokenType.IDENTIFIER) {
                exprList.add(primary());
            } else {
                exprList.add(identifierTuple());
            }
        } while (!isEnd() && match(TokenType.COMMA));
        consume(TokenType.RIGHT_PAREN, "A tuple needs to end with )");
        return new Expr.TupleExpr(exprList);
    }


    /**
     * 现有的问题：仅用正则表达式无法处理嵌套的{}，可以考虑改成栈式解析器
     *
     * @return fstring
     */
    private Expr fstring() {
        Token str = consume(TokenType.STRING, "as FString, here should be a string");
        String literal = str.literal.toString();
        String new_literal = literal.replaceAll("\\{.*?}", "%s");
        List<Expr> exprList = new ArrayList<>();

        Pattern pattern = Pattern.compile("\\{(.*?)}");
        Matcher matcher = pattern.matcher(literal);

        while (matcher.find()) {
            String exp = matcher.group(1);
            LoxScanner scanner = new LoxScanner(exp);
            LoxParser parser = new LoxParser(scanner.scanTokens());
            Expr expr = parser.expression(); // 对于每个{}中的内容，我们只读取第一个表达式
            exprList.add(expr);
        }
        return new Expr.FString(new_literal, exprList);
    }

    /**
     * @return 形如 a][b][c] 的表达式
     */
    private List<Expr> arrayCreation() {
        List<Expr> exprList = new ArrayList<>();
        do {
            Expr length = expression();
            exprList.add(length);
            consume(TokenType.RIGHT_BRACKET, "] is needed for array creation");
        } while (!isEnd() && match(TokenType.LEFT_BRACKET));
        return exprList;
    }


    /**
     * 實際上，這才是那個真正推進匹配的函數。
     *
     * @param types 我们想要匹配的 token 的类型。
     * @return 如果下一个 token 是给定的 types 中的任意一个，那么将其消耗，然后返回 true。否则，不消耗，返回 false；
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

    /**
     * 断言下一个token 是某个类型，如果错误，则抛出 {@link ParseError}
     *
     * @param type    断言的类型
     * @param message 如果失败，给出的错误信息
     * @return 如果断言成功，返回刚才的那个 token
     */
    private Token consume(TokenType type, String message) {
        if (match(type)) {
            return previous();
        }
        if (Lox.repl && isEnd()) {
            // 如果要的是分号，那么自动补全它
            if (type == TokenType.SEMICOLON) {
                return new Token(TokenType.SEMICOLON, ";", null, -1);
            } else {
                throw new ReplPending();
            }
        }
        throw parseError(peek(), message);
    }

    /**
     * 返回一个 {@link ParseError}，值得注意的是，该函数本身并不会直接将其抛出
     *
     * @param token   出现错误的 {@link Token}
     * @param message 错误信息
     * @return 一个代表错误的 error 对象
     */
    private ParseError parseError(Token token, String message) {
        Lox.parsingError(token.line, token.lexeme, message);
        return new ParseError();
    }

    /**
     * 当遇到 {@link ParseError}的时候，移动 current 指针，直到下一个分号，或者下一个我们认为很可能是语句开头的 token
     * 这用于处理错误，防止因为一个错误而将所有其他语句认为也是错误的。
     */
    private void synchronize() {
        current++;
        while (!isEnd()) {
            if (previous().type == TokenType.SEMICOLON) {
                return;
            }
            // 下面这些token 被认为大概率是一个语句开始的标志，因此我们停留在这里，开始下一次的 parse
            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            current++;
        }
    }

    private boolean isEnd() {
        if (current >= tokens.size()) {
            return true;
        }
        return tokens.get(current).type == TokenType.EOF;
    }

    static class ParseError extends RuntimeException {

    }

    /**
     * 在 REPL 模式下，我们会在某些情况下抛出该异常。runPrompt 中会处理这个异常。
     */
    static class ReplPending extends RuntimeException {

    }
}
