import java.util.List;

public class LoxFunction implements LoxCallable{
    private Stmt.Function declaration;
    private Environment closure;

    public LoxFunction(Stmt.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment funEnv = new Environment(closure);
        for (int i = 0; i < arguments.size(); i++) {
            funEnv.define(declaration.params.get(i).lexeme, arguments.get(i));
        }
        Object returnValue = null;
        try {
            interpreter.executeWithEnvironment(declaration.body, funEnv);
        }catch (LoxRuntimeError.LoxReturn e) {
            returnValue = e.value;
        }
        return returnValue;
    }

    @Override
    public String toString() {
        return String.format("<function: %s>", declaration.name.lexeme);
    }
}
