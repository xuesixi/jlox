import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoxClass extends LoxInstance implements LoxCallable {
    final String name;
    final HashMap<String, LoxFunction> methods;
    final LoxClass superClass;

    /**
     * 一个类可以有自己的父类。但是，作为一个对象，它自己的类型是 origin。如果没有指定父类，那么父类也是 origin
     */
    LoxClass(String name, HashMap<String, LoxFunction> methods, HashMap<String, Object> staticFields, LoxClass superClass) {
        super(staticFields);
        this.name = name;
        this.methods = methods;
        if (superClass != null) {
            this.superClass = superClass;
        }else {
            this.superClass = origin;
        }
    }

    /**
     * @return null if not found
     */
    public LoxFunction getMethod(String methodName) {
        if (methods.containsKey(methodName)) {
            return methods.get(methodName);
        } else if (superClass != null) {
            return superClass.getMethod(methodName);
        } else {
            return null;
        }
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

    public boolean isOfType(LoxClass type) {
        if (this == type) {
            return true;
        }
        if (superClass == null) {
            return false;
        }
        return this.superClass.isOfType(type);
    }

    /**
     * <p>只有两个对象的父类是 null: native object 和 origin.
     *  <p>origin 的父类是 null，因为在用 LoxClass 构造函数构造 origin 的时候，origin 还是 null，所以得到了静态引用
     *  <p>但是，值得注意的是，loxFunction 并不是loxInstance</p>
      */

    public static LoxClass origin;
}
