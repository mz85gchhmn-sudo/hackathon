// ─────────────────────────────────────────────────────────────
// Environment.java
// Represents a single scope layer (global, function, block)
// Each scope has a reference to its parent scope
// Variable lookup walks up the chain until found
// ─────────────────────────────────────────────────────────────

import java.util.HashMap;
import java.util.Map;

public class Environment {


    // ── Variable storage for this scope level ─────────────────
    private final Map<String, Object> store = new HashMap<>();


    // ── Link to the enclosing (parent) scope ──────────────────
    // null means this is the global scope
    private final Environment parent;


    // ── Constructor ───────────────────────────────────────────
    public Environment(Environment parent) {
        this.parent = parent;
    }


    // ── Define a brand-new variable in the CURRENT scope ──────
    // Always use this for let / const / var declarations
    public void define(String name, Object value) {
        store.put(name, value);
    }


    // ── Look up a variable, walking up the scope chain ────────
    // Throws if not found anywhere
    public Object get(String name) {
        if (store.containsKey(name)) return store.get(name);
        if (parent != null)          return parent.get(name);
        throw new RuntimeException("ReferenceError: " + name + " is not defined");
    }


    // ── Update an existing variable wherever it was defined ───
    // Walks up the chain to find the right scope, then updates
    public void set(String name, Object value) {
        if (store.containsKey(name)) {
            store.put(name, value);
            return;
        }
        if (parent != null) {
            parent.set(name, value);
            return;
        }
        // If not found anywhere, define it in global scope (loose mode behavior)
        store.put(name, value);
    }


    // ── Check if a variable exists in any scope ───────────────
    public boolean has(String name) {
        if (store.containsKey(name)) return true;
        if (parent != null)          return parent.has(name);
        return false;
    }
}
