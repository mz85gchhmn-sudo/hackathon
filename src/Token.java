// ─────────────────────────────────────────────────────────────
// Token.java
// Defines every possible token type in JavaScript
// and the Token object that holds a type + its raw value
// ─────────────────────────────────────────────────────────────

public class Token {


    // ── All possible token types ──────────────────────────────
    public enum Type {

        // Literals
        NUMBER, STRING, BOOLEAN, NULL, UNDEFINED,

        // Identifier (variable names, function names, etc.)
        IDENTIFIER,

        // Keywords
        LET, CONST, VAR,
        IF, ELSE,
        FOR, WHILE, DO,
        FUNCTION, RETURN,
        SWITCH, CASE, BREAK, CONTINUE, DEFAULT,
        TRY, CATCH, FINALLY, THROW,
        TEMPLATE,
        NEW, TYPEOF, INSTANCEOF,
        TRUE, FALSE,

        // Arithmetic operators
        PLUS, MINUS, STAR, SLASH, PERCENT, STAR_STAR,

        // Comparison operators
        EQ_EQ, EQ_EQ_EQ,
        NOT_EQ, NOT_EQ_EQ,
        LT, LT_EQ,
        GT, GT_EQ,

        // Logical operators
        AND, OR, NOT,

        // Assignment operators
        ASSIGN,
        PLUS_ASSIGN, MINUS_ASSIGN,
        STAR_ASSIGN, SLASH_ASSIGN,

        // Increment / Decrement
        PLUS_PLUS, MINUS_MINUS,

        // Punctuation
        LPAREN, RPAREN,
        LBRACE, RBRACE,
        LBRACKET, RBRACKET,
        SEMICOLON, COMMA, DOT,
        COLON, QUESTION, QUESTION_DOT, NULLISH,  // ?? operator
        SPREAD,         // ...

        // Arrow for arrow functions
        ARROW,          // =>

        // End of file
        EOF
    }


    // ── Fields ────────────────────────────────────────────────
    public final Type type;
    public final String value;


    // ── Constructor ───────────────────────────────────────────
    public Token(Type type, String value) {
        this.type  = type;
        this.value = value;
    }


    // ── Nice string form for debugging ────────────────────────
    @Override
    public String toString() {
        return "Token(" + type + ", " + value + ")";
    }
}
