import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {

    public static boolean hadError = false;
    public static boolean hadRuntimeError = false;
    public static boolean repl = false; // 调用 runPrompt 的时候，该变量会被设为 true。这会导致返回值不为 nil 的表达式语句输出其值。

    public static Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException {
        if (args.length == 1) {
            System.out.println("running file: " + args[0]);
            System.out.println();
            runFile(args[0]);
        } else if (args.length == 0) {
            System.out.println("running prompt");
            System.out.println();
            runPrompt();
        } else {
            System.out.println("Error. You can have 0 argument to run the repl, or 1 argument to run a specific lox file");
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

    /**
     * REPL 模式下，如果补货到了 ReplPending 异常，说明在某个该有语句的地方没有语句。
     * 此时我们会保存上一次的输入，然后重新读取输入。将这两次的输入合并在一起，一起重新解释。
     * @throws IOException ???
     */
    public static void runPrompt() throws IOException {
        repl = true;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String old = "";
        while (true) {
            if (old.isEmpty()) {
                System.out.print("> ");
            } else {
                System.out.print("... ");
            }
            String line = reader.readLine();
            if (line == null) {
                System.out.println();
                break;
            }
            try {
                run(old + line);
                old = "";
            }catch (LoxParser.ReplPending e) {
                old += line;
                continue;
            } catch (LoxParser.ParseError e) {
                old = "";
            }
            hadError = false;
        }
    }

    public static void run(String source) {
        LoxScanner scanner = new LoxScanner(source);
        List<Token> tokens = scanner.scanTokens();
        LoxParser parser = new LoxParser(tokens);
        List<Stmt> statements = parser.parse();
        if (!hadError) {
            Resolver resolver = new Resolver(interpreter);
            resolver.resolve(statements);
            if (!hadError) {
                interpreter.interpret(statements);
            }
        }
    }

    public static void parsingError(int line,String where, String message) {
        report(line, where, "Parsing error: " + message);
    }

    public static void scanningError(int line, String where, String message) {
        report(line, where, "Scanning error: " + message);
    }

    public static void resolvingError(int line, String where, String message) {
        report(line, where, "Resolving error: " + message);
    }


    /**
     * 这个函数不是用来抛出运行时错误，而是当运行时错误被 catch 时，调用该函数向用户报告。
     * @param error 捕获到的运行时错误
     */
    static void reportRuntimeError(LoxRuntimeError error) {
        System.out.printf("[Line %d] Runtime error: %s: \"%s\"\n", error.token.line, error.getMessage(), error.token.lexeme);
        hadRuntimeError = true;
    }

    public static void report(int line, String where, String message) {
        System.out.printf("[line %d] %s: \"%s\"\n", line, message, where);
        hadError = true;
    }

}

