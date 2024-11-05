import java.util.HashSet;
import java.util.List;
import java.util.Stack;

/**
 * <p>对于每个语句，判断它其中出现的变量的层级</p>
 * resolve 只关心变量。如果是一个链式语句，那么也只关心其中的第一个变量。
 * 比如说<pre>a.b().c.d()</pre>这样的语句，我们只resolve第一个 a。剩余的检查都发生在运行时。
 * 如果一个变量没有找到对应的层级，那么它会在运行时动态地被查找。
 */
public class LoxResolver implements Stmt.Visitor<Void>, Expr.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<HashSet<String >> scopes;
    private FunctionType functionType; // 进入函数时会被设置。如果在非函数预警下遇到了 return 语句，产生错误。
    private ClassType classType;

    public LoxResolver(Interpreter interpreter) {
        functionType = FunctionType.None;
        classType = ClassType.None;
        this.interpreter = interpreter;
        scopes = new Stack<>();
        scopes.add(new HashSet<>()); // 全局层
    }

    public void resolve(List<Stmt> stmts) {
        for (Stmt stmt : stmts) {
            resolve(stmt);
        }
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    /**
     * 从当前 scope 向外找，找到第一个同名变量后，在 {@link Interpreter}中记录下途径的层级
     * @param expr 变量出现的表达式。
     * @param token 想要 resolve 的变量的名字
     */
    private void resolveLocal(Expr expr, Token token) {
        for (int i = scopes.size() - 1 ; i >= 0; i--) {
            HashSet<String> env = scopes.get(i);
            if (env.contains(token.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
        if (!Lox.repl) {
            System.out.printf("Resolver Warning: the variable [%s] is not resolved, and left to runtime\n", token.lexeme);
        }
    }

    private void beginScope() {
        scopes.push(new HashSet<>());
    }

    private void endScope() {
        scopes.pop();
    }

    private void define(Token name) {
        if (!scopes.isEmpty()) {
            scopes.peek().add(name.lexeme);
        }
    }

    private void define(String name) {
        if (! scopes.isEmpty()) {
            scopes.peek().add(name);
        }
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr argument : expr.arguments) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.object);
        resolve(expr.value);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitFStringExpr(Expr.FString expr) {
        for (Expr e : expr.exprList) {
            resolve(e);
        }
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (classType != ClassType.Class) {
            Lox.resolvingError(expr.keyword.line, expr.keyword.lexeme, "the return keyword is only allowed inside a function");
            return null;
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitArrayCreationExpr(Expr.ArrayCreationExpr expr) {
        for (Expr len : expr.lengthList) {
            resolve(len);
        }
        return null;
    }

    @Override
    public Void visitArrayGetExpr(Expr.ArrayGetExpr expr) {
        resolve(expr.array);
        resolve(expr.index);
        return null;
    }

    @Override
    public Void visitArraySetExpr(Expr.ArraySetExpr expr) {
        resolve(expr.value);
        resolve(expr.array);
        resolve(expr.index);
        return null;
    }

    @Override
    public Void visitTupleExpr(Expr.TupleExpr expr) {
        for (Expr e : expr.exprList) {
            resolve(e);
        }
        return null;
    }

    @Override
    public Void visitTupleUnpackExpr(Expr.TupleUnpackExpr expr) {
        resolve(expr.right);
        resolve(expr.left);
        return null;
    }

    @Override
    public Void visitNativeExpr(Expr.Native expr) {
        return null;
    }

    /**
     * block具有新一层 scope
     * @param stmt block
     * @return null
     */
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        for (Stmt statement : stmt.statements) {
            resolve(statement);
        }
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        define(stmt.name);
        ClassType old = classType;
        classType = ClassType.Class;
        beginScope(); // 这一层是 class 的静态环境，其中储存着静态函数和静态变量
        for (Stmt.Var staticVariable : stmt.staticVariables) {
            resolve(staticVariable);
        }
        for (Stmt.Function staticMethod : stmt.staticMethods) {
            resolve(staticMethod);
        }
        beginScope(); // 这层环境中只有 this
        scopes.peek().add("this");
        for (Stmt.Function method : stmt.methods) {
            define(method.name);
            if (method.name.lexeme.equals("init")) {
                resolveFunction(method, FunctionType.Initializer);
            } else {
                resolveFunction(method, FunctionType.Function);
            }
        }
        endScope();
        endScope();
        classType = old;
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType oldType = functionType;
        functionType = type;
        beginScope();
        for (Token param : function.params) {
            define(param);
        }
        for (Stmt s : function.body) {
            resolve(s);
        }
        endScope();
        functionType = oldType;
    }

    /**
     * 函数定义有自己的 scope。参数定义在其中。
     * @param stmt function
     * @return null
     */
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        define(stmt.name);
        resolveFunction(stmt, FunctionType.Function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) {
            resolve(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (functionType == FunctionType.Initializer) {
            Lox.resolvingError(stmt.keyword.line, stmt.keyword.lexeme, "the return keyword is not allowed inside an initializer");
            return null;
        }
        if (functionType != FunctionType.Function) {
            Lox.resolvingError(stmt.keyword.line, stmt.keyword.lexeme, "the return keyword is only allowed inside a function");
            return null;
        }
        if (stmt.value != null) {
            resolve(stmt.value);
        }
        return null;
    }

    /**
     * 在当前 scope 中申明一个变量
     * @param stmt 变量申明语句
     * @return null
     */
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        if (scopes.isEmpty()) {
            return null;
        }
        if (scopes.peek().contains(stmt.name.lexeme)) {
            Lox.resolvingError(stmt.name.line, stmt.name.lexeme, "Cannot re-declare the same identifier in the same local scope");
            return null;
        }
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    /**
     * 用在 visitVarTupleStmt 中。如果tupleExpr 中存在非标识符元素，那么出错。
     * 由于我已经在 varTuple 的匹配中进行了限定，理论上这里不应该出现错误。
     * @param tupleExpr 一个仅有标识符或者标识符元祖的元祖
     */
    public void resolveVariableOnlyTuple(Expr.TupleExpr tupleExpr) {
//        for (Expr expr : tupleExpr.exprList) {
//            if (expr instanceof Expr.Variable || expr instanceof Expr.TupleExpr) {
//                resolve(expr);
//            } else {
//                Lox.resolvingError(-1, "tuple", "not valid variable to declare");
//            }
//        }
        for (Expr expr : tupleExpr.exprList) {
            if (expr instanceof Expr.Variable ) {
                define(((Expr.Variable) expr).name);
            } else if (expr instanceof Expr.TupleExpr) {
                resolve(expr);
            } else {
                Lox.resolvingError(-1, "tuple", "not valid variable to declare");
            }
        }
    }

    /**
     * var (a, b) = (1, 2)
     * var ((a, b), c ) = nums;
     */
    @Override
    public Void visitVarTupleStmt(Stmt.VarTuple stmt) {
        resolve(stmt.initializer);
        resolveVariableOnlyTuple(stmt.tuple);
        return null;
    }

    @Override
    public Void visitImportStmt(Stmt.Import stmt) {
        String moduleName = stmt.path.literal.toString();
        if (stmt.path.literal.toString().contains(".")) {
            Lox.resolvingError(stmt.path.line, moduleName, "The module should not contain dot");
        }
        if (stmt.items.isEmpty()) {
            define(moduleName);
        } else {
            for (Token item : stmt.items) {
                define(item.lexeme);
            }
        }
        return null;
    }

    private enum FunctionType {
        None,
        Function,
        Initializer,
    }

    private enum ClassType {
        None,
        Class,
    }
}
