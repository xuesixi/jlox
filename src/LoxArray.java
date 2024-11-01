import java.util.Arrays;

public class LoxArray {
    private final Object[] backing;

    public LoxArray(int[] dimensions) {
        int len = dimensions[0];
        backing = new Object[len];
        if (dimensions.length == 1) {
            return;
        }
        int[] remaining = Arrays.copyOfRange(dimensions, 1, dimensions.length);
        for (int i = 0; i < len; i++) {
            backing[i] = new LoxArray(remaining);
        }
    }


    public int getLength() {
        return backing.length;
    }

    public Object atIndex(int index) {
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
