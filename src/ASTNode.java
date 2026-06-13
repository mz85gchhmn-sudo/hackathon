// ─────────────────────────────────────────────────────────────
// ASTNode.java
// Every node type in the Abstract Syntax Tree lives here
// as a static inner class extending ASTNode
// The Parser creates these; the Interpreter reads them
// ─────────────────────────────────────────────────────────────

import java.util.List;
import java.util.Map;

public abstract class ASTNode {


    // ══════════════════════════════════════════════════════════
    //  STATEMENT NODES
    // ══════════════════════════════════════════════════════════


    // ── Holds the full list of top-level statements ───────────
    public static class Program extends ASTNode {
        public final List<ASTNode> body;
        public Program(List<ASTNode> body) { this.body = body; }
    }


    // ── A { ... } block containing multiple statements ────────
    public static class Block extends ASTNode {
        public final List<ASTNode> statements;
        public Block(List<ASTNode> statements) { this.statements = statements; }
    }


    // ── let / const / var x = value  (or destructuring pattern) ─
    public static class VarDeclaration extends ASTNode {
        public final String kind;   // "let", "const", or "var"
        public final String name;   // simple name (null if using a pattern)
        public final ASTNode pattern; // ArrayPattern/ObjectPattern (null if simple name)
        public final ASTNode init;  // the right-hand side (can be null)
        public VarDeclaration(String kind, String name, ASTNode init) {
            this.kind = kind;
            this.name = name;
            this.pattern = null;
            this.init = init;
        }
        public VarDeclaration(String kind, ASTNode pattern, ASTNode init, boolean isPattern) {
            this.kind = kind;
            this.name = null;
            this.pattern = pattern;
            this.init = init;
        }
    }


    // ── if (condition) { ... } else { ... } ──────────────────
    public static class IfStatement extends ASTNode {
        public final ASTNode condition;
        public final ASTNode thenBranch;
        public final ASTNode elseBranch;  // can be null
        public IfStatement(ASTNode condition, ASTNode thenBranch, ASTNode elseBranch) {
            this.condition  = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }
    }


    // ── while (condition) { ... } ────────────────────────────
    public static class WhileStatement extends ASTNode {
        public final ASTNode condition;
        public final ASTNode body;
        public WhileStatement(ASTNode condition, ASTNode body) {
            this.condition = condition;
            this.body      = body;
        }
    }


    // ── do { ... } while (condition) ─────────────────────────
    public static class DoWhileStatement extends ASTNode {
        public final ASTNode body;
        public final ASTNode condition;
        public DoWhileStatement(ASTNode body, ASTNode condition) {
            this.body      = body;
            this.condition = condition;
        }
    }


    // ── for (init; condition; update) { ... } ────────────────
    public static class ForStatement extends ASTNode {
        public final ASTNode init;       // can be null
        public final ASTNode condition;  // can be null
        public final ASTNode update;     // can be null
        public final ASTNode body;
        public ForStatement(ASTNode init, ASTNode condition, ASTNode update, ASTNode body) {
            this.init      = init;
            this.condition = condition;
            this.update    = update;
            this.body      = body;
        }
    }


    // ── switch (value) { case x: ... default: ... } ──────────
    public static class SwitchStatement extends ASTNode {
        public final ASTNode discriminant;
        public final List<SwitchCase> cases;
        public SwitchStatement(ASTNode discriminant, List<SwitchCase> cases) {
            this.discriminant = discriminant;
            this.cases        = cases;
        }
    }


    // ── A single case (or default) inside a switch ────────────
    public static class SwitchCase extends ASTNode {
        public final ASTNode test;              // null means "default:"
        public final List<ASTNode> consequent;
        public SwitchCase(ASTNode test, List<ASTNode> consequent) {
            this.test       = test;
            this.consequent = consequent;
        }
    }


    // ── function name(params) { body } ───────────────────────
    public static class FunctionDeclaration extends ASTNode {
        public final String name;
        public final List<Param> params;
        public final Block body;
        public FunctionDeclaration(String name, List<Param> params, Block body) {
            this.name   = name;
            this.params = params;
            this.body   = body;
        }
    }


