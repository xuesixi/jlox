
import java.util.List;

public class LoxFunction implements LoxCallable{
    private final Stmt.Function declaration;
    private final Environment closure;
    private final  boolean isInitializer;

    public LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
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
        // initializer always returns the object itself (explicit return value is disallowed)
        if (isInitializer) {
            return closure.getAt("this", 0);
        }
        return returnValue;
    }

    public LoxFunction binding(LoxInstance instance) {
        Environment newEnv = new Environment(this.closure);
        newEnv.define("this", instance);
        return new LoxFunction(this.declaration, newEnv, isInitializer);
    }

    @Override
    public String toString() {
        return String.format("<function: %s>", declaration.name.lexeme);
    }
}
