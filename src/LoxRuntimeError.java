public class LoxRuntimeError extends RuntimeException{
    Token token;
    LoxRuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }

    static class LoxReturn extends  LoxRuntimeError {
        Object value;

        LoxReturn(Token token, Object value) {
            super(token, "A return statement is only allowed inside a function");
            this.value = value;
        }
    }
}
