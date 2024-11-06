import java.util.HashMap;

public class LoxInstance {
    private final LoxClass loxClass;
    private HashMap<String, Object> fields = new HashMap<>();

    public LoxInstance(LoxClass loxClass) {
        this.loxClass = loxClass;
    }

    public LoxInstance(HashMap<String, Object> fields) {
        this((LoxClass) null);
        this.fields = fields;
    }

    public boolean contains(String field) {
        if (fields.containsKey(field)) {
            return true;
        }
        if (loxClass == null) {
            return false;
        }
        return loxClass.getMethod(field) != null;
    }

    public Object get(Token field) {
//        if (fields.containsKey(field.lexeme)) {
//            return fields.get(field.lexeme);
//        }
//        if (loxClass == null) {
//            throw new LoxRuntimeError(field, "the field or method does not exist");
//        }
//        LoxFunction original = loxClass.getMethod(field.lexeme);
//        if (original == null) {
//            throw new LoxRuntimeError(field, "the field or method does not exist");
//        }
//        return original.binding(this);
        try {
            return get(field.lexeme);
        }catch (LoxRuntimeError e) {
            throw new LoxRuntimeError(field, "the field or method does not exist");
        }
    }

    public Object get(String field) {
         if (fields.containsKey(field)) {
            return fields.get(field);
        }
        if (loxClass == null) {
            throw new LoxRuntimeError(null, "the field or method does not exist");
        }
        LoxFunction original = loxClass.getMethod(field);
        if (original == null) {
            throw new LoxRuntimeError(null, "the field or method does not exist");
        }
        return original.binding(this);
    }

    public void set(Token field, Object value) {
        set(field.lexeme, value);
    }

    public void set(String field, Object value) {
        fields.put(field, value);
    }

    @Override
    public String toString() {
        // LoxClass as LoxInstance has no own class
        if (loxClass != null) {
            return "<object: %s>".formatted(loxClass.name);
        } else {
            return "<object: NoClass>";
        }
    }

    public static class LoxModule extends LoxInstance{
        String moduleName;
        public LoxModule(String name, Environment env) {
            super(env.values);
            this.moduleName = name;
        }

        @Override
        public String toString() {
            return "<module: %s>".formatted(moduleName);
        }
    }

    public LoxClass getLoxClass() {
        return loxClass;
    }
}
