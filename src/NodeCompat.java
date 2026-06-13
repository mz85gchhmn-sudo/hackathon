// ─────────────────────────────────────────────────────────────
// NodeCompat.java
// BONUS: Optional Node.js compatibility shims
//
// This file is intentionally separate from Interpreter.java so the
// "core" interpreter stays a clean implementation of the JS language
// itself. NodeCompat just registers a few extra global objects that
// real Node.js scripts often rely on (process, etc.), so that simple
// Node-style scripts run without crashing on "X is not defined".
//
// To enable: call NodeCompat.install(globalEnv) once during
// Interpreter setup (see setupBuiltins() in Interpreter.java).
// ─────────────────────────────────────────────────────────────

import java.util.LinkedHashMap;
import java.util.Map;

public class NodeCompat {

    // ── Install Node-like globals into the given environment ──
    public static void install(Environment env) {

        // ── process.stdout.write / process.argv / process.env ─
        Map<String, Object> stdout = new LinkedHashMap<>();
        stdout.put("write", (Interpreter.JSBuiltin) args -> {
            // process.stdout.write does NOT add a trailing newline,
            // unlike console.log
            for (Object a : args) System.out.print(jsToStringSimple(a));
            return true;
        });

        Map<String, Object> stderr = new LinkedHashMap<>();
        stderr.put("write", (Interpreter.JSBuiltin) args -> {
            for (Object a : args) System.err.print(jsToStringSimple(a));
            return true;
        });

        Map<String, Object> process = new LinkedHashMap<>();
        process.put("stdout", stdout);
        process.put("stderr", stderr);
        process.put("argv", new java.util.ArrayList<Object>());   // empty by default
        process.put("env", new LinkedHashMap<String, Object>());  // empty by default
        process.put("platform", "js-interpreter");
        process.put("exit", (Interpreter.JSBuiltin) args -> {
            int code = args.isEmpty() ? 0 : (int) Double.parseDouble(jsToStringSimple(args.get(0)));
            System.exit(code);
            return Interpreter.UNDEFINED;
        });

        env.define("process", process);
    }


    // ── Minimal stringify, mirrors Interpreter.jsToString for ─
    // ── the simple value types process.stdout.write is given ──
    private static String jsToStringSimple(Object val) {
        if (val == null)              return "null";
        if (val == Interpreter.UNDEFINED) return "undefined";
        if (val instanceof Double d) {
            if (d == Math.floor(d) && !d.isInfinite()) return String.valueOf(d.longValue());
            return String.valueOf(d);
        }
        return String.valueOf(val);
    }
}
