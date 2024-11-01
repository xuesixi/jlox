import java.util.Arrays;

public class LoxArray {
    private int length;
    private Object[] backing;

    public LoxArray(int length) {
        this.length = length;
        backing = new Object[length];
    }

    public int getLength() {
        return length;
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
//        if (length <= 7) {
            sb.append("[");
            for (int i = 0; i < length; i++) {
                sb.append(Interpreter.stringify(backing[i])).append(", ");
            }
            sb.delete(sb.length() -2 , sb.length());
            sb.append("]");
            return sb.toString();
//        } else {
//            sb.append("[\n");
//            for (int i = 0; i < length; i++) {
//                sb.append("    ").append(Interpreter.stringify(backing[i])).append(",\n");
//            }
//            sb.append("]");
//            return sb.toString();
//        }
    }
}
