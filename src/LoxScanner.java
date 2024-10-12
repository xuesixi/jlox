import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LoxScanner {

    private static final HashMap<String, TokenType> typeMap;

    static {
        typeMap = new HashMap<>();
        typeMap.put("and", TokenType.AND);
        typeMap.put("class", TokenType.CLASS);
        typeMap.put("else", TokenType.ELSE);
        typeMap.put("false", TokenType.FALSE);
        typeMap.put("for", TokenType.FOR);
        typeMap.put("fun", TokenType.FUN);
        typeMap.put("if", TokenType.IF);
        typeMap.put("nil", TokenType.NIL);
        typeMap.put("or", TokenType.OR);
        typeMap.put("print", TokenType.PRINT);
        typeMap.put("return", TokenType.RETURN);
        typeMap.put("super", TokenType.SUPER);
        typeMap.put("this", TokenType.THIS);
        typeMap.put("true", TokenType.TRUE);
        typeMap.put("var", TokenType.VAR);
        typeMap.put("while", TokenType.WHILE);
    }

    private final String source;
    private int start;
    private int current;
    private int line = 1;
    private ArrayList<Token> tokenList = new ArrayList<>();

    public LoxScanner(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isEnd()) {
            nextToken();
        }
        tokenList.add(new Token(TokenType.EOF, "", null, line));
        return tokenList;
    }

    private void nextToken() {
        start = current;
        char next = advance();
        switch (next) {
            case '(':
                addToken(TokenType.LEFT_PAREN);
                break;
            case ')':
                addToken(TokenType.RIGHT_PAREN);
                break;
            case '{':
                addToken(TokenType.LEFT_BRACE);
                break;
            case '}':
                addToken(TokenType.RIGHT_BRACE);
                break;
            case ',':
                addToken(TokenType.COMMA);
                break;
            case '.':
                addToken(TokenType.DOT);
                break;
            case '-':
                addToken(TokenType.MINUS);
                break;
            case '+':
                addToken(TokenType.PLUS);
                break;
            case ';':
                addToken(TokenType.SEMICOLON);
                break;
            case '*':
                addToken(TokenType.STAR);
                break;
            case '!':
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;
            case '=':
                addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                break;
            case '/':
                if (match('/')) {
                    while (!isEnd() && peek() != '\n') {
                        current++;
                    }
                } else {
                    addToken(TokenType.SLASH);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;
            case '"':
                stringLiteral();
                break;
            default:
                if (isDigit(next)) {
                    numberLiteral();
                } else if (isAlpha(next)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character");
                    break;
                }
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) {
            current++;
        }
        String token = source.substring(start, current);
        TokenType type = typeMap.get(token);
        if (type == null) {
            type = TokenType.IDENTIFIER;
        }
        addToken(type);
    }

    private void stringLiteral() {
        // 該循環會因為兩個原因結束：源代碼結束，或者遇到右引號
        while (!isEnd() && peek() != '"') {
            current++;
            if (peek() == '\n') {
                line++;
            }
        }
        // 如果是因為源代碼結束，那麼說明我們沒有遇到右引號
        if (isEnd()) {
            Lox.error(line, "Unterminated string");
            return;
        }

        //  否則，我們遇到了右引號
        current++;
        addToken(TokenType.STRING, source.substring(start + 1, current - 1));
    }

    private void numberLiteral() {
        while (isDigit(peek()))
            advance();

        // Look for a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();

            while (isDigit(peek()))
                advance();
        }

        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    /**
     * return the current char and increment current pointer
     *
     * @return the current char
     */
    private char advance() {
        return source.charAt(current++);
    }

    /**
     * check if the current char match the given char. If yes, increment the current pointer
     *
     * @param ch a char to test matching
     * @return if the current char match of given char
     */
    private boolean match(char ch) {
        if (isEnd()) {
            return false;
        }
        if (source.charAt(current + 1) != ch) {
            return false;
        } else {
            current++;
            return true;
        }
    }

    /**
     * @return the current char without incrementing
     */
    private char peek() {
        if (isEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length())
            return '\0';
        return source.charAt(current + 1);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        Token token = new Token(type, text, literal, line);
        tokenList.add(token);
    }

    private static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isEnd() {
        return current >= source.length();
    }

}
