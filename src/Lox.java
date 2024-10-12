import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Lox {

    public static boolean hadError = false;
    public static void main(String[] args) throws IOException {
        if (args.length == 1) {
            System.out.println("running file");
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
    }

    public static void runPrompt() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.println("> ");
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            run(line);
            hadError = false;
        }
    }

    public static void run(String source) {
        LoxScanner scanner = new LoxScanner(source);
        scanner.scanTokens();
        for (Token token: scanner.tokenList) {
            System.out.println(token);
        }
    }

    public static void error(int line, String message) {
        report(line, "", message);
    }

    public static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] error " + where + ": " + message);
    }

}

