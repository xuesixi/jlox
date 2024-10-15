public class LoxRuntimeError extends RuntimeException{
    Token token;
    LoxRuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
