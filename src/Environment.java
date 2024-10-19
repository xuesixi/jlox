import java.util.HashMap;

public class Environment {
    private HashMap<String, Object> values = new HashMap<>();
    private Environment enclosing;

    public Environment() {
    }

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    public Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        } else if (enclosing != null) {
            return enclosing.get(name);
        } else {
            throw new LoxRuntimeError(name, "No such variable: " + name.lexeme);
        }
    }

    public void define(String name, Object value) {
        values.put(name, value);
    }

    public void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
        } else if (enclosing != null) {
            enclosing.assign(name, value);
        } else {
            throw new LoxRuntimeError(name, "the variable: " + name.lexeme + " does not exist");
        }
    }
}
