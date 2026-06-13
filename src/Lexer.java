// ─────────────────────────────────────────────────────────────
// Lexer.java
// Reads raw JavaScript source code character by character
// and produces a flat list of Token objects for the Parser
// ─────────────────────────────────────────────────────────────

import java.util.ArrayList;
import java.util.List;

public class Lexer {


    // ── Internal state ────────────────────────────────────────
    private final String src;   // the full JS source code
    private int pos;            // current read position
    private final List<Token> tokens = new ArrayList<>();


    // ── Constructor ───────────────────────────────────────────
    public Lexer(String src) {
        this.src = src;
        this.pos = 0;
    }


    // ── Main entry point ──────────────────────────────────────
    // Call this to get the full token list
    public List<Token> tokenize() {
        while (pos < src.length()) {
            skipWhitespaceAndComments();
            if (pos >= src.length()) break;

            char c = src.charAt(pos);

            if (Character.isDigit(c))                  readNumber();
            else if (c == '`')                         readTemplate();
            else if (c == '"' || c == '\'')            readString(c);
            else if (Character.isLetter(c) || c == '_' || c == '$') readIdentifierOrKeyword();
            else                                        readSymbol();
        }

        tokens.add(new Token(Token.Type.EOF, ""));
        return tokens;
    }


    // ── Skip spaces, newlines, and // or /* */ comments ───────
    private void skipWhitespaceAndComments() {
        while (pos < src.length()) {
            char c = src.charAt(pos);

            // whitespace
            if (Character.isWhitespace(c)) {
                pos++;
            }

            // single-line comment  //
            else if (peek(0) == '/' && peek(1) == '/') {
                while (pos < src.length() && src.charAt(pos) != '\n') pos++;
            }

            // multi-line comment  /* ... */
            else if (peek(0) == '/' && peek(1) == '*') {
                pos += 2;
                while (pos + 1 < src.length() &&
                       !(src.charAt(pos) == '*' && src.charAt(pos + 1) == '/')) {
                    pos++;
                }
                pos += 2; // skip closing */
            }

            else break;
        }
    }


