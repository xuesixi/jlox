import java.util.Arrays;
import java.util.List;

public class LoxArray extends LoxInstance{
    private final Object[] backing;

    public LoxArray(int len) {
        super((LoxClass) null);
        backing = new Object[len];
        this.set("length", (double) len);
    }

    public LoxArray(int[] dimensions) {
        this(dimensions[0]);
        if (dimensions.length == 1) {
            return;
        }
        int[] remaining = Arrays.copyOfRange(dimensions, 1, dimensions.length);
        for (int i = 0; i < backing.length; i++) {
            backing[i] = new LoxArray(remaining);
        }
    }

    public LoxArray(List<Object> items) {
        this(items.size());
        int len = items.size();
        for (int i = 0; i < len; i++) {
            backing[i] = items.get(i);
        }
    }


    public int getLength() {
        return backing.length;
    }

    public Object getAtIndex(int index) {
        return backing[index];
    }

    public void setAtIndex(int index, Object value) {
        backing[index] = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Object item : backing) {
            sb.append(Interpreter.stringify(item)).append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append("]");
        return sb.toString();
    }
}
