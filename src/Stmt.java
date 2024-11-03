
import java.util.List;

public abstract class Stmt {
  interface Visitor<R> {
    R visitBlockStmt(Block stmt);
    R visitClassStmt(Class stmt);
    R visitExpressionStmt(Expression stmt);
    R visitFunctionStmt(Function stmt);
    R visitIfStmt(If stmt);
    R visitPrintStmt(Print stmt);
    R visitReturnStmt(Return stmt);
    R visitVarStmt(Var stmt);
    R visitWhileStmt(While stmt);
    R visitVarTupleStmt(VarTuple stmt);
    R visitImportStmt(Import stmt);
  }
  public static class Block extends Stmt {
    Block(List<Stmt> statements) {
      this.statements = statements;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }

    final List<Stmt> statements;
  }
  public static class Class extends Stmt {
    Class(Token name, List<Stmt.Function> methods, List<Stmt.Function> staticMethods, List<Stmt.Var> staticVariables) {
      this.name = name;
      this.methods = methods;
      this.staticVariables = staticVariables;
      this.staticMethods = staticMethods;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitClassStmt(this);
    }

    final Token name;
    final List<Stmt.Function> methods;
    final List<Stmt.Function> staticMethods;
    final List<Stmt.Var> staticVariables;
  }
  public static class Expression extends Stmt {
    Expression(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }

    final Expr expression;
  }
  public static class Function extends Stmt {
    Function(Token name, List<Token> params, List<Stmt> body) {
      this.name = name;
      this.params = params;
      this.body = body;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFunctionStmt(this);
    }

    final Token name;
    final List<Token> params;
    final List<Stmt> body;
  }
  public static class If extends Stmt {
    If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }

    final Expr condition;
    final Stmt thenBranch;
    final Stmt elseBranch;
  }
  public static class Print extends Stmt {
    Print(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }

    final Expr expression;
  }
  public static class Return extends Stmt {
    Return(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitReturnStmt(this);
    }

    final Token keyword;
    final Expr value;
  }
  public static class Var extends Stmt {
    Var(Token name, Expr initializer) {
      this.name = name;
      this.initializer = initializer;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarStmt(this);
    }

    final Token name;
    final Expr initializer;
  }
  public static class While extends Stmt {
    While(Expr condition, Stmt body) {
      this.condition = condition;
      this.body = body;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }

    final Expr condition;
    final Stmt body;
  }

  /**
   *  var (a, b) = (1, 2) 的语句
   */
  public static class VarTuple extends Stmt {
    Expr.TupleExpr tuple;
    Expr initializer;
    Token equal;

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarTupleStmt(this);
    }

    public VarTuple(Expr.TupleExpr tuple, Expr initializer, Token equal) {
      this.tuple = tuple;
      this.initializer = initializer;
      this.equal = equal;
    }
  }

  public static class Import extends Stmt {
    Token path;
    List<Token> items;

    public Import(Token path, List<Token> items) {
      this.path = path;
      this.items = items;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitImportStmt(this);
    }
  }

  abstract <R> R accept(Visitor<R> visitor);
}
