// ─────────────────────────────────────────────────────────────
// Interpreter.java
// Walks the AST produced by the Parser and executes each node
// Handles all JS built-ins (Math, Array, String, console, Date)
// ─────────────────────────────────────────────────────────────

import java.util.*;

public class Interpreter {


    // ── Thrown internally to unwind the call stack on return ──
    private static class ReturnException extends RuntimeException {
        final Object value;
        ReturnException(Object value) { super(null, null, true, false); this.value = value; }
    }


    // ── Thrown internally to handle break statements ──────────
    private static class BreakException extends RuntimeException {
        BreakException() { super(null, null, true, false); }
    }


    // ── Thrown internally to handle continue statements ───────
    private static class ContinueException extends RuntimeException {
        ContinueException() { super(null, null, true, false); }
    }


    // ── Thrown internally for JS `throw` statements ───────────
    private static class ThrowException extends RuntimeException {
        final Object value;
        ThrowException(Object value) { super(null, null, true, false); this.value = value; }
    }


    // ── Singleton for JS undefined ────────────────────────────
    public static final Object UNDEFINED = new Object() {
        @Override public String toString() { return "undefined"; }
    };


    // ── The global scope shared across the whole program ──────
    private final Environment globalEnv;


    // ── Constructor: set up global scope with built-ins ───────
    public Interpreter() {
        globalEnv = new Environment(null);
        setupBuiltins();
    }


    // ══════════════════════════════════════════════════════════
    //  BUILT-IN SETUP
    // ══════════════════════════════════════════════════════════


