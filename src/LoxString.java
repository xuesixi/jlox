public class LoxString extends LoxInstance{
    private final String backing;
    public static LoxClass LoxStringClass = null;

    public LoxString(String s) {
        super(LoxStringClass);
        this.backing = s;
    }

    public String getBacking() {
        return backing;
    }

    @Override
    public String toString() {
        return backing;
    }
}