    // ── return value; ─────────────────────────────────────────
    public static class ReturnStatement extends ASTNode {
        public final ASTNode value;  // can be null
        public ReturnStatement(ASTNode value) { this.value = value; }
    }


    // ── break; ───────────────────────────────────────────────
    public static class BreakStatement extends ASTNode {}


    // ── A bare expression used as a statement  e.g. foo(); ───
    public static class ExpressionStatement extends ASTNode {
        public final ASTNode expression;
        public ExpressionStatement(ASTNode expression) { this.expression = expression; }
    }


    // ── try { } catch (e) { } finally { } ────────────────────
    public static class TryStatement extends ASTNode {
        public final Block tryBlock;
        public final String catchParam;   // can be null (catch with no binding)
        public final Block catchBlock;    // can be null (no catch clause)
        public final Block finallyBlock;  // can be null
        public TryStatement(Block tryBlock, String catchParam, Block catchBlock, Block finallyBlock) {
            this.tryBlock     = tryBlock;
            this.catchParam   = catchParam;
            this.catchBlock   = catchBlock;
            this.finallyBlock = finallyBlock;
        }
    }


    // ── throw expr; ───────────────────────────────────────────
    public static class ThrowStatement extends ASTNode {
        public final ASTNode value;
        public ThrowStatement(ASTNode value) { this.value = value; }
    }


    // ── for (let x of iterable) { ... } ──────────────────────
    public static class ForOfStatement extends ASTNode {
        public final String kind;
        public final String varName;
        public final ASTNode iterable;
        public final ASTNode body;
        public ForOfStatement(String kind, String varName, ASTNode iterable, ASTNode body) {
            this.kind     = kind;
            this.varName  = varName;
            this.iterable = iterable;
            this.body     = body;
        }
    }


    // ── for (let x in obj) { ... } ───────────────────────────
    public static class ForInStatement extends ASTNode {
        public final String kind;
        public final String varName;
        public final ASTNode object;
        public final ASTNode body;
        public ForInStatement(String kind, String varName, ASTNode object, ASTNode body) {
            this.kind    = kind;
            this.varName = varName;
            this.object  = object;
            this.body    = body;
        }
    }


    // ══════════════════════════════════════════════════════════
    //  EXPRESSION NODES
    // ══════════════════════════════════════════════════════════


    // ── `text ${expr} text` — parts are String or ASTNode ─────
    public static class TemplateLiteral extends ASTNode {
        public final List<Object> parts;
        public TemplateLiteral(List<Object> parts) { this.parts = parts; }
    }


    // ── A raw literal value: number, string, boolean, null ────
    public static class Literal extends ASTNode {
        public final Object value;
        public Literal(Object value) { this.value = value; }
    }


    // ── A variable name reference  e.g. x, arr, myFunc ───────
    public static class Identifier extends ASTNode {
        public final String name;
        public Identifier(String name) { this.name = name; }
    }


    // ── left OP right  e.g. a + b, x === y ──────────────────
    public static class BinaryOp extends ASTNode {
        public final ASTNode left;
        public final String op;
        public final ASTNode right;
        public BinaryOp(ASTNode left, String op, ASTNode right) {
            this.left  = left;
            this.op    = op;
            this.right = right;
        }
    }


    // ── OP operand  e.g. !x, -y, typeof z ────────────────────
    public static class UnaryOp extends ASTNode {
        public final String op;
        public final ASTNode operand;
        public UnaryOp(String op, ASTNode operand) {
            this.op      = op;
            this.operand = operand;
        }
    }


    // ── x++  or  x-- (post-increment / post-decrement) ───────
    public static class UpdateExpression extends ASTNode {
        public final String op;       // "++" or "--"
        public final ASTNode operand;
        public final boolean prefix;  // true = ++x, false = x++
        public UpdateExpression(String op, ASTNode operand, boolean prefix) {
            this.op      = op;
            this.operand = operand;
            this.prefix  = prefix;
        }
    }


    // ── target = value  or  target += value  etc. ────────────
    public static class Assignment extends ASTNode {
        public final ASTNode target;
        public final String op;
        public final ASTNode value;
        public Assignment(ASTNode target, String op, ASTNode value) {
            this.target = target;
            this.op     = op;
            this.value  = value;
        }
    }