    // ── Register all global built-in objects and functions ────
    private void setupBuiltins() {

        // console object with .log()
        Map<String, Object> console = new LinkedHashMap<>();
        console.put("log", (JSBuiltin) args -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(jsToString(args.get(i)));
            }
            System.out.println(sb);
            return UNDEFINED;
        });
        globalEnv.define("console", console);

        // Math object
        Map<String, Object> math = new LinkedHashMap<>();
        math.put("PI",      Math.PI);
        math.put("E",       Math.E);
        math.put("floor",   (JSBuiltin) a -> Math.floor(toNumber(a.get(0))));
        math.put("ceil",    (JSBuiltin) a -> Math.ceil(toNumber(a.get(0))));
        math.put("round",   (JSBuiltin) a -> (double) Math.round(toNumber(a.get(0))));
        math.put("abs",     (JSBuiltin) a -> Math.abs(toNumber(a.get(0))));
        math.put("sqrt",    (JSBuiltin) a -> Math.sqrt(toNumber(a.get(0))));
        math.put("pow",     (JSBuiltin) a -> Math.pow(toNumber(a.get(0)), toNumber(a.get(1))));
        math.put("max",     (JSBuiltin) a -> a.stream().mapToDouble(this::toNumber).max().orElse(Double.NEGATIVE_INFINITY));
        math.put("min",     (JSBuiltin) a -> a.stream().mapToDouble(this::toNumber).min().orElse(Double.POSITIVE_INFINITY));
        math.put("random",  (JSBuiltin) a -> Math.random());
        math.put("log",     (JSBuiltin) a -> Math.log(toNumber(a.get(0))));
        math.put("trunc",   (JSBuiltin) a -> (double)(long)(double) toNumber(a.get(0)));
        math.put("sign",    (JSBuiltin) a -> (double) Double.compare(toNumber(a.get(0)), 0));
        globalEnv.define("Math", math);

        // parseInt and parseFloat
        globalEnv.define("parseInt",   (JSBuiltin) a -> {
            try { return (double) Integer.parseInt(jsToString(a.get(0))); }
            catch (Exception e) { return Double.NaN; }
        });
        globalEnv.define("parseFloat", (JSBuiltin) a -> {
            try { return Double.parseDouble(jsToString(a.get(0))); }
            catch (Exception e) { return Double.NaN; }
        });

        // isNaN / isFinite
        globalEnv.define("isNaN",     (JSBuiltin) a -> Double.isNaN(toNumber(a.get(0))));
        globalEnv.define("isFinite",  (JSBuiltin) a -> Double.isFinite(toNumber(a.get(0))));

        // Type conversion functions
        globalEnv.define("String",    (JSBuiltin) a -> jsToString(a.get(0)));
        globalEnv.define("Number",    (JSBuiltin) a -> toNumber(a.get(0)));
        globalEnv.define("Boolean",   (JSBuiltin) a -> isTruthy(a.get(0)));

        // Array.isArray
        Map<String, Object> arrayObj = new LinkedHashMap<>();
        arrayObj.put("isArray", (JSBuiltin) a -> a.get(0) instanceof List);
        globalEnv.define("Array", arrayObj);

        // undefined value
        globalEnv.define("undefined", UNDEFINED);

        // Date constructor
        globalEnv.define("Date", (JSBuiltin) a -> {
            Map<String, Object> dateObj = new LinkedHashMap<>();
            java.util.Date now = new java.util.Date();
            dateObj.put("getFullYear",        (JSBuiltin) x -> (double)(now.getYear() + 1900));
            dateObj.put("getMonth",           (JSBuiltin) x -> (double) now.getMonth());
            dateObj.put("getDate",            (JSBuiltin) x -> (double) now.getDate());
            dateObj.put("getDay",             (JSBuiltin) x -> (double) now.getDay());
            dateObj.put("getHours",           (JSBuiltin) x -> (double) now.getHours());
            dateObj.put("getMinutes",         (JSBuiltin) x -> (double) now.getMinutes());
            dateObj.put("getSeconds",         (JSBuiltin) x -> (double) now.getSeconds());
            dateObj.put("getTime",            (JSBuiltin) x -> (double) now.getTime());
            dateObj.put("toLocaleDateString", (JSBuiltin) x -> now.toString());
            return dateObj;
        });

        // BONUS: Node.js compatibility shims (process.stdout, etc.)
        // See NodeCompat.java
        NodeCompat.install(globalEnv);
    }


    // ── Functional interface for built-in Java lambdas ────────
    @FunctionalInterface
    interface JSBuiltin {
        Object call(List<Object> args);
    }


    // ══════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ══════════════════════════════════════════════════════════


    // ── Execute the full program AST ──────────────────────────
    public void execute(ASTNode.Program program) {
        for (ASTNode stmt : program.body) {
            eval(stmt, globalEnv);
        }
    }


    // ══════════════════════════════════════════════════════════
    //  MAIN EVAL DISPATCHER
    // ══════════════════════════════════════════════════════════


    // ── Route each node to the right eval method ──────────────
    @SuppressWarnings("unchecked")
    private Object eval(ASTNode node, Environment env) {

        // ── Literal value (number, string, boolean, null) ─────
        if (node instanceof ASTNode.Literal n) {
            return n.value == null ? null : n.value;
        }

        // ── Variable reference ────────────────────────────────
        if (node instanceof ASTNode.Identifier n) {
            if (n.name.equals("undefined")) return UNDEFINED;
            if (n.name.equals("NaN"))       return Double.NaN;
            if (n.name.equals("Infinity"))  return Double.POSITIVE_INFINITY;
            return env.get(n.name);
        }

        // ── let / const / var x = ...  (or destructuring) ────
        if (node instanceof ASTNode.VarDeclaration n) {
            Object val = n.init != null ? eval(n.init, env) : UNDEFINED;
            if (n.pattern != null) {
                bindPattern(n.pattern, val, env);
            } else {
                env.define(n.name, val);
            }
            return UNDEFINED;
        }

        // ── let a = 1, b = 2, c;  (multiple declarations) ─────
        if (node instanceof ASTNode.VarDeclarationList n) {
            for (ASTNode.VarDeclaration decl : n.declarations) {
                eval(decl, env);
            }
            return UNDEFINED;
        }

        // ── { statements } ────────────────────────────────────
        if (node instanceof ASTNode.Block n) {
            Environment blockEnv = new Environment(env);
            Object last = UNDEFINED;
            for (ASTNode stmt : n.statements) {
                last = eval(stmt, blockEnv);
            }
            return last;
        }

        // ── Program node ──────────────────────────────────────
        if (node instanceof ASTNode.Program n) {
            for (ASTNode stmt : n.body) eval(stmt, env);
            return UNDEFINED;
        }

        // ── Expression used as a statement ────────────────────
        if (node instanceof ASTNode.ExpressionStatement n) {
            return eval(n.expression, env);
        }

        // ── if / else ─────────────────────────────────────────
        if (node instanceof ASTNode.IfStatement n) {
            if (isTruthy(eval(n.condition, env))) return eval(n.thenBranch, env);
            if (n.elseBranch != null)              return eval(n.elseBranch, env);
            return UNDEFINED;
        }

        // ── while loop ────────────────────────────────────────
        if (node instanceof ASTNode.WhileStatement n) {
            outer:
            while (isTruthy(eval(n.condition, env))) {
                try { eval(n.body, env); }
                catch (BreakException e)    { break outer; }
                catch (ContinueException e) { continue outer; }
            }
            return UNDEFINED;
        }

        // ── do...while loop ───────────────────────────────────
        if (node instanceof ASTNode.DoWhileStatement n) {
            outer:
            do {
                try { eval(n.body, env); }
                catch (BreakException e)    { break outer; }
                catch (ContinueException e) { continue outer; }
            } while (isTruthy(eval(n.condition, env)));
            return UNDEFINED;
        }

        // ── for loop ──────────────────────────────────────────
        if (node instanceof ASTNode.ForStatement n) {
            Environment forEnv = new Environment(env);
            if (n.init != null) eval(n.init, forEnv);
            outer:
            while (n.condition == null || isTruthy(eval(n.condition, forEnv))) {
                try { eval(n.body, forEnv); }
                catch (BreakException e)    { break outer; }
                catch (ContinueException e) { /* fall through to update */ }
                if (n.update != null) eval(n.update, forEnv);
            }
            return UNDEFINED;
        }

        // ── switch statement ──────────────────────────────────
        if (node instanceof ASTNode.SwitchStatement n) {
            Object disc = eval(n.discriminant, env);
            boolean matched = false;
            try {
                for (ASTNode.SwitchCase c : n.cases) {
                    if (!matched && c.test != null) {
                        matched = jsStrictEqual(disc, eval(c.test, env));
                    } else if (c.test == null) {
                        matched = true;
                    }
                    if (matched) {
                        for (ASTNode stmt : c.consequent) eval(stmt, env);
                    }
                }
            } catch (BreakException ignored) {}
            return UNDEFINED;
        }

        // ── break ─────────────────────────────────────────────
        if (node instanceof ASTNode.BreakStatement) {
            throw new BreakException();
        }

        // ── continue ──────────────────────────────────────────
        if (node instanceof ASTNode.ContinueStatement) {
            throw new ContinueException();
        }

        // ── for...of loop ──────────────────────────────────────
        if (node instanceof ASTNode.ForOfStatement n) {
            Object iterable = eval(n.iterable, env);
            try {
                if (iterable instanceof List<?> list) {
                    outer:
                    for (Object item : list) {
                        Environment loopEnv = new Environment(env);
                        loopEnv.define(n.varName, item);
                        try { eval(n.body, loopEnv); }
                        catch (ContinueException e) { continue outer; }
                    }
                } else if (iterable instanceof String s) {
                    outer:
                    for (int i = 0; i < s.length(); i++) {
                        Environment loopEnv = new Environment(env);
                        loopEnv.define(n.varName, String.valueOf(s.charAt(i)));
                        try { eval(n.body, loopEnv); }
                        catch (ContinueException e) { continue outer; }
                    }
                } else if (iterable instanceof Map<?,?> m) {
                    outer:
                    for (Object v : ((Map<String, Object>) m).values()) {
                        Environment loopEnv = new Environment(env);
                        loopEnv.define(n.varName, v);
                        try { eval(n.body, loopEnv); }
                        catch (ContinueException e) { continue outer; }
                    }
                } else {
                    throw new RuntimeException("TypeError: value is not iterable");
                }
            } catch (BreakException ignored) {}
            return UNDEFINED;
        }

        // ── for...in loop ──────────────────────────────────────
        if (node instanceof ASTNode.ForInStatement n) {
            Object obj = eval(n.object, env);
            List<String> keys = new ArrayList<>();
            if (obj instanceof Map<?,?> m) {
                for (Object k : m.keySet()) {
                    if (!"__constructor__".equals(k)) keys.add((String) k);
                }
            } else if (obj instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) keys.add(String.valueOf(i));
            }
            try {
                for (String key : keys) {
                    Environment loopEnv = new Environment(env);
                    loopEnv.define(n.varName, key);
                    eval(n.body, loopEnv);
                }
            } catch (BreakException ignored) {}
            return UNDEFINED;
        }

        // ── throw expr ──────────────────────────────────────────
        if (node instanceof ASTNode.ThrowStatement n) {
            throw new ThrowException(eval(n.value, env));
        }

        // ── try / catch / finally ────────────────────────────────
        if (node instanceof ASTNode.TryStatement n) {
            try {
                try {
                    eval(n.tryBlock, env);
                } catch (ReturnException | BreakException | ContinueException ce) {
                    throw ce;
                } catch (RuntimeException re) {
                    if (n.catchBlock == null) throw re;

                    Object errVal;
                    if (re instanceof ThrowException te) {
                        errVal = te.value;
                    } else {
                        // Build a proper error object with message property
                        Map<String, Object> errObj = new LinkedHashMap<>();
                        String msg = re.getMessage() != null ? re.getMessage() : "Unknown error";
                        // Strip Java class prefix like "RuntimeException: "
                        if (msg.contains(": ")) msg = msg.substring(msg.indexOf(": ") + 2);
                        errObj.put("name",    "Error");
                        errObj.put("message", msg);
                        errObj.put("stack",   re.toString());
                        errVal = errObj;
                    }

                    Environment catchEnv = new Environment(env);
                    if (n.catchParam != null) catchEnv.define(n.catchParam, errVal);
                    eval(n.catchBlock, catchEnv);
                }
            } finally {
                if (n.finallyBlock != null) eval(n.finallyBlock, env);
            }
            return UNDEFINED;
        }

        // ── function declaration ──────────────────────────────
        if (node instanceof ASTNode.FunctionDeclaration n) {
            JSFunction fn = new JSFunction(n.name, n.params, n.body, env);
            env.define(n.name, fn);
            return UNDEFINED;
        }

        // ── return statement ──────────────────────────────────
        if (node instanceof ASTNode.ReturnStatement n) {
            Object val = n.value != null ? eval(n.value, env) : UNDEFINED;
            throw new ReturnException(val);
        }

        // ── Binary operation  a OP b ──────────────────────────
        if (node instanceof ASTNode.BinaryOp n) {
            return evalBinaryOp(n, env);
        }

        // ── Unary operation  OP a ─────────────────────────────
        if (node instanceof ASTNode.UnaryOp n) {
            return evalUnaryOp(n, env);
        }

        // ── x++  /  x-- ──────────────────────────────────────
        if (node instanceof ASTNode.UpdateExpression n) {
            return evalUpdate(n, env);
        }

        // ── Assignment  x = value  /  x += value  etc. ───────
        if (node instanceof ASTNode.Assignment n) {
            return evalAssignment(n, env);
        }

        // ── Function call  foo(args) ──────────────────────────
        if (node instanceof ASTNode.CallExpression n) {
            return evalCall(n, env);
        }

        // ── Member access  obj.prop  /  obj[key] ─────────────
        if (node instanceof ASTNode.MemberExpression n) {
            return evalMember(n, env, null);
        }

        // ── Array literal  [1, 2, 3] ──────────────────────────
        if (node instanceof ASTNode.ArrayLiteral n) {
            List<Object> arr = new ArrayList<>();
            for (ASTNode elem : n.elements) {
                if (elem instanceof ASTNode.SpreadElement s) {
                    Object spread = eval(s.argument, env);
                    if (spread instanceof List<?> list) arr.addAll((List<Object>) list);
                } else {
                    arr.add(eval(elem, env));
                }
            }
            return arr;
        }

        // ── Object literal  { key: value } ───────────────────
        if (node instanceof ASTNode.ObjectLiteral n) {
            Map<String, Object> obj = new LinkedHashMap<>();
            for (Map.Entry<String, ASTNode> e : n.properties.entrySet()) {
                obj.put(e.getKey(), eval(e.getValue(), env));
            }
            return obj;
        }

        // ── Function expression or arrow function ─────────────
        if (node instanceof ASTNode.FunctionExpression n) {
            return new JSFunction(null, n.params, n.body, env);
        }

        // ── condition ? a : b ─────────────────────────────────
        if (node instanceof ASTNode.TernaryExpression n) {
            return isTruthy(eval(n.condition, env)) ? eval(n.thenExpr, env) : eval(n.elseExpr, env);
        }

        // ── Template literal `text ${expr} text` ──────────────
        if (node instanceof ASTNode.TemplateLiteral n) {
            StringBuilder sb = new StringBuilder();
            for (Object part : n.parts) {
                if (part instanceof String s) sb.append(s);
                else sb.append(jsToString(eval((ASTNode) part, env)));
            }
            return sb.toString();
        }

        // ── new Foo(args) ─────────────────────────────────────
        if (node instanceof ASTNode.NewExpression n) {
            return evalNew(n, env);
        }

        throw new RuntimeException("Unknown AST node: " + node.getClass().getSimpleName());
    }


    // ══════════════════════════════════════════════════════════
    //  BINARY OPERATIONS
    // ══════════════════════════════════════════════════════════


    // ── Evaluate left OP right with JS type rules ─────────────
    private Object evalBinaryOp(ASTNode.BinaryOp n, Environment env) {

        // Short-circuit for && and ||
        if (n.op.equals("&&")) {
            Object left = eval(n.left, env);
            return isTruthy(left) ? eval(n.right, env) : left;
        }
        if (n.op.equals("||")) {
            Object left = eval(n.left, env);
            return isTruthy(left) ? left : eval(n.right, env);
        }
        // Nullish coalescing ?? — return right only if left is null/undefined
        if (n.op.equals("??")) {
            Object left = eval(n.left, env);
            return (left == null || left == UNDEFINED) ? eval(n.right, env) : left;
        }

        Object left  = eval(n.left,  env);
        Object right = eval(n.right, env);

        switch (n.op) {
            case "+":
                // string concat OR numeric add
                if (left instanceof String || right instanceof String)
                    return jsToString(left) + jsToString(right);
                return toNumber(left) + toNumber(right);
            case "-":   return toNumber(left) - toNumber(right);
            case "*":   return toNumber(left) * toNumber(right);
            case "/":   return toNumber(left) / toNumber(right);
            case "%":   return toNumber(left) % toNumber(right);
            case "**":  return Math.pow(toNumber(left), toNumber(right));
            case "===": return jsStrictEqual(left, right);
            case "!==": return !jsStrictEqual(left, right);
            case "==":  return jsLooseEqual(left, right);
            case "!=":  return !jsLooseEqual(left, right);
            // String-aware comparisons
            case "<":   return (left instanceof String && right instanceof String)
                               ? ((String)left).compareTo((String)right) < 0
                               : toNumber(left) < toNumber(right);
            case ">":   return (left instanceof String && right instanceof String)
                               ? ((String)left).compareTo((String)right) > 0
                               : toNumber(left) > toNumber(right);
            case "<=":  return (left instanceof String && right instanceof String)
                               ? ((String)left).compareTo((String)right) <= 0
                               : toNumber(left) <= toNumber(right);
            case ">=":  return (left instanceof String && right instanceof String)
                               ? ((String)left).compareTo((String)right) >= 0
                               : toNumber(left) >= toNumber(right);
            // ── x instanceof Ctor ─────────────────────────────
            case "instanceof":
                if (!(right instanceof JSFunction)) {
                    throw new RuntimeException("TypeError: Right-hand side of 'instanceof' is not callable");
                }
                if (!(left instanceof Map<?,?> m)) return false;
                return m.get("__constructor__") == right;
            default:    throw new RuntimeException("Unknown operator: " + n.op);
        }
    }


    // ── Bind a value to a pattern (Identifier / ArrayPattern / ObjectPattern) ─
    @SuppressWarnings("unchecked")
    private void bindPattern(ASTNode pattern, Object value, Environment env) {

        if (pattern instanceof ASTNode.Identifier id) {
            env.define(id.name, value);
            return;
        }

        if (pattern instanceof ASTNode.ArrayPattern ap) {
            List<Object> list = (value instanceof List<?> l) ? (List<Object>) l : new ArrayList<>();
            int idx = 0;
            for (ASTNode.Param el : ap.elements) {
                if (el == null) {
                    // Elision: a "hole" in the pattern, e.g. [, second] — skip this slot
                    idx++;
                    continue;
                }
                if (el.rest) {
                    List<Object> rest = idx < list.size()
                        ? new ArrayList<>(list.subList(idx, list.size()))
                        : new ArrayList<>();
                    bindPattern(el.pattern, rest, env);
                    break;
                }
                Object v = idx < list.size() ? list.get(idx) : UNDEFINED;
                if (v == UNDEFINED && el.defaultValue != null) v = eval(el.defaultValue, env);
                bindPattern(el.pattern, v, env);
                idx++;
            }
            return;
        }

        if (pattern instanceof ASTNode.ObjectPattern op) {
            Map<String, Object> map = (value instanceof Map<?,?> m) ? (Map<String, Object>) m : new LinkedHashMap<>();
            Set<String> usedKeys = new HashSet<>();
            for (Map.Entry<String, ASTNode.Param> entry : op.properties.entrySet()) {
                String key = entry.getKey();
                ASTNode.Param p = entry.getValue();
                usedKeys.add(key);
                Object v = map.containsKey(key) ? map.get(key) : UNDEFINED;
                if (v == UNDEFINED && p.defaultValue != null) v = eval(p.defaultValue, env);
                bindPattern(p.pattern, v, env);
            }
            if (op.restName != null) {
                Map<String, Object> rest = new LinkedHashMap<>();
                for (Map.Entry<String, Object> e : map.entrySet()) {
                    if (!usedKeys.contains(e.getKey()) && !"__constructor__".equals(e.getKey())) {
                        rest.put(e.getKey(), e.getValue());
                    }
                }
                env.define(op.restName, rest);
            }
            return;
        }

        throw new RuntimeException("Invalid binding pattern: " + pattern.getClass().getSimpleName());
    }


    // ── Evaluate prefix unary operators ───────────────────────
    private Object evalUnaryOp(ASTNode.UnaryOp n, Environment env) {
        Object val = eval(n.operand, env);
        switch (n.op) {
            case "!":      return !isTruthy(val);
            case "-":      return -toNumber(val);
            case "+":      return toNumber(val);
            case "typeof":
                if (val == null)               return "object";
                if (val == UNDEFINED)          return "undefined";
                if (val instanceof Boolean)    return "boolean";
                if (val instanceof Double)     return "number";
                if (val instanceof String)     return "string";
                if (val instanceof JSFunction) return "function";
                return "object";
            default: throw new RuntimeException("Unknown unary op: " + n.op);
        }
    }


    // ── x++  /  x--  /  ++x  /  --x ─────────────────────────
    private Object evalUpdate(ASTNode.UpdateExpression n, Environment env) {
        double current = toNumber(evalLValue(n.operand, env));
        double updated = n.op.equals("++") ? current + 1 : current - 1;
        assignLValue(n.operand, updated, env);
        return n.prefix ? updated : current;
    }


    // ══════════════════════════════════════════════════════════
    //  ASSIGNMENT
    // ══════════════════════════════════════════════════════════


    // ── Handle = and compound assignments like += ─────────────
    @SuppressWarnings("unchecked")
    private Object evalAssignment(ASTNode.Assignment n, Environment env) {
        Object value = eval(n.value, env);

        if (!n.op.equals("=")) {
            Object current = evalLValue(n.target, env);
            switch (n.op) {
                case "+=":
                    value = (current instanceof String || value instanceof String)
                            ? jsToString(current) + jsToString(value)
                            : toNumber(current) + toNumber(value);
                    break;
                case "-=": value = toNumber(current) - toNumber(value); break;
                case "*=": value = toNumber(current) * toNumber(value); break;
                case "/=": value = toNumber(current) / toNumber(value); break;
            }
        }

        assignLValue(n.target, value, env);
        return value;
    }


    // ── Read the current value of a variable or property ──────
    private Object evalLValue(ASTNode node, Environment env) {
        if (node instanceof ASTNode.Identifier n)       return env.get(n.name);
        if (node instanceof ASTNode.MemberExpression n) return evalMember(n, env, null);
        throw new RuntimeException("Invalid assignment target");
    }


    // ── Write a value to a variable or object property ────────
    @SuppressWarnings("unchecked")
    private void assignLValue(ASTNode node, Object value, Environment env) {
        if (node instanceof ASTNode.Identifier n) {
            env.set(n.name, value);
        } else if (node instanceof ASTNode.MemberExpression n) {
            Object obj = eval(n.object, env);
            String key = n.computed != null
                         ? jsToString(eval(n.computed, env))
                         : n.property;
            if (obj instanceof Map<?,?> m) {
                ((Map<String, Object>) m).put(key, value);
            } else if (obj instanceof List<?>) {
                int idx = (int) toNumber(eval(n.computed, env));
                ((List<Object>) obj).set(idx, value);
            }
        }
    }


    // ══════════════════════════════════════════════════════════
    //  FUNCTION CALLS
    // ══════════════════════════════════════════════════════════


    // ── Evaluate a function call expression ───────────────────
    @SuppressWarnings("unchecked")
    private Object evalCall(ASTNode.CallExpression n, Environment env) {
        Object thisObj = null;
        Object callee;

        // For method calls: resolve object and method separately
        if (n.callee instanceof ASTNode.MemberExpression mem) {
            thisObj = eval(mem.object, env);
            if (mem.optional && (thisObj == null || thisObj == UNDEFINED)) return UNDEFINED;
            callee  = evalMember(mem, env, thisObj);
        } else {
            callee = eval(n.callee, env);
        }

        // Build argument list (with spread support)
        List<Object> args = new ArrayList<>();
        for (ASTNode arg : n.args) {
            if (arg instanceof ASTNode.SpreadElement s) {
                Object spread = eval(s.argument, env);
                if (spread instanceof List<?> list) args.addAll((List<Object>) list);
            } else {
                args.add(eval(arg, env));
            }
        }

        return callFunction(callee, args, thisObj, env);
    }


    // ── Actually invoke a function (user-defined or built-in) ─
    private Object callFunction(Object callee, List<Object> args, Object thisObj, Environment env) {
        if (callee instanceof JSBuiltin b) {
            return b.call(args);
        }

        if (callee instanceof JSFunction fn) {
            Environment fnEnv = new Environment(fn.closureEnv);
            // Bind parameters to arguments (supports defaults, destructuring, rest)
            for (int i = 0; i < fn.params.size(); i++) {
                ASTNode.Param p = fn.params.get(i);
                if (p.rest) {
                    List<Object> rest = new ArrayList<>(args.subList(Math.min(i, args.size()), args.size()));
                    bindPattern(p.pattern, rest, fnEnv);
                    break;
                }
                Object val = i < args.size() ? args.get(i) : UNDEFINED;
                if (val == UNDEFINED && p.defaultValue != null) {
                    val = eval(p.defaultValue, fnEnv);
                }
                bindPattern(p.pattern, val, fnEnv);
            }
            if (thisObj != null) fnEnv.define("this", thisObj);

            // Arrow functions with an expression body (no braces) return that value directly
            if (!(fn.body instanceof ASTNode.Block)) {
                return eval(fn.body, fnEnv);
            }

            try {
                eval(fn.body, fnEnv);
            } catch (ReturnException ret) {
                return ret.value;
            }
            return UNDEFINED;
        }

        throw new RuntimeException("TypeError: " + jsToString(callee) + " is not a function");
    }


    // ── Handle new Foo(args) ──────────────────────────────────
    @SuppressWarnings("unchecked")
    private Object evalNew(ASTNode.NewExpression n, Environment env) {
        Object callee = eval(n.callee, env);

        List<Object> args = new ArrayList<>();
        for (ASTNode arg : n.args) args.add(eval(arg, env));

        if (callee instanceof JSBuiltin b) return b.call(args);

        if (callee instanceof JSFunction fn) {
            Map<String, Object> instance = new LinkedHashMap<>();
            instance.put("__constructor__", fn);
            callFunction(fn, args, instance, env);
            return instance;
        }

        throw new RuntimeException("TypeError: not a constructor");
    }


    // ══════════════════════════════════════════════════════════
    //  MEMBER ACCESS  (obj.prop  /  obj[key])
    //  Also handles ALL built-in array and string methods
    // ══════════════════════════════════════════════════════════


    @SuppressWarnings("unchecked")
    private Object evalMember(ASTNode.MemberExpression n, Environment env, Object preEvaluatedObj) {
        Object obj = preEvaluatedObj != null ? preEvaluatedObj : eval(n.object, env);
        String key = n.computed != null
                     ? jsToString(eval(n.computed, env))
                     : n.property;


        // ── Array methods and properties ──────────────────────
        if (obj instanceof List<?> rawList) {
            List<Object> arr = (List<Object>) rawList;

            if (key.equals("length"))   return (double) arr.size();
            if (key.equals("push"))     return (JSBuiltin) args -> { arr.addAll(args); return (double) arr.size(); };
            if (key.equals("pop"))      return (JSBuiltin) args -> arr.isEmpty() ? UNDEFINED : arr.remove(arr.size() - 1);
            if (key.equals("shift"))    return (JSBuiltin) args -> arr.isEmpty() ? UNDEFINED : arr.remove(0);
            if (key.equals("unshift"))  return (JSBuiltin) args -> { arr.addAll(0, args); return (double) arr.size(); };
            if (key.equals("reverse"))  return (JSBuiltin) args -> { Collections.reverse(arr); return arr; };
            if (key.equals("includes")) return (JSBuiltin) args -> arr.stream().anyMatch(x -> jsStrictEqual(x, args.get(0)));

            if (key.equals("join")) return (JSBuiltin) args -> {
                String sep = args.isEmpty() ? "," : jsToString(args.get(0));
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.size(); i++) {
                    if (i > 0) sb.append(sep);
                    sb.append(jsToString(arr.get(i)));
                }
                return sb.toString();
            };

            if (key.equals("toString")) return (JSBuiltin) args -> {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(jsToString(arr.get(i)));
                }
                return sb.toString();
            };

            if (key.equals("indexOf")) return (JSBuiltin) args -> {
                for (int i = 0; i < arr.size(); i++)
                    if (jsStrictEqual(arr.get(i), args.get(0))) return (double) i;
                return -1.0;
            };

            if (key.equals("slice")) return (JSBuiltin) args -> {
                int from = args.size() > 0 ? (int) toNumber(args.get(0)) : 0;
                int to   = args.size() > 1 ? (int) toNumber(args.get(1)) : arr.size();
                if (from < 0) from = Math.max(0, arr.size() + from);
                if (to   < 0) to   = Math.max(0, arr.size() + to);
                return new ArrayList<>(arr.subList(Math.min(from, arr.size()), Math.min(to, arr.size())));
            };

            if (key.equals("splice")) return (JSBuiltin) args -> {
                int start  = (int) toNumber(args.get(0));
                int delCnt = args.size() > 1 ? (int) toNumber(args.get(1)) : arr.size() - start;
                List<Object> removed = new ArrayList<>(arr.subList(start, Math.min(start + delCnt, arr.size())));
                arr.subList(start, Math.min(start + delCnt, arr.size())).clear();
                for (int i = 2; i < args.size(); i++) arr.add(start + i - 2, args.get(i));
                return removed;
            };

            if (key.equals("concat")) return (JSBuiltin) args -> {
                List<Object> res = new ArrayList<>(arr);
                for (Object a : args) {
                    if (a instanceof List<?> l) res.addAll((List<Object>) l);
                    else res.add(a);
                }
                return res;
            };

            if (key.equals("find")) return (JSBuiltin) args -> {
                JSFunction fn = (JSFunction) args.get(0);
                for (Object el : arr)
                    if (isTruthy(callFunction(fn, List.of(el), null, env))) return el;
                return UNDEFINED;
            };

            if (key.equals("findIndex")) return (JSBuiltin) args -> {
                JSFunction fn = (JSFunction) args.get(0);
                for (int i = 0; i < arr.size(); i++)
                    if (isTruthy(callFunction(fn, List.of(arr.get(i)), null, env))) return (double) i;
                return -1.0;
            };

            if (key.equals("some")) return (JSBuiltin) args -> {
                JSFunction fn = (JSFunction) args.get(0);
                for (Object el : arr)
                    if (isTruthy(callFunction(fn, List.of(el), null, env))) return true;
                return false;
            };

            if (key.equals("every")) return (JSBuiltin) args -> {
                JSFunction fn = (JSFunction) args.get(0);
                for (Object el : arr)
                    if (!isTruthy(callFunction(fn, List.of(el), null, env))) return false;
                return true;
            };

            if (key.equals("map")) return (JSBuiltin) args -> {
                JSFunction fn = (JSFunction) args.get(0);
                List<Object> res = new ArrayList<>();
                for (int i = 0; i < arr.size(); i++)
                    res.add(callFunction(fn, List.of(arr.get(i), (double) i, arr), null, env));
                return res;
            };

            if (key.equals("filter")) return (JSBuiltin) args -> {
                JSFunction fn = (JSFunction) args.get(0);
                List<Object> res = new ArrayList<>();
                for (int i = 0; i < arr.size(); i++)
                    if (isTruthy(callFunction(fn, List.of(arr.get(i), (double) i, arr), null, env)))
                        res.add(arr.get(i));
                return res;
            };

            if (key.equals("reduce")) return (JSBuiltin) args -> {
                JSFunction fn = (JSFunction) args.get(0);
                Object acc    = args.size() > 1 ? args.get(1) : arr.get(0);
                int start     = args.size() > 1 ? 0 : 1;
                for (int i = start; i < arr.size(); i++)
                    acc = callFunction(fn, List.of(acc, arr.get(i), (double) i, arr), null, env);
                return acc;
            };

            if (key.equals("forEach")) return (JSBuiltin) args -> {
                JSFunction fn = (JSFunction) args.get(0);
                for (int i = 0; i < arr.size(); i++)
                    callFunction(fn, List.of(arr.get(i), (double) i, arr), null, env);
                return UNDEFINED;
            };

            if (key.equals("sort")) return (JSBuiltin) args -> {
                if (!args.isEmpty() && args.get(0) instanceof JSFunction fn) {
                    arr.sort((a, b) -> (int) toNumber(callFunction(fn, List.of(a, b), null, env)));
                } else {
                    arr.sort(Comparator.comparing(this::jsToString));
                }
                return arr;
            };

            if (key.equals("flat")) return (JSBuiltin) args -> {
                List<Object> res = new ArrayList<>();
                for (Object el : arr) {
                    if (el instanceof List<?> l) res.addAll((List<Object>) l);
                    else res.add(el);
                }
                return res;
            };

            if (key.equals("fill")) return (JSBuiltin) args -> {
                Object val = args.get(0);
                for (int i = 0; i < arr.size(); i++) arr.set(i, val);
                return arr;
            };

            // numeric index access  arr[0]
            try {
                int idx = Integer.parseInt(key);
                return (idx >= 0 && idx < arr.size()) ? arr.get(idx) : UNDEFINED;
            } catch (NumberFormatException e) {
                return UNDEFINED;
            }
        }


        // ── String methods and properties ─────────────────────
        if (obj instanceof String str) {

            if (key.equals("length"))      return (double) str.length();
            if (key.equals("toUpperCase")) return (JSBuiltin) args -> str.toUpperCase();
            if (key.equals("toLowerCase")) return (JSBuiltin) args -> str.toLowerCase();
            if (key.equals("trim"))        return (JSBuiltin) args -> str.trim();
            if (key.equals("trimStart"))   return (JSBuiltin) args -> str.stripLeading();
            if (key.equals("trimEnd"))     return (JSBuiltin) args -> str.stripTrailing();
            if (key.equals("includes"))    return (JSBuiltin) args -> str.contains(jsToString(args.get(0)));
            if (key.equals("startsWith"))  return (JSBuiltin) args -> str.startsWith(jsToString(args.get(0)));
            if (key.equals("endsWith"))    return (JSBuiltin) args -> str.endsWith(jsToString(args.get(0)));
            if (key.equals("indexOf"))     return (JSBuiltin) args -> (double) str.indexOf(jsToString(args.get(0)));
            if (key.equals("lastIndexOf")) return (JSBuiltin) args -> (double) str.lastIndexOf(jsToString(args.get(0)));
            if (key.equals("charAt"))      return (JSBuiltin) args -> String.valueOf(str.charAt((int) toNumber(args.get(0))));
            if (key.equals("charCodeAt"))  return (JSBuiltin) args -> (double) str.charAt((int) toNumber(args.get(0)));
            if (key.equals("repeat"))      return (JSBuiltin) args -> str.repeat((int) toNumber(args.get(0)));
            if (key.equals("toString"))    return (JSBuiltin) args -> str;
            if (key.equals("valueOf"))     return (JSBuiltin) args -> str;

            if (key.equals("slice")) return (JSBuiltin) args -> {
                int from = (int) toNumber(args.get(0));
                int to   = args.size() > 1 ? (int) toNumber(args.get(1)) : str.length();
                if (from < 0) from = Math.max(0, str.length() + from);
                if (to   < 0) to   = Math.max(0, str.length() + to);
                return str.substring(Math.min(from, str.length()), Math.min(to, str.length()));
            };

            if (key.equals("substring")) return (JSBuiltin) args -> {
                int from = (int) toNumber(args.get(0));
                int to   = args.size() > 1 ? (int) toNumber(args.get(1)) : str.length();
                return str.substring(Math.min(from, str.length()), Math.min(to, str.length()));
            };

            if (key.equals("split")) return (JSBuiltin) args -> {
                String sep = jsToString(args.get(0));
                List<Object> parts = new ArrayList<>();
                if (sep.isEmpty()) {
                    for (char ch : str.toCharArray()) parts.add(String.valueOf(ch));
                } else {
                    for (String p : str.split(java.util.regex.Pattern.quote(sep), -1)) parts.add(p);
                }
                return parts;
            };

            if (key.equals("replace")) return (JSBuiltin) args -> {
                String pat = jsToString(args.get(0));
                String rep = jsToString(args.get(1));
                return str.replaceFirst(java.util.regex.Pattern.quote(pat), java.util.regex.Matcher.quoteReplacement(rep));
            };

            if (key.equals("replaceAll")) return (JSBuiltin) args -> {
                String pat = jsToString(args.get(0));
                String rep = jsToString(args.get(1));
                return str.replace(pat, rep);
            };

            if (key.equals("padStart")) return (JSBuiltin) args -> {
                int len    = (int) toNumber(args.get(0));
                String p   = args.size() > 1 ? jsToString(args.get(1)) : " ";
                if (str.length() >= len) return str;
                int needed = len - str.length();
                return p.repeat((needed + p.length() - 1) / p.length()).substring(0, needed) + str;
            };

            if (key.equals("padEnd")) return (JSBuiltin) args -> {
                int len    = (int) toNumber(args.get(0));
                String p   = args.size() > 1 ? jsToString(args.get(1)) : " ";
                if (str.length() >= len) return str;
                int needed = len - str.length();
                return str + p.repeat((needed + p.length() - 1) / p.length()).substring(0, needed);
            };

            // character index access  "hello"[0]
            try {
                int idx = Integer.parseInt(key);
                return (idx >= 0 && idx < str.length()) ? String.valueOf(str.charAt(idx)) : UNDEFINED;
            } catch (NumberFormatException e) {
                return UNDEFINED;
            }
        }


        // ── Plain object (Map) property access ────────────────
        if (obj instanceof Map<?,?> m) {
            Object val = ((Map<String, Object>) m).get(key);
            return val != null ? val : UNDEFINED;
        }


        // ── Number methods ────────────────────────────────────
        if (obj instanceof Double d) {
            if (key.equals("toString")) return (JSBuiltin) args -> {
                int radix = args.isEmpty() ? 10 : (int) toNumber(args.get(0));
                return radix == 10 ? formatNumber(d) : Long.toString(d.longValue(), radix);
            };
            if (key.equals("toFixed")) return (JSBuiltin) args -> {
                int digits = args.isEmpty() ? 0 : (int) toNumber(args.get(0));
                return String.format("%." + digits + "f", d);
            };
        }

        return UNDEFINED;
    }


    // ══════════════════════════════════════════════════════════
    //  TYPE CONVERSION UTILITIES
    // ══════════════════════════════════════════════════════════


    // ── Convert any JS value to a Java double ─────────────────
    public double toNumber(Object val) {
        if (val == UNDEFINED) return Double.NaN;
        if (val == null)      return 0.0;
        if (val instanceof Double d)         return d;
        if (val instanceof Boolean b)        return b ? 1.0 : 0.0;
        if (val instanceof String s) {
            s = s.trim();
            if (s.isEmpty()) return 0.0;
            try { return Double.parseDouble(s); }
            catch (NumberFormatException e) { return Double.NaN; }
        }
        return Double.NaN;
    }


    // ── Convert any JS value to its string representation ─────
    @SuppressWarnings("unchecked")
    public String jsToString(Object val) {
        if (val == null)             return "null";
        if (val == UNDEFINED)        return "undefined";
        if (val instanceof Boolean b) return b.toString();
        if (val instanceof Double d)  return formatNumber(d);
        if (val instanceof String s)  return s;
        if (val instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(jsToString(list.get(i)));
            }
            return sb.toString();
        }
        if (val instanceof Map<?,?>) return "[object Object]";
        if (val instanceof JSFunction fn) return fn.toString();
        return val.toString();
    }


    // ── Format a double the way JS does (no trailing .0) ──────
    private String formatNumber(double d) {
        if (Double.isNaN(d))      return "NaN";
        if (Double.isInfinite(d)) return d > 0 ? "Infinity" : "-Infinity";
        if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15)
            return String.valueOf((long) d);
        return String.valueOf(d);
    }


    // ── Determine if a value is truthy by JS rules ────────────
    public boolean isTruthy(Object val) {
        if (val == null || val == UNDEFINED) return false;
        if (val instanceof Boolean b)        return b;
        if (val instanceof Double d)         return d != 0.0 && !Double.isNaN(d);
        if (val instanceof String s)         return !s.isEmpty();
        return true;
    }


    // ── JS strict equality (===) ──────────────────────────────
    private boolean jsStrictEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a == UNDEFINED && b == UNDEFINED) return true;
        if (a == UNDEFINED || b == UNDEFINED) return false;
        // NaN is never equal to anything, including itself
        if (a instanceof Double da && Double.isNaN(da)) return false;
        if (b instanceof Double db && Double.isNaN(db)) return false;
        return a.equals(b);
    }


    // ── JS loose equality (==) with type coercion ─────────────
    private boolean jsLooseEqual(Object a, Object b) {
        if (jsStrictEqual(a, b)) return true;
        // null == undefined (and vice versa) is true; null/undefined
        // are otherwise only ever equal to each other
        if ((a == null && b == UNDEFINED) || (a == UNDEFINED && b == null)) return true;
        if (a == null || b == null || a == UNDEFINED || b == UNDEFINED) return false;
        if (a instanceof Double  && b instanceof String)  return a.equals(toNumber(b));
        if (a instanceof String  && b instanceof Double)  return toNumber(a) == (Double) b;
        if (a instanceof Boolean) return jsLooseEqual(toNumber(a), b);
        if (b instanceof Boolean) return jsLooseEqual(a, toNumber(b));
        return false;
    }
}
