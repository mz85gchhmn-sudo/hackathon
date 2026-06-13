// ─────────────────────────────────────────────────────────────
// Main.java
// Entry point of the JS interpreter
// Usage:  java Main <path-to-js-file>
//    or:  cat script.js | java Main
// Reads the source, runs Lexer → Parser → Interpreter
// ─────────────────────────────────────────────────────────────

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {

        // ── Read JS source from file or stdin ─────────────────
        String source;

        if (args.length > 0) {
            // File path provided as argument
            source = new String(Files.readAllBytes(Paths.get(args[0])));
        } else {
            // Read from stdin (pipe mode)
            source = new String(System.in.readAllBytes());
        }


        // ── Step 1: Tokenize ──────────────────────────────────
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();


        // ── Step 2: Parse into AST ────────────────────────────
        Parser parser = new Parser(tokens);
        ASTNode.Program program = parser.parse();


        // ── Step 3: Execute the AST ───────────────────────────
        Interpreter interpreter = new Interpreter();
        interpreter.execute(program);
    }
}
