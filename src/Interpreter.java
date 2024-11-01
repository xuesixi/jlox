import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    public Environment global = new Environment(); // global 用来储存全局变量
    private final HashMap<Expr, Integer> locals = new HashMap<>(); // 每一个变量表达式所访问的变量的深度。
    private Environment environment = global;

    public Interpreter() {
        global.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return System.currentTimeMillis()/1000.0;
            }

            @Override
            public String toString() {
                return "<function: clock>";
            }
        });
    }

    /**
     * 运行一个语句列表
     * @param statementList 我们想要执行的语句列表，这个期间可能会出现运行时错误
     */
    public void interpret(List<Stmt> statementList) {
        try {
            for (Stmt stmt : statementList) {
                execute(stmt);
            }
        } catch (LoxRuntimeError e) {
            // 运行时有很多方法可能会产生运行时错误
            Lox.reportRuntimeError(e);
        }
    }

    /**
     * 对一个表达式求值
     * @param expr 想要求值的表达式
     * @return 该表达式的“值”
     */
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    /**
     * 运行一个语句
     * @param stmt 想要运行的语句
     * @return 一般来说，为 Void
     */
    private Object execute(Stmt stmt) {
        return stmt.accept(this);
    }

    public void resolve(Expr expr, int distance) {
        locals.put(expr, distance);
    }

    private Object lookupVariable(Expr expr, Token token) {
        Integer distance = locals.get(expr);
        if (distance == null) {
            return global.get(token);
        } else {
            return environment.getAt(token, distance);
        }
    }

    /**
     * 切换到目标环境，执行语句，然后切换回原环境
     * @param statements 要执行的语句
     * @param env 执行语句的环境
     */
    public void executeWithEnvironment(List<Stmt> statements, Environment env) {
        Environment old = this.environment;
        this.environment = env;
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = old;
        }
    }


    /**
     * 判断一个值是否为 Lox 意义上的“true”。只有 null 和 false 为 false，其他都是 true
     * @param a 想要判断的值
     * @return 是否为真
     */
    private boolean isTrue(Object a) {
        if (a == null) {
            return false;
        }
        if (a instanceof Boolean) {
            return (boolean) a;
        }
        return true;
    }

    /**
     * 判断两个“值”是否相等。它对 null 有特殊处理。两个 null 是相等的。
     * @param a 值1
     * @param b 值2
     * @return 是否相等
     */
    private boolean isEqual(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        } else {
            return a.equals(b);
        }
    }

    private void checkNumberOperand(Token operator, Object... operands) {
        for (Object operand : operands) {
            if (!(operand instanceof Double)) {
                throw new LoxRuntimeError(operator, "the operator expects number operand ");
            }
        }
    }

    private int validUint(Object value) {
        if (!(value instanceof Double)) {
            return -1;
        }
        Double d = (Double) value;
        if (d % 1 != 0) {
            return -1;
        } else {
            return d.intValue();
        }
    }

    /**
     * 对于一个“值”，返回它的字符串表达方式。
     * @param object 想要表达的值
     * @return 字符串表达
     */
    public static String stringify(Object object) {
        if (object == null)
            return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }


    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance == null) {
            global.assign(expr.name, value);;
        } else {
            environment.assignAt(expr.name, value, distance);
        }
        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        Token operator = expr.operator;
        switch (expr.operator.type) {
            case TokenType.PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                } else if (left instanceof String && right instanceof String) {
                    return left + (String) right;
                } else if (left instanceof String) {
                    return left + stringify(right);
                } else if (right instanceof String) {
                    return stringify(left) + right;
                }
                throw new LoxRuntimeError(operator, "the operands do not support addition");
            case TokenType.MINUS:
                checkNumberOperand(operator, left, right);
                return (double) left - (double) right;
            case TokenType.STAR:
                checkNumberOperand(operator, left, right);
                return (double) left * (double) right;
            case TokenType.SLASH:
                checkNumberOperand(operator, left, right);
                return (double) left / (double) right;
            case TokenType.GREATER:
                checkNumberOperand(operator, left, right);
                return (double) left > (double) right;
            case TokenType.GREATER_EQUAL:
                checkNumberOperand(operator, left, right);
                return (double) left >= (double) right;
            case TokenType.LESS:
                checkNumberOperand(operator, left, right);
                return (double) left < (double) right;
            case TokenType.LESS_EQUAL:
                checkNumberOperand(operator, left, right);
                return (double) left <= (double) right;
            case TokenType.EQUAL_EQUAL:
                return isEqual(left, right);
            case TokenType.BANG_EQUAL:
                return !isEqual(left, right);
        }
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            Object value = evaluate(argument);
            arguments.add(value);
        }
        Object called = evaluate(expr.callee);
        if (! (called instanceof LoxCallable)) {
            throw new LoxRuntimeError(expr.paren, "the value " + stringify(called) + " is not callable");
        }
        LoxCallable function = (LoxCallable) called;
        if (function.arity() != arguments.size()) {
            throw new LoxRuntimeError(expr.paren, "the callable " + stringify(called) + " expects " + function.arity() + " arguments, but got " + arguments.size());
        }
        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object instance = evaluate(expr.object);
        if (instance instanceof LoxInstance) {
            return ((LoxInstance) instance).get(expr.name);
        } else if (instance instanceof LoxArray && expr.name.lexeme.equals("length")) {
            int length = ((LoxArray) instance).getLength();
            return Double.valueOf(length);
        }
        throw new LoxRuntimeError(expr.name, "only object supports field getting");
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return expr.expression.accept(this);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        if (expr.operator.type == TokenType.AND && !isTrue(left)) {
            return left;
        } else if (expr.operator.type == TokenType.OR && isTrue(left)) {
            return left;
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object instance = evaluate(expr.object);
        Object value = evaluate(expr.value);
        if (instance instanceof LoxInstance) {
            ((LoxInstance) instance).set(expr.name, value);
            return value;
        }
        throw new LoxRuntimeError(expr.name, "only object supports field setting");
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        if (expr.operator.type == TokenType.BANG) {
            return !isTrue(right);
        } else if (expr.operator.type == TokenType.MINUS) {
            checkNumberOperand(expr.operator, right);
            return -(double) right;
        }
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookupVariable(expr, expr.name);
    }

    @Override
    public Object visitFStringExpr(Expr.FString expr) {
        Object[] values = new Object[expr.exprList.size()];
        for (int i = 0; i < expr.exprList.size(); i++) {
            values[i] = stringify(evaluate(expr.exprList.get(i)));
        }
        return expr.literal.formatted(values);
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookupVariable(expr, expr.keyword);
    }

    @Override
    public Object visitArrayCreationExpr(Expr.ArrayCreationExpr expr) {

        int[] dimensions = new int[expr.lengthList.size()];
        for (int i = 0; i < expr.lengthList.size(); i++) {
            Object length = evaluate(expr.lengthList.get(i));
            int len = validUint(length);
            if (len <= 0) {
                throw new LoxRuntimeError(expr.rightBracket, "%s is not a valid array length".formatted(stringify(length)));
            }
            dimensions[i] = len;
        }

        return new LoxArray(dimensions);
    }

    @Override
    public Object visitArrayGetExpr(Expr.ArrayGetExpr expr) {
        Object arr = evaluate(expr.array);
        Object indexValue = evaluate(expr.index);
        if (!(arr instanceof LoxArray)) {
            throw new LoxRuntimeError(expr.rightBracket, "%s is not a valid array".formatted(stringify(arr)));
        }
        int index = validUint(indexValue);
        if (index <= -1) {
            throw new LoxRuntimeError(expr.rightBracket, "%s is not a valid index".formatted(stringify(indexValue)));
        }
        try {
            return ((LoxArray) arr).atIndex(index);
        }catch (IndexOutOfBoundsException e) {
            throw new LoxRuntimeError(expr.rightBracket, "%d is out of bound of %d".formatted(index, ((LoxArray) arr).getLength()));
        }

    }

    @Override
    public Object visitArraySetExpr(Expr.ArraySetExpr expr) {
        Object value = evaluate(expr.value);
        Object arr = evaluate(expr.array);
        Object indexValue = evaluate(expr.index);
        if (!(arr instanceof LoxArray)) {
            throw new LoxRuntimeError(expr.rightBracket, "%s is not a valid array".formatted(stringify(arr)));
        }
        int index = validUint(indexValue);
        if (index <= -1) {
            throw new LoxRuntimeError(expr.rightBracket, "%s is not a valid index".formatted(stringify(indexValue)));
        }
        try {
            ((LoxArray) arr).setAtIndex(index, value);
            return value;
        }catch (IndexOutOfBoundsException e) {
            throw new LoxRuntimeError(expr.rightBracket, "%d is out of bound of %d".formatted(index, ((LoxArray) arr).getLength()));
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        Environment newEnv = new Environment(this.environment);
        executeWithEnvironment(stmt.statements, newEnv);
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        Environment oldEnv = environment;
        this.environment = new Environment(oldEnv);

        HashMap<String , LoxFunction> methods = new HashMap<>();
        HashMap<String , Object> staticFields = new HashMap<>();

        for (Stmt.Function method : stmt.methods) {
            LoxFunction function = new LoxFunction(method, this.environment, method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, function);
        }

        // 静态方法/字段既是 class 的字段，又处于 class 的环境之中。
        // 前者是为了类似 Math.PI 之类的访问
        // 后者是为了 class 内部其他方法的closure，
        for (Stmt.Function staticMethod : stmt.staticMethods) {
            LoxFunction function = new LoxFunction(staticMethod, environment, false);
            environment.define(staticMethod.name.lexeme, function); // 环境定义
            staticFields.put(staticMethod.name.lexeme, function); // 字段添加
        }

        for (Stmt.Var staticVariable : stmt.staticVariables) {
            Object value = null;
            if (staticVariable.initializer != null) {
                value = evaluate(staticVariable.initializer);
            }
            environment.define(staticVariable.name.lexeme, value); // 环境定义
            staticFields.put(staticVariable.name.lexeme, value); // 字段添加
        }

        LoxClass loxClass = new LoxClass(stmt.name.lexeme, methods, staticFields);
        this.environment = oldEnv;

        environment.define(stmt.name.lexeme, loxClass);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        Object value = evaluate(stmt.expression);
        if (Lox.repl) {
            // in repl mode, an expression statement will print out its result
            if (value != null) {
                System.out.println(stringify(value));
            }
        }
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment, false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTrue(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null){
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }
        throw new LoxRuntimeError.LoxReturn(stmt.keyword, value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTrue(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }
}