    // ── callee(arg1, arg2, ...) ───────────────────────────────
    public static class CallExpression extends ASTNode {
        public final ASTNode callee;
        public final List<ASTNode> args;
        public CallExpression(ASTNode callee, List<ASTNode> args) {
            this.callee = callee;
            this.args   = args;
        }
    }


    // ── object.property  or  object[computed]  or  object?.prop ─
    public static class MemberExpression extends ASTNode {
        public final ASTNode object;
        public final String property;    // used when computed = false
        public final ASTNode computed;   // used when computed = true (bracket access)
        public final boolean optional;   // true for ?.  (optional chaining)
        public MemberExpression(ASTNode object, String property, ASTNode computed) {
            this(object, property, computed, false);
        }
        public MemberExpression(ASTNode object, String property, ASTNode computed, boolean optional) {
            this.object   = object;
            this.property = property;
            this.computed = computed;
            this.optional = optional;
        }
    }


    // ── [elem1, elem2, ...] ───────────────────────────────────
    public static class ArrayLiteral extends ASTNode {
        public final List<ASTNode> elements;
        public ArrayLiteral(List<ASTNode> elements) { this.elements = elements; }
    }


    // ── { key: value, ... } ───────────────────────────────────
    public static class ObjectLiteral extends ASTNode {
        public final Map<String, ASTNode> properties;
        public ObjectLiteral(Map<String, ASTNode> properties) { this.properties = properties; }
    }


    // ── function(params) { body }  or  (params) => expr ──────
    public static class FunctionExpression extends ASTNode {
        public final List<Param> params;
        public final ASTNode body;          // Block or single expression
        public final boolean isArrow;
        public FunctionExpression(List<Param> params, ASTNode body, boolean isArrow) {
            this.params  = params;
            this.body    = body;
            this.isArrow = isArrow;
        }
    }


    // ── condition ? thenExpr : elseExpr ──────────────────────
    public static class TernaryExpression extends ASTNode {
        public final ASTNode condition;
        public final ASTNode thenExpr;
        public final ASTNode elseExpr;
        public TernaryExpression(ASTNode condition, ASTNode thenExpr, ASTNode elseExpr) {
            this.condition = condition;
            this.thenExpr  = thenExpr;
            this.elseExpr  = elseExpr;
        }
    }


    // ── ...arg  (spread inside arrays or function calls) ─────
    public static class SpreadElement extends ASTNode {
        public final ASTNode argument;
        public SpreadElement(ASTNode argument) { this.argument = argument; }
    }


    // ── new ClassName(args) ───────────────────────────────────
    public static class NewExpression extends ASTNode {
        public final ASTNode callee;
        public final List<ASTNode> args;
        public NewExpression(ASTNode callee, List<ASTNode> args) {
            this.callee = callee;
            this.args   = args;
        }
    }


    // ══════════════════════════════════════════════════════════
    //  PARAMETERS & DESTRUCTURING PATTERNS
    // ══════════════════════════════════════════════════════════


    // ── A single parameter / binding target ───────────────────
    // pattern: Identifier, ArrayPattern, or ObjectPattern
    // defaultValue: the expression after `=` (or null)
    // rest: true if this is a `...name` rest parameter/element
    public static class Param extends ASTNode {
        public final ASTNode pattern;
        public final ASTNode defaultValue;
        public final boolean rest;
        public Param(ASTNode pattern, ASTNode defaultValue, boolean rest) {
            this.pattern      = pattern;
            this.defaultValue = defaultValue;
            this.rest         = rest;
        }
    }


    // ── [a, b = 1, ...rest] destructuring pattern ─────────────
    public static class ArrayPattern extends ASTNode {
        public final List<Param> elements;
        public ArrayPattern(List<Param> elements) { this.elements = elements; }
    }


    // ── { a, b: bAlias = 1, ...rest } destructuring pattern ───
    public static class ObjectPattern extends ASTNode {
        public final Map<String, Param> properties;
        public final String restName; // can be null
        public ObjectPattern(Map<String, Param> properties, String restName) {
            this.properties = properties;
            this.restName   = restName;
        }
    }
}
