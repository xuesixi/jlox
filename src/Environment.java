import java.util.HashMap;

public class Environment {
    public final HashMap<String, Object> values = new HashMap<>();
    private Environment enclosing;

    public String getDir() {
        return dir;
    }

    private String dir;

    public Environment() {
    }

    public Environment(String dir) {
        this.dir = dir;
    }

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
        this.dir = enclosing.dir;
    }

    public Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        } else if (enclosing != null) {
            return enclosing.get(name);
        } else {
            throw new LoxRuntimeError(name, "no such variable");
        }
    }

    public Object get(String name) {
        if (values.containsKey(name)) {
            return values.get(name);
        } else if (enclosing != null) {
            return enclosing.get(name);
        } else {
            throw new LoxRuntimeError(null, "no such variable: " + name);
        }
    }

    public Object getAt(String name, int distance) {
        Environment curr = this;
        for (int i = 0; i < distance; i++) {
            curr = curr.enclosing;
        }
        if (curr.values.containsKey(name)) {
            return curr.values.get(name);
        } else {
            throw new LoxRuntimeError(null, "no such variable/field: " + name);
        }
    }

    public Object getAt(Token name, int distance) {
        return getAt(name.lexeme, distance);
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
            throw new LoxRuntimeError(name, "the variable does not exist");
        }
    }

    public void assignAt(Token name, Object value, int distance) {
        Environment curr = this;
        for (int i = 0; i < distance; i++) {
            curr = curr.enclosing;
        }
        curr.values.put(name.lexeme, value);
    }
}
