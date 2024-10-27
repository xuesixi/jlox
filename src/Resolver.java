import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

/**
 * 对于每个语句，判断它其中出现的变量的层级
 */
public class Resolver implements Stmt.Visitor<Void>, Expr.Visitor<Void> {
    private Interpreter interpreter;
    private Stack<HashSet<String >> scopes;
    private FunctionType functionType; // 进入函数时会被设置。如果在非函数预警下遇到了 return 语句，产生错误。

    public Resolver(Interpreter interpreter) {
        functionType = FunctionType.None;
        this.interpreter = interpreter;
        scopes = new Stack<>();
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
    }

    private void beginScope() {
        scopes.push(new HashSet<>());
    }

    private void endScope() {
        scopes.pop();
    }

    private void define(Token name) {
        if (scopes.isEmpty()) {
            return;
        } else {
            scopes.peek().add(name.lexeme);
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
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        resolveLocal(expr, expr.name);
        return null;
    }

    /**
     * block具有新一层 scope
     * @param stmt
     * @return
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
     * @param stmt
     * @return
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
     * @return
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

    private enum FunctionType {
        None,
        Function,
    }
}
