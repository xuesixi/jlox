public class RPNPrinter implements Expr.Visitor<String>{

    public String getRPN(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return combine(getRPN(expr.left), getRPN(expr.right), expr.operator.lexeme);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return combine("(", getRPN(expr.expression), ")");
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return combine(getRPN(expr.right), expr.operator.lexeme);
    }

    public static String combine(String... strings) {
        StringBuilder sb = new StringBuilder();
        for (String string : strings) {
            if (sb.length() == 0) {
                sb.append(string);
            } else {
                sb.append(" ").append(string);
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
                new Expr.Grouping(
                        new Expr.Binary(
                                new Expr.Literal(1),
                                new Token(TokenType.AND, "+", null, 1),
                                new Expr.Literal(2)
                        )
                ),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Binary(
                                new Expr.Literal(4),
                                new Token(TokenType.MINUS, "-", null, 1),
                                new Expr.Literal(3)
                        )
                )
        );

        System.out.println(new RPNPrinter().getRPN(expression));
    }
}
