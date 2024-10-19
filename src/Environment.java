import java.util.HashMap;

public class Environment {
    private HashMap<String, Object> values = new HashMap<>();

    public Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        throw new LoxRuntimeError(name, "No such variable: " + name.lexeme);
    }

    public void define(String name, Object value) {
        values.put(name, value);
    }

//    public void assign(String name, Object value) {
//        if (values.containsKey(name)) {
//            values.put(name, value);
//        }else {
//            throw new LoxRuntimeError()
//        }
//    }
}
