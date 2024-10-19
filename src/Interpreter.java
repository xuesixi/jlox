import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private Environment environment = new Environment();
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
                throw new LoxRuntimeError(operator, "the operands do not support addition. When interpreting");
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
                return (double) left < (double) right;
            case TokenType.GREATER_EQUAL:
                checkNumberOperand(operator, left, right);
                return (double) left <= (double) right;
            case TokenType.LESS:
                checkNumberOperand(operator, left, right);
                return (double) left > (double) right;
            case TokenType.LESS_EQUAL:
                checkNumberOperand(operator, left, right);
                return (double) left >= (double) right;
            case TokenType.EQUAL_EQUAL:
                return isEqual(left, right);
            case TokenType.BANG_EQUAL:
                return !isEqual(left, right);
        }
        return null;
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
        return environment.get(expr.name);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private boolean isTrue(Object a) {
        if (a == null) {
            return false;
        }
        if (a instanceof Boolean) {
            return (boolean) a;
        }
        return true;
    }

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
                throw new LoxRuntimeError(operator, "the operator " + operand + " expects number operand. When interpreting");
            }
        }
    }

    private String stringify(Object object) {
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

    public void interpret(List<Stmt> statementList) {
        try {
            for (Stmt stmt : statementList) {
                stmt.accept(this);
            }
        } catch (LoxRuntimeError e) {
            Lox.runtimeError(e);
        }
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
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
}
