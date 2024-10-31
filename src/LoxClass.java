import java.util.HashMap;
import java.util.List;

public class LoxClass implements LoxCallable {
    final String name;
    final HashMap<String, LoxFunction> methods;

    LoxClass(String name, HashMap<String, LoxFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    public LoxFunction getMethod(String methodName) {
        return methods.get(methodName);
    }

    @Override
    public String toString() {
        return "<class: %s>".formatted(name);
    }

    @Override
    public int arity() {
        if (getMethod("init") != null) {
            return getMethod("init").arity();
        }
        return 0;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        LoxFunction initializer = getMethod("init");
        if (initializer != null) {
            initializer.binding(instance).call(interpreter, arguments);
        }
        return instance;
    }
}
