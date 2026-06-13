// ─────────────────────────────────────────────────────────────
// JSFunction.java
// Represents a JavaScript function as a Java object
// Stores parameter names, the body AST node, and the
// closure environment (the scope where it was defined)
// ─────────────────────────────────────────────────────────────

import java.util.List;

public class JSFunction {


    // ── Parameter definitions in declaration order ─────────────
    public final List<ASTNode.Param> params;


    // ── The function body (Block or single expression) ────────
    public final ASTNode body;


    // ── The scope where this function was CREATED ─────────────
    // This is what enables closures to work correctly
    public final Environment closureEnv;


    // ── Optional name (for function declarations) ─────────────
    public final String name;


    // ── Constructor ───────────────────────────────────────────
    public JSFunction(String name, List<ASTNode.Param> params, ASTNode body, Environment closureEnv) {
        this.name       = name;
        this.params     = params;
        this.body       = body;
        this.closureEnv = closureEnv;
    }


    // ── Nice string form for debugging ────────────────────────
    @Override
    public String toString() {
        return "[Function: " + (name != null ? name : "anonymous") + "]";
    }
}