    // ── Read a number literal (integer or decimal) ─────────────
    private void readNumber() {
        int start = pos;
        while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
            pos++;
        }
        tokens.add(new Token(Token.Type.NUMBER, src.substring(start, pos)));
    }


    // ── Read a template literal `...${expr}...`  ───────────────
    // Keeps escapes and ${...} placeholders raw; Parser splits them
    private void readTemplate() {
        pos++; // skip opening `
        StringBuilder sb = new StringBuilder();

        while (pos < src.length() && src.charAt(pos) != '`') {
            char c = src.charAt(pos);

            if (c == '\\' && pos + 1 < src.length()) {
                sb.append(c).append(src.charAt(pos + 1));
                pos += 2;
                continue;
            }

            if (c == '$' && peek(1) == '{') {
                sb.append("${");
                pos += 2;
                int depth = 1;
                while (pos < src.length() && depth > 0) {
                    char cc = src.charAt(pos);
                    if (cc == '{') depth++;
                    else if (cc == '}') { depth--; if (depth == 0) break; }
                    sb.append(cc);
                    pos++;
                }
                sb.append('}');
                pos++; // skip closing }
                continue;
            }

            sb.append(c);
            pos++;
        }

        pos++; // skip closing `
        tokens.add(new Token(Token.Type.TEMPLATE, sb.toString()));
    }


    // ── Read a string literal with matching quote ──────────────
    private void readString(char quote) {
        pos++; // skip opening quote
        StringBuilder sb = new StringBuilder();

        while (pos < src.length() && src.charAt(pos) != quote) {
            if (src.charAt(pos) == '\\') {
                pos++; // skip backslash
                char esc = src.charAt(pos);
                switch (esc) {
                    case 'n'  -> sb.append('\n');
                    case 't'  -> sb.append('\t');
                    case '\\' -> sb.append('\\');
                    case '\'' -> sb.append('\'');
                    case '"'  -> sb.append('"');
                    default   -> sb.append(esc);
                }
            } else {
                sb.append(src.charAt(pos));
            }
            pos++;
        }

        pos++; // skip closing quote
        tokens.add(new Token(Token.Type.STRING, sb.toString()));
    }


    // ── Read identifier and check if it's a keyword ───────────
    private void readIdentifierOrKeyword() {
        int start = pos;
        while (pos < src.length() &&
               (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_' || src.charAt(pos) == '$')) {
            pos++;
        }

        String word = src.substring(start, pos);

        // Map keywords to their specific token types
        Token.Type type = switch (word) {
            case "let"        -> Token.Type.LET;
            case "const"      -> Token.Type.CONST;
            case "var"        -> Token.Type.VAR;
            case "if"         -> Token.Type.IF;
            case "else"       -> Token.Type.ELSE;
            case "for"        -> Token.Type.FOR;
            case "while"      -> Token.Type.WHILE;
            case "do"         -> Token.Type.DO;
            case "function"   -> Token.Type.FUNCTION;
            case "return"     -> Token.Type.RETURN;
            case "switch"     -> Token.Type.SWITCH;
            case "case"       -> Token.Type.CASE;
            case "break"      -> Token.Type.BREAK;
            case "continue"   -> Token.Type.CONTINUE;
            case "default"    -> Token.Type.DEFAULT;
            case "try"        -> Token.Type.TRY;
            case "catch"      -> Token.Type.CATCH;
            case "finally"    -> Token.Type.FINALLY;
            case "throw"      -> Token.Type.THROW;
            case "new"        -> Token.Type.NEW;
            case "typeof"     -> Token.Type.TYPEOF;
            case "instanceof" -> Token.Type.INSTANCEOF;
            case "true"       -> Token.Type.TRUE;
            case "false"      -> Token.Type.FALSE;
            case "null"       -> Token.Type.NULL;
            case "undefined"  -> Token.Type.UNDEFINED;
            default           -> Token.Type.IDENTIFIER;
        };

        tokens.add(new Token(type, word));
    }


    // ── Read all operator and punctuation symbols ──────────────
    private void readSymbol() {
        char c = src.charAt(pos);

        switch (c) {

            case '+' -> {
                if (peek(1) == '+')      { tokens.add(new Token(Token.Type.PLUS_PLUS,   "++")); pos += 2; }
                else if (peek(1) == '=') { tokens.add(new Token(Token.Type.PLUS_ASSIGN, "+=")); pos += 2; }
                else                     { tokens.add(new Token(Token.Type.PLUS,         "+")); pos++; }
            }

            case '-' -> {
                if (peek(1) == '-')      { tokens.add(new Token(Token.Type.MINUS_MINUS,   "--")); pos += 2; }
                else if (peek(1) == '=') { tokens.add(new Token(Token.Type.MINUS_ASSIGN,  "-=")); pos += 2; }
                else                     { tokens.add(new Token(Token.Type.MINUS,          "-")); pos++; }
            }

            case '*' -> {
                if (peek(1) == '*')      { tokens.add(new Token(Token.Type.STAR_STAR,    "**")); pos += 2; }
                else if (peek(1) == '=') { tokens.add(new Token(Token.Type.STAR_ASSIGN,  "*=")); pos += 2; }
                else                     { tokens.add(new Token(Token.Type.STAR,           "*")); pos++; }
            }

            case '/' -> {
                if (peek(1) == '=') { tokens.add(new Token(Token.Type.SLASH_ASSIGN, "/=")); pos += 2; }
                else                { tokens.add(new Token(Token.Type.SLASH,          "/")); pos++; }
            }
            case '%' -> { tokens.add(new Token(Token.Type.PERCENT, "%")); pos++; }

            case '=' -> {
                if (peek(1) == '=' && peek(2) == '=') { tokens.add(new Token(Token.Type.EQ_EQ_EQ, "===")); pos += 3; }
                else if (peek(1) == '=')               { tokens.add(new Token(Token.Type.EQ_EQ,    "=="));  pos += 2; }
                else if (peek(1) == '>')               { tokens.add(new Token(Token.Type.ARROW,    "=>"));  pos += 2; }
                else                                   { tokens.add(new Token(Token.Type.ASSIGN,    "="));  pos++; }
            }

            case '!' -> {
                if (peek(1) == '=' && peek(2) == '=') { tokens.add(new Token(Token.Type.NOT_EQ_EQ, "!==")); pos += 3; }
                else if (peek(1) == '=')               { tokens.add(new Token(Token.Type.NOT_EQ,    "!="));  pos += 2; }
                else                                   { tokens.add(new Token(Token.Type.NOT,         "!")); pos++; }
            }

            case '<' -> {
                if (peek(1) == '=') { tokens.add(new Token(Token.Type.LT_EQ, "<=")); pos += 2; }
                else                { tokens.add(new Token(Token.Type.LT,     "<")); pos++; }
            }

            case '>' -> {
                if (peek(1) == '=') { tokens.add(new Token(Token.Type.GT_EQ, ">=")); pos += 2; }
                else                { tokens.add(new Token(Token.Type.GT,     ">")); pos++; }
            }

            case '&' -> { tokens.add(new Token(Token.Type.AND, "&&")); pos += 2; }
            case '|' -> { tokens.add(new Token(Token.Type.OR,  "||")); pos += 2; }

            case '.' -> {
                if (peek(1) == '.' && peek(2) == '.') { tokens.add(new Token(Token.Type.SPREAD, "...")); pos += 3; }
                else                                   { tokens.add(new Token(Token.Type.DOT,    "."));   pos++; }
            }

            case '(' -> { tokens.add(new Token(Token.Type.LPAREN,    "(")); pos++; }
            case ')' -> { tokens.add(new Token(Token.Type.RPAREN,    ")")); pos++; }
            case '{' -> { tokens.add(new Token(Token.Type.LBRACE,    "{")); pos++; }
            case '}' -> { tokens.add(new Token(Token.Type.RBRACE,    "}")); pos++; }
            case '[' -> { tokens.add(new Token(Token.Type.LBRACKET,  "[")); pos++; }
            case ']' -> { tokens.add(new Token(Token.Type.RBRACKET,  "]")); pos++; }
            case ';' -> { tokens.add(new Token(Token.Type.SEMICOLON, ";")); pos++; }
            case ',' -> { tokens.add(new Token(Token.Type.COMMA,     ",")); pos++; }
            case ':' -> { tokens.add(new Token(Token.Type.COLON,     ":")); pos++; }
            case '?' -> {
                if (peek(1) == '?' ) { tokens.add(new Token(Token.Type.NULLISH,      "??")); pos += 2; }
                else if (peek(1) == '.') { tokens.add(new Token(Token.Type.QUESTION_DOT, "?.")); pos += 2; }
                else                { tokens.add(new Token(Token.Type.QUESTION,        "?")); pos++; }
            }

            default  -> pos++; // skip unknown characters
        }
    }


    // ── Safe character lookahead ───────────────────────────────
    // peek(0) = current char, peek(1) = next, etc.
    private char peek(int offset) {
        int i = pos + offset;
        return (i < src.length()) ? src.charAt(i) : '\0';
    }
}
