import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {

    public static boolean hadError = false;
    public static boolean hadRuntimeError = false;
    public static boolean repl = false; // 调用 runPrompt 的时候，该变量会被设为 true。这会导致在所有表达式语句输出其值。

    public static Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException {
        if (args.length == 1) {
            System.out.println("running file: " + args[0]);
            runFile(args[0]);
        } else if (args.length == 0) {
            System.out.println("running prompt");
            runPrompt();
        } else {
            System.out.println("Error");
        }
    }

    public static void runFile(String filename) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filename));
        String source = new String(bytes);
        run(source);
        if (hadError) {
            System.exit(65);
        }
        if (hadRuntimeError) {
            System.exit(70);
        }
    }

    public static void runPrompt() throws IOException {
        repl = true;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            // in repl mode, if the input does not end with a semicolon, automatically append one;
            if (!line.endsWith(";")) {
                line += ";";
            }
            run(line);
            hadError = false;
        }
    }

    public static void run(String source) {
        LoxScanner scanner = new LoxScanner(source);
        List<Token> tokens = scanner.scanTokens();
        LoxParser parser = new LoxParser(tokens);
        List<Stmt> statements = parser.parse();
        if (!hadError) {
            interpreter.interpret(statements);
        }
    }

    public static void parsingError(int line,String where, String message) {
        report(line, where, "Parsing error: " + message);
    }

    public static void scanningError(int line, String where, String message) {
        report(line, where, "Scanning error: " + message);
    }

//    public static void error(Token token, String message) {
//        error(token.line, message);
//    }

    static void runtimeError(LoxRuntimeError error) {
        System.err.println("Runtime error: " + error.getMessage() +
                "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    public static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] error \"" + where + "\": " + message);
        hadError = true;
    }

}

