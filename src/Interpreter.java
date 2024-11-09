import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    public Environment global = new Environment(); // global 用来储存全局变量

    /**
     * native object 的父类是 null！
     */
    private final LoxInstance nativeObject = new LoxInstance((LoxClass) null);
    private final HashMap<Expr, Integer> locals = new HashMap<>(); // 每一个变量表达式所访问的变量的深度。
    private Environment environment = global;

    /**
     * native：提供一些底层函数
     * origin：所有类的父类
     * core：array 等内建特殊类
     * lib：并不特殊，但预先导入
     */
    public Interpreter() {
        setupNative();
        loadLoxOrigin();
        loadLoxCore();
        loadLoxLib();
    }

    /**
     * 运行一个语句列表
     *
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
     *
     * @param expr 想要求值的表达式
     * @return 该表达式的“值”
     */
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    /**
     * 运行一个语句
     *
     * @param stmt 想要运行的语句
     * @return 一般来说，为 Void
     */
    private Object execute(Stmt stmt) {
        return stmt.accept(this);
    }

    public void resolve(Expr expr, int distance) {
        locals.put(expr, distance);
    }

    /**
     * 根据 locals 中的结果，确定需要向外寻找的环境的层级
     * 如果 locals 中不存在对应的 key，那么动态地寻找
     */
    private Object lookupVariable(Expr expr, Token token) {
        Integer distance = locals.get(expr);
        if (distance == null) {
//            return global.get(token);
            return environment.get(token);
        } else {
            return environment.getAt(token, distance);
        }
    }

    /**
     * 切换到目标环境，执行语句，然后切换回原环境
     *
     * @param statements 要执行的语句
     * @param env        执行语句的环境
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
     *
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
     *
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

    /**
     *
     * @param value any object
     * @return -1 if not valid
     */
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
     *
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


    // 特指变量赋值。
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        return varAssignHelper(expr, expr.name, value);
    }

    public Object varAssignHelper(Expr expr, Token varName, Object value) {
        Integer distance = locals.get(expr);
        if (distance == null) {
//            global.assign(varName, value);
            environment.assign(varName, value);
        } else {
            environment.assignAt(varName, value, distance);
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
        if (!(called instanceof LoxCallable)) {
            throw new LoxRuntimeError(expr.paren, "the value " + stringify(called) + " is not callable");
        }
        LoxCallable callable = (LoxCallable) called; // not necessary function. Can be class, function, native (e.g. clock)
        if (callable.arity() != arguments.size()) {
            throw new LoxRuntimeError(expr.paren, "the callable " + stringify(called) + " expects " + callable.arity() + " arguments, but got " + arguments.size());
        }
        return callable.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object instance = evaluate(expr.object);
        if (instance instanceof LoxInstance) {
            return ((LoxInstance) instance).get(expr.name);
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
        Object value = evaluate(expr.value);
        return setHelper(expr.object, expr.name, value);
    }

    public Object setHelper(Expr targetObject, Token field, Object value) {
        Object instance = evaluate(targetObject);
        if (instance instanceof LoxInstance) {
            ((LoxInstance) instance).set(field, value);
            return value;
        }
        throw new LoxRuntimeError(field, "only object supports field setting");
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
            return ((LoxArray) arr).getAtIndex(index);
        } catch (IndexOutOfBoundsException e) {
            throw new LoxRuntimeError(expr.rightBracket, "%d is out of bound of %d".formatted(index, ((LoxArray) arr).getLength()));
        }

    }

    @Override
    public Object visitArraySetExpr(Expr.ArraySetExpr expr) {
        Object value = evaluate(expr.value);
        return arraySetHelper(expr.array, expr.index, expr.rightBracket, value);
    }

    public Object arraySetHelper(Expr arrExpr, Expr indexExpr, Token keyword, Object value) {
        Object arr = evaluate(arrExpr);
        Object indexValue = evaluate(indexExpr);
        if (!(arr instanceof LoxArray)) {
            throw new LoxRuntimeError(keyword, "%s is not a valid array".formatted(stringify(arr)));
        }
        int index = validUint(indexValue);
        if (index <= -1) {
            throw new LoxRuntimeError(keyword, "%s is not a valid index".formatted(stringify(indexValue)));
        }
        try {
            ((LoxArray) arr).setAtIndex(index, value);
            return value;
        } catch (IndexOutOfBoundsException e) {
            throw new LoxRuntimeError(keyword, "%d is out of bound of %d".formatted(index, ((LoxArray) arr).getLength()));
        }
    }

    @Override
    public Object visitTupleExpr(Expr.TupleExpr expr) {
        List<Object> valueList = new ArrayList<>();
        for (Expr e : expr.exprList) {
            valueList.add(evaluate(e));
        }
        return new LoxArray(valueList);
    }

    /**
     * 对形如 (a, (dog.name, arr[3]), b) = nums 的元组进行解构
     *
     * @param expr 形如 {@code  (a, (dog.name, arr[3]), b) = nums }的元组
     * @return {@code null}
     */
    @Override
    public Object visitTupleUnpackExpr(Expr.TupleUnpackExpr expr) {

        // 右侧的值必须是一个数组
        Object rightValue = evaluate(expr.right);
        if (!(rightValue instanceof LoxArray)) {
            throw new LoxRuntimeError(expr.equal, "The right value of tuple unpacking must be an array");
        }

        LoxArray arr = (LoxArray) rightValue;

        // 如果左侧的长度大于右侧，则出错
        int leftSize = expr.left.exprList.size();
        if (leftSize > arr.getLength()) {
            throw new LoxRuntimeError(expr.equal, "Unbalanced unpacking with left size %d and right size %d".formatted(leftSize, arr.getLength()));
        }

        // 将右侧的每一个值分别赋值给左侧的对应的值。
        for (int i = 0; i < leftSize; i++) {
            Expr left = expr.left.exprList.get(i); //
            Object value = arr.getAtIndex(i);
            if (left instanceof Expr.Variable) {
                // 如果左侧是变量，则进行变量赋值
                varAssignHelper(left, ((Expr.Variable) left).name, value);
            } else if (left instanceof Expr.Get) {
                // 如果左侧是对象取字段，那么修改对象字段
                Expr.Get temp = (Expr.Get) left;
                setHelper(temp.object, temp.name, value);
            } else if (left instanceof Expr.ArrayGetExpr) {
                // 如果左侧是数组取索引，那么修改数组对应索引
                Expr.ArrayGetExpr temp = (Expr.ArrayGetExpr) left;
                arraySetHelper(temp.array, temp.index, temp.rightBracket, value);
            } else if (left instanceof Expr.TupleExpr) {
                // 如果左侧是另一个元组，那么递归地进行赋值
                Expr.TupleExpr temp = (Expr.TupleExpr) left;
                Expr right = new Expr.Literal(value);
                Expr.TupleUnpackExpr newExp = new Expr.TupleUnpackExpr(temp, right, expr.equal);
                evaluate(newExp);
            } else {
                throw new LoxRuntimeError(expr.equal, "%s is not a valid assign target".formatted(stringify(evaluate(left))));
            }
        }
        return null;
    }

    @Override
    public Object visitNativeExpr(Expr.Native expr) {
        return nativeObject;
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        Object o = lookupVariable(expr, expr.superKeyword);
        if (!(o instanceof LoxClass)) {
            throw new LoxRuntimeError(expr.superKeyword, "The super refers to a non class object! This is a implementation error");
        }
        Object o1 = environment.get("this");
        if (!(o1 instanceof LoxInstance)) {
            throw new LoxRuntimeError(expr.superKeyword, "The this refers to a non instance object! This is a implementation error");
        }
        LoxClass superClass = (LoxClass) o;
        LoxInstance thisObject = (LoxInstance) o1;
        LoxFunction method = superClass.getMethod(expr.methodName.lexeme);
        if (method == null) {
            throw new LoxRuntimeError(expr.methodName, "the method does not exist in super");
        }else {
            return method.binding(thisObject);
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

        LoxClass superclass = null;

        if (stmt.superName != null) {
            Object superClassResult = evaluate(stmt.superName);
            if (superClassResult instanceof LoxClass) {
                superclass = (LoxClass) superClassResult;
            } else {
                throw new LoxRuntimeError(stmt.superName.name, "This cannot be used as super class");
            }
        }

        // class 內部存在一個新的環境，用於儲存靜態字段和靜態方法
        this.environment = new Environment(oldEnv);

        HashMap<String, LoxFunction> methods = new HashMap<>();
        HashMap<String, Object> staticFields = new HashMap<>();

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

        environment.define("super", superclass);

        LoxClass loxClass = new LoxClass(stmt.name.lexeme, methods, staticFields, superclass);
        this.environment = oldEnv;

        environment.define(stmt.name.lexeme, loxClass);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        Object value = evaluate(stmt.expression);
        if (Lox.repl) {
            // in repl mode, a non-assignment expression statement will print out the expression result
            if (value != null && ! Expr.isAssignment(stmt.expression)) {
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
        } else if (stmt.elseBranch != null) {
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

    @Override
    public Void visitVarTupleStmt(Stmt.VarTuple stmt) {
        // 将先左侧的所有值定义
        defineIdentifierTuple(stmt.tuple);
        // 再进行一次元组解构
        visitTupleUnpackExpr(new Expr.TupleUnpackExpr(stmt.tuple, stmt.initializer, stmt.equal));
        return null;
    }

    @Override
    public Void visitImportStmt(Stmt.Import stmt) {
        // the resolver assures that the path does not end with .lox

        String pathString = stmt.path.literal.toString();
        try {
            Environment moduleEnv = importModule(stmt.path.literal.toString());

            if (stmt.items.isEmpty()) {
                // 如果是 import "huhu"; 式的全部导入，那么在当前环境中创建一个 huhu 对象。
                LoxInstance module = new LoxInstance.LoxModule(pathString, moduleEnv);
                this.environment.define(pathString, module);
            } else {
                // 如果是 import "huhu": Animal, sayHello; 式的选择性导入，那么在当前环境中分别定义 Animal 和 sayHello
                for (Token item : stmt.items) {
                    this.environment.define(item.lexeme, moduleEnv.get(item));
                }
            }
        } catch (IOException e) {
            throw new LoxRuntimeError(stmt.path, "No module found at the given path");
        }
        return null;
    }

    /**
     * 该函数用于导入内建的特殊 lox 文件
     */
    private Environment importResource(String pathString) throws IOException {
        InputStream is = Interpreter.class.getResourceAsStream(pathString);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        char[] chars = new char[1024];
        int len;
        while ((len = br.read(chars)) != -1) {
            sb.append(new String(chars, 0, len));
        }
        String src = sb.toString();
        br.close();
        return runSrc(src);
    }

    /**
     * 该函数用于导入普通的模块
     */
    private Environment importModule(String pathString) throws IOException {
        // the resolver assures that the path does not end with .lox
        Path path = Path.of(pathString + ".lox");
        String moduleSrc = Files.readString(path);
        return runSrc(moduleSrc);
    }

    private Environment runSrc(String moduleSrc) {
        List<Token> tokens = new LoxScanner(moduleSrc).scanTokens();
        List<Stmt> statements = new LoxParser(tokens).parse();
        new LoxResolver(this).resolve(statements);

        Environment moduleEnv = new Environment();
        executeWithEnvironment(statements, moduleEnv);
        return moduleEnv;
    }

    /**
     * 对于形如 (a, (b, c), d) 的元组，把其中的每一个标识符都在当前环境中定义
     *
     * @param expr 形如 (a, (b, c), d) 的元组
     */
    private void defineIdentifierTuple(Expr.TupleExpr expr) {
        for (Expr e : expr.exprList) {
            if (e instanceof Expr.Variable) {
                environment.define(((Expr.Variable) e).name.lexeme, null);
            } else if (e instanceof Expr.TupleExpr) {
                defineIdentifierTuple((Expr.TupleExpr) e);
            } else {
                throw new LoxRuntimeError(null, "tuple var declare with invalid variable");
            }
        }
    }

    private void setupNative() {

        nativeObject.set("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return System.currentTimeMillis() / 1000.0;
            }

            public String toString() {
                return "<native: clock>";
            }
        });
        nativeObject.set("panic", new LoxCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                throw new LoxRuntimeError(null, arguments.getFirst().toString());
            }
            @Override
            public String toString() {
                return "<native: panic>";
            }
        });

        nativeObject.set("len", new LoxCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object arg = arguments.getFirst();
                if (arg instanceof LoxArray) {
                    return (double)((LoxArray) arg).getLength();
                } else if (arg instanceof String) {
                    return (double)((String) arg).length();
                } else {
                    return null;
                }
            }

            @Override
            public String toString() {
                return "<native: len>";
            }
        });

        nativeObject.set("charAt", new LoxCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                String str = (String) arguments.get(0);
                Double index = (Double) arguments.get(1);
                int i = validUint(index);
                if (i < 0) {
                    return null;
                }else {
                    return String.valueOf(str.charAt(i));
                }
            }
            @Override
            public String toString() {
                return "<native: charAt>";
            }
        });

        nativeObject.set("type", new LoxCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object o = arguments.getFirst();
                if (o instanceof Double) {
                    return "<Number>";
                } else if (o instanceof String) {
                    return "<String>";
                } else if (o instanceof Boolean) {
                    return "<Boolean>";
                } else if (o instanceof LoxFunction) {
                    return "<Function>";
                } else if (o instanceof LoxClass) {
                    return "<Class>";
                } else if (o instanceof LoxInstance) {
                    return "<%s>".formatted(((LoxInstance) o).getLoxClass().name);
                } else {
                    throw new LoxRuntimeError(null, "Invalid argument for native.type");
                }
            }
            @Override
            public String toString() {
                return "<native: type>";
            }
        });

        nativeObject.set("is", new LoxCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object o = arguments.get(0);
                if (!(o instanceof LoxInstance)) {
                    throw new LoxRuntimeError(null, "%s is not an object".formatted(stringify(o)));
                }
                Object className = arguments.get(1);
                if (!(className instanceof LoxClass)) {
                    throw new LoxRuntimeError(null, "%s is not a class".formatted(stringify(className)));
                }
                return ((LoxInstance) o).isInstanceOf(((LoxClass) className));
            }

            @Override
            public String toString() {
                return "<native: is>";
            }

        });

        nativeObject.set("has", new LoxCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object arg0 = arguments.get(0);
                Object arg1 = arguments.get(1);
                if (!(arg1 instanceof String)) {
                    throw new LoxRuntimeError(null, "%s needs to be a string".formatted(stringify(arg1)));
                }
                if (arg0 instanceof LoxInstance) {
                    return ((LoxInstance) arg0).contains(((String) arg1));
                } else {
                    throw new LoxRuntimeError(null, "%s is not an object".formatted(stringify(arg0)));
                }
            }
        });

    }

    private void loadLoxOrigin() {
        try {
            Environment moduleEnv = importResource("/resources/LoxOrigin.lox");
            Object origin = moduleEnv.get("Origin");
            LoxClass.origin = ((LoxClass) origin);
            this.environment.define("Origin", origin);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadLoxCore() {
        try {
            Environment moduleEnv = importResource("/resources/LoxCore.lox");
            Object arr = moduleEnv.get("Array");
            LoxArray.loxArrayClass = (LoxClass) arr;
            this.environment.define("Array", arr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadLoxLib() {
        try {
            Environment moduleEnv = importResource("/resources/LoxLib.lox");
            List<String> imported = List.of("enum", "range", "is", "type", "List");
            for (String name : imported) {
                this.environment.define(name, moduleEnv.get(name));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
