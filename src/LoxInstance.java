import java.util.HashMap;

public class LoxInstance {
    private LoxClass loxClass;
    private HashMap<String, Object> fields = new HashMap<>();

    public LoxInstance(LoxClass loxClass) {
        this.loxClass = loxClass;
    }

    public Object get(Token field) {
        if (fields.containsKey(field.lexeme)) {
            return fields.get(field.lexeme);
        }
        LoxFunction original = loxClass.getMethod(field.lexeme);
        if (original == null) {
            throw new LoxRuntimeError(field, "the field or method does not exist");
        }
        return original.binding(this);
    }

    public void set(Token field, Object value) {
        fields.put(field.lexeme, value);
    }

    @Override
    public String toString() {
        return "<object: %s>".formatted(loxClass.name);
    }
}
