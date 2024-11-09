import java.util.List;

public abstract class Expr {
  interface Visitor<R> {
    R visitAssignExpr(Assign expr);
    R visitBinaryExpr(Binary expr);
    R visitCallExpr(Call expr);
    R visitGetExpr(Get expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitLogicalExpr(Logical expr);
    R visitSetExpr(Set expr);
    R visitUnaryExpr(Unary expr);
    R visitVariableExpr(Variable expr);
    R visitFStringExpr(FString expr);
    R visitThisExpr(This expr);
    R visitArrayCreationExpr(ArrayCreationExpr expr);
    R visitArrayGetExpr(ArrayGetExpr expr);
    R visitArraySetExpr(ArraySetExpr expr);
    R visitTupleExpr(TupleExpr expr);
    R visitTupleUnpackExpr(TupleUnpackExpr expr);
    R visitNativeExpr(Native expr);
    R visitSuperExpr(Super expr);
  }
  public static class Assign extends Expr {
    Assign(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitAssignExpr(this);
    }

    final Token name;
    final Expr value;
  }
  public static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }
  public static class Call extends Expr {
    Call(Expr callee, Token paren, List<Expr> arguments) {
      this.callee = callee;
      this.paren = paren;
      this.arguments = arguments;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitCallExpr(this);
    }

    final Expr callee;
    final Token paren;
    final List<Expr> arguments;
  }
  public static class Get extends Expr {
    Get(Expr object, Token name) {
      this.object = object;
      this.name = name;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGetExpr(this);
    }

    final Expr object;
    final Token name;
  }
  public static class Grouping extends Expr {
    Grouping(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }

    final Expr expression;
  }
  public static class Literal extends Expr {
    Literal(Object value) {
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }

    final Object value;
  }
  public static class Logical extends Expr {
    Logical(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLogicalExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }
  public static class Set extends Expr {
    Set(Expr object, Token name, Expr value) {
      this.object = object;
      this.name = name;
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitSetExpr(this);
    }

    final Expr object;
    final Token name;
    final Expr value;
  }
  public static class Unary extends Expr {
    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }

    final Token operator;
    final Expr right;
  }
  public static class Variable extends Expr {
    Variable(Token name) {
      this.name = name;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVariableExpr(this);
    }

    final Token name;
  }

  public static class FString extends Expr {
    List<Expr> exprList;
    String literal;

    public FString(String literal, List<Expr> exprList) {
      this.literal = literal;
      this.exprList = exprList;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFStringExpr(this);
    }
  }

  public static class This extends Expr {
    Token keyword;

    public This(Token keyword) {
      this.keyword = keyword;
    }


    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitThisExpr(this);
    }
  }

  public static class ArrayCreationExpr extends Expr {
    List<Expr> lengthList;
    Token rightBracket;

    public ArrayCreationExpr(List<Expr> lengthList, Token rightBracket) {
      this.lengthList = lengthList;
      this.rightBracket = rightBracket;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitArrayCreationExpr(this);
    }
  }

  public static class ArrayGetExpr extends Expr {

    Expr array;
    Expr index;
    Token rightBracket;

    public ArrayGetExpr(Expr array, Expr index, Token rightBracket) {
      this.array = array;
      this.index = index;
      this.rightBracket = rightBracket;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitArrayGetExpr(this);
    }
  }

  public static class ArraySetExpr extends Expr {
    Expr array;
    Expr index;
    Expr value;
    Token rightBracket;

    public ArraySetExpr(Expr array, Expr index, Expr value, Token rightBracket) {
      this.array = array;
      this.index = index;
      this.value = value;
      this.rightBracket = rightBracket;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitArraySetExpr(this);
    }
  }

  public static class TupleExpr extends Expr {

    List<Expr> exprList;

    public TupleExpr(List<Expr> exprList) {
      this.exprList = exprList;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitTupleExpr(this);
    }
  }

  public static class TupleUnpackExpr extends Expr {

    Expr.TupleExpr left;
    Expr right;
    Token equal;

    public TupleUnpackExpr(TupleExpr left, Expr right, Token equal) {
      this.left = left;
      this.right = right;
      this.equal = equal;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitTupleUnpackExpr(this);
    }
  }

  public static class Native extends Expr {
    Token keyword;

    public Native(Token keyword) {
      this.keyword = keyword;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitNativeExpr(this);
    }
  }

  public static class Super extends Expr {
    Token superKeyword;
    Token methodName;

    public Super(Token superKeyword, Token methodName) {
      this.superKeyword = superKeyword;
      this.methodName = methodName;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitSuperExpr(this);
    }
  }

  public static boolean isAssignment(Expr expr) {
    return (expr instanceof Assign || expr instanceof Set || expr instanceof ArraySetExpr);
  }

  abstract <R> R accept(Visitor<R> visitor);
}
