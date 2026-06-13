// ─────────────────────────────────────────────────────────────
// Parser.java
// Takes the flat list of Tokens from the Lexer and builds
// a nested Abstract Syntax Tree using recursive descent
// Each parseXxx() method handles one grammar construct
// ─────────────────────────────────────────────────────────────

import java.util.*;

public class Parser {


    // ── Token stream and current read position ────────────────
    private final List<Token> tokens;
    private int pos;


    // ── Constructor ───────────────────────────────────────────
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos    = 0;
    }


    // ══════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ══════════════════════════════════════════════════════════


    // ── Parse the entire program into a Program node ──────────
    public ASTNode.Program parse() {
        List<ASTNode> body = new ArrayList<>();
        while (!check(Token.Type.EOF)) {
            body.add(parseStatement());
        }
        return new ASTNode.Program(body);
    }


    // ══════════════════════════════════════════════════════════
    //  STATEMENT PARSING
    // ══════════════════════════════════════════════════════════


    // ── Dispatch to the correct statement parser ───────────────
    private ASTNode parseStatement() {
        Token t = current();

        return switch (t.type) {
            case LET, CONST, VAR -> parseVarDeclaration();
            case IF               -> parseIf();
            case FOR              -> parseFor();
            case WHILE            -> parseWhile();
            case DO               -> parseDoWhile();
            case FUNCTION         -> parseFunctionDeclaration();
            case RETURN           -> parseReturn();
            case SWITCH           -> parseSwitch();
            case BREAK            -> { advance(); skipSemicolon(); yield new ASTNode.BreakStatement(); }
            case CONTINUE         -> { advance(); skipSemicolon(); yield new ASTNode.ContinueStatement(); }
            case TRY              -> parseTry();
            case THROW            -> parseThrow();
            case LBRACE           -> parseBlock();
            case SEMICOLON        -> { advance(); yield parseStatement(); }
            default               -> parseExpressionStatement();
        };
    }


    // ── let / const / var name = expr;  or  destructuring ────
    // also handles  let a = 1, b = 2, c;
    private ASTNode parseVarDeclaration() {
        String kind = advance().value;       // consume let/const/var

        List<ASTNode.VarDeclaration> decls = new ArrayList<>();
        decls.add(parseSingleVarDeclarator(kind));

        while (check(Token.Type.COMMA)) {
            advance();                       // consume ','
            decls.add(parseSingleVarDeclarator(kind));
        }

        skipSemicolon();
        return decls.size() == 1 ? decls.get(0) : new ASTNode.VarDeclarationList(decls);
    }


    // ── A single  name = expr  or  pattern = expr  binding ────
    // (no semicolon / comma handling — caller manages those)
    private ASTNode.VarDeclaration parseSingleVarDeclarator(String kind) {
        if (check(Token.Type.LBRACKET) || check(Token.Type.LBRACE)) {
            ASTNode pattern = parsePattern();
            expect(Token.Type.ASSIGN);
            ASTNode init = parseExpression();
            return new ASTNode.VarDeclaration(kind, pattern, init, true);
        }

        String name = expect(Token.Type.IDENTIFIER).value;

        ASTNode init = null;
        if (check(Token.Type.ASSIGN)) {
            advance();                       // consume '='
            init = parseExpression();
        }

        return new ASTNode.VarDeclaration(kind, name, init);
    }


    // ── if (cond) stmt [else stmt] ────────────────────────────
    private ASTNode parseIf() {
        advance();                           // consume 'if'
        expect(Token.Type.LPAREN);
        ASTNode condition = parseExpression();
        expect(Token.Type.RPAREN);

        ASTNode thenBranch = parseStatement();

        ASTNode elseBranch = null;
        if (check(Token.Type.ELSE)) {
            advance();                       // consume 'else'
            elseBranch = parseStatement();
        }

        return new ASTNode.IfStatement(condition, thenBranch, elseBranch);
    }


    // ── try { } catch (e) { } finally { } ────────────────────
    private ASTNode parseTry() {
        advance();                              // consume 'try'
        ASTNode.Block tryBlock = parseBlock();

        String catchParam = null;
        ASTNode.Block catchBlock = null;
        if (check(Token.Type.CATCH)) {
            advance();                          // consume 'catch'
            if (check(Token.Type.LPAREN)) {
                advance();
                catchParam = expect(Token.Type.IDENTIFIER).value;
                expect(Token.Type.RPAREN);
            }
            catchBlock = parseBlock();
        }

        ASTNode.Block finallyBlock = null;
        if (check(Token.Type.FINALLY)) {
            advance();                          // consume 'finally'
            finallyBlock = parseBlock();
        }

        return new ASTNode.TryStatement(tryBlock, catchParam, catchBlock, finallyBlock);
    }


    // ── throw expr; ────────────────────────────────────────────
    private ASTNode parseThrow() {
        advance();                              // consume 'throw'
        ASTNode value = parseExpression();
        skipSemicolon();
        return new ASTNode.ThrowStatement(value);
    }


    // ── for (init; condition; update) body ───────────────────
    private ASTNode parseFor() {
        advance();                           // consume 'for'
        expect(Token.Type.LPAREN);

        // ── Check for `for (let x of expr)` / `for (let x in expr)` ──
        if (check(Token.Type.LET) || check(Token.Type.CONST) || check(Token.Type.VAR)) {
            String kind = current().value;
            Token nameTok  = tokens.get(pos + 1);
            Token afterTok = tokens.get(pos + 2);
            if (nameTok.type == Token.Type.IDENTIFIER &&
                afterTok.type == Token.Type.IDENTIFIER &&
                (afterTok.value.equals("of") || afterTok.value.equals("in"))) {

                boolean isOf = afterTok.value.equals("of");
                advance();                   // consume let/const/var
                String name = advance().value;  // consume identifier
                advance();                   // consume "of"/"in"
                ASTNode iterable = parseExpression();
                expect(Token.Type.RPAREN);
                ASTNode body = parseStatement();
                return isOf
                    ? new ASTNode.ForOfStatement(kind, name, iterable, body)
                    : new ASTNode.ForInStatement(kind, name, iterable, body);
            }
        }

        ASTNode init = null;
        if (!check(Token.Type.SEMICOLON)) {
            if (check(Token.Type.LET) || check(Token.Type.CONST) || check(Token.Type.VAR)) {
                init = parseVarDeclaration();
            } else {
                init = parseExpressionStatement();
            }
        } else {
            advance();                       // consume ';'
        }

        ASTNode condition = null;
        if (!check(Token.Type.SEMICOLON)) condition = parseExpression();
        expect(Token.Type.SEMICOLON);

        ASTNode update = null;
        if (!check(Token.Type.RPAREN)) update = parseExpression();
        expect(Token.Type.RPAREN);

        ASTNode body = parseStatement();
        return new ASTNode.ForStatement(init, condition, update, body);
    }


    // ── while (condition) body ────────────────────────────────
    private ASTNode parseWhile() {
        advance();                           // consume 'while'
        expect(Token.Type.LPAREN);
        ASTNode condition = parseExpression();
        expect(Token.Type.RPAREN);
        ASTNode body = parseStatement();
        return new ASTNode.WhileStatement(condition, body);
    }


    // ── do { body } while (condition); ────────────────────────
    private ASTNode parseDoWhile() {
        advance();                           // consume 'do'
        ASTNode body = parseStatement();
        expect(Token.Type.WHILE);
        expect(Token.Type.LPAREN);
        ASTNode condition = parseExpression();
        expect(Token.Type.RPAREN);
        skipSemicolon();
        return new ASTNode.DoWhileStatement(body, condition);
    }


    // ── function name(params) { body } ───────────────────────
    private ASTNode parseFunctionDeclaration() {
        advance();                           // consume 'function'
        String name = expect(Token.Type.IDENTIFIER).value;
        List<ASTNode.Param> params = parseParamList();
        ASTNode.Block body = parseBlock();
        return new ASTNode.FunctionDeclaration(name, params, body);
    }


    // ── return [expr]; ────────────────────────────────────────
    private ASTNode parseReturn() {
        advance();                           // consume 'return'
        ASTNode value = null;
        if (!check(Token.Type.SEMICOLON) && !check(Token.Type.RBRACE) && !check(Token.Type.EOF)) {
            value = parseExpression();
        }
        skipSemicolon();
        return new ASTNode.ReturnStatement(value);
    }


    // ── switch (expr) { case x: ... default: ... } ───────────
    private ASTNode parseSwitch() {
        advance();                           // consume 'switch'
        expect(Token.Type.LPAREN);
        ASTNode disc = parseExpression();
        expect(Token.Type.RPAREN);
        expect(Token.Type.LBRACE);

        List<ASTNode.SwitchCase> cases = new ArrayList<>();
        while (!check(Token.Type.RBRACE) && !check(Token.Type.EOF)) {
            ASTNode test = null;
            if (check(Token.Type.CASE)) {
                advance();
                test = parseExpression();
                expect(Token.Type.COLON);
            } else if (check(Token.Type.DEFAULT)) {
                advance();
                expect(Token.Type.COLON);
            }

            List<ASTNode> consequent = new ArrayList<>();
            while (!check(Token.Type.CASE) && !check(Token.Type.DEFAULT) &&
                   !check(Token.Type.RBRACE) && !check(Token.Type.EOF)) {
                consequent.add(parseStatement());
            }
            cases.add(new ASTNode.SwitchCase(test, consequent));
        }

        expect(Token.Type.RBRACE);
        return new ASTNode.SwitchStatement(disc, cases);
    }


    // ── { statement* } ────────────────────────────────────────
    private ASTNode.Block parseBlock() {
        expect(Token.Type.LBRACE);
        List<ASTNode> stmts = new ArrayList<>();
        while (!check(Token.Type.RBRACE) && !check(Token.Type.EOF)) {
            stmts.add(parseStatement());
        }
        expect(Token.Type.RBRACE);
        return new ASTNode.Block(stmts);
    }


    // ── expr; ─────────────────────────────────────────────────
    private ASTNode parseExpressionStatement() {
        ASTNode expr = parseExpression();
        skipSemicolon();
        return new ASTNode.ExpressionStatement(expr);
    }


    // ══════════════════════════════════════════════════════════
    //  EXPRESSION PARSING  (operator precedence chain)
    //  Each level calls the one above it (higher precedence)
    // ══════════════════════════════════════════════════════════


    // ── Top of expression: assignment ─────────────────────────
    private ASTNode parseExpression() {
        return parseAssignment();
    }


    // ── x = y  /  x += y  /  x -= y  etc. ───────────────────
    private ASTNode parseAssignment() {
        ASTNode left = parseTernary();

        if (check(Token.Type.ASSIGN) || check(Token.Type.PLUS_ASSIGN) ||
            check(Token.Type.MINUS_ASSIGN) || check(Token.Type.STAR_ASSIGN) ||
            check(Token.Type.SLASH_ASSIGN)) {
            String op = advance().value;
            ASTNode value = parseAssignment();
            return new ASTNode.Assignment(left, op, value);
        }

        return left;
    }


    // ── condition ? thenExpr : elseExpr ──────────────────────
    private ASTNode parseTernary() {
        ASTNode cond = parseOr();
        if (check(Token.Type.QUESTION)) {
            advance();
            ASTNode then = parseExpression();
            expect(Token.Type.COLON);
            ASTNode else_ = parseExpression();
            return new ASTNode.TernaryExpression(cond, then, else_);
        }
        return cond;
    }


    // ── a ?? b  (nullish coalescing) ─────────────────────────
    private ASTNode parseNullish() {
        ASTNode left = parseAnd();
        while (check(Token.Type.NULLISH)) {
            advance();
            left = new ASTNode.BinaryOp(left, "??", parseAnd());
        }
        return left;
    }


    // ── a || b ────────────────────────────────────────────────
    private ASTNode parseOr() {
        ASTNode left = parseNullish();
        while (check(Token.Type.OR)) {
            String op = advance().value;
            left = new ASTNode.BinaryOp(left, op, parseNullish());
        }
        return left;
    }


    // ── a && b ────────────────────────────────────────────────
    private ASTNode parseAnd() {
        ASTNode left = parseEquality();
        while (check(Token.Type.AND)) {
            String op = advance().value;
            left = new ASTNode.BinaryOp(left, op, parseEquality());
        }
        return left;
    }


    // ── a === b  /  a !== b  /  a == b  /  a != b ────────────
    private ASTNode parseEquality() {
        ASTNode left = parseComparison();
        while (check(Token.Type.EQ_EQ_EQ) || check(Token.Type.NOT_EQ_EQ) ||
               check(Token.Type.EQ_EQ)    || check(Token.Type.NOT_EQ)) {
            String op = advance().value;
            left = new ASTNode.BinaryOp(left, op, parseComparison());
        }
        return left;
    }


    // ── a < b  /  a > b  /  a <= b  /  a >= b  /  a instanceof b ─
    private ASTNode parseComparison() {
        ASTNode left = parseAddSub();
        while (check(Token.Type.LT) || check(Token.Type.GT) ||
               check(Token.Type.LT_EQ) || check(Token.Type.GT_EQ) ||
               check(Token.Type.INSTANCEOF)) {
            String op = advance().value;
            left = new ASTNode.BinaryOp(left, op, parseAddSub());
        }
        return left;
    }


    // ── a + b  /  a - b ──────────────────────────────────────
    private ASTNode parseAddSub() {
        ASTNode left = parseMulDiv();
        while (check(Token.Type.PLUS) || check(Token.Type.MINUS)) {
            String op = advance().value;
            left = new ASTNode.BinaryOp(left, op, parseMulDiv());
        }
        return left;
    }


    // ── a * b  /  a / b  /  a % b  /  a ** b ────────────────
    private ASTNode parseMulDiv() {
        ASTNode left = parseUnary();
        while (check(Token.Type.STAR) || check(Token.Type.SLASH) ||
               check(Token.Type.PERCENT) || check(Token.Type.STAR_STAR)) {
            String op = advance().value;
            left = new ASTNode.BinaryOp(left, op, parseUnary());
        }
        return left;
    }


    // ── !x  /  -x  /  typeof x  /  ++x  /  --x ─────────────
    private ASTNode parseUnary() {
        if (check(Token.Type.NOT))   { String op = advance().value; return new ASTNode.UnaryOp(op, parseUnary()); }
        if (check(Token.Type.MINUS)) { String op = advance().value; return new ASTNode.UnaryOp(op, parseUnary()); }
        if (check(Token.Type.PLUS))  { String op = advance().value; return new ASTNode.UnaryOp(op, parseUnary()); }
        if (check(Token.Type.TYPEOF)){ advance(); return new ASTNode.UnaryOp("typeof", parseUnary()); }

        // Prefix increment/decrement
        if (check(Token.Type.PLUS_PLUS))  { advance(); return new ASTNode.UpdateExpression("++", parseUnary(), true); }
        if (check(Token.Type.MINUS_MINUS)){ advance(); return new ASTNode.UpdateExpression("--", parseUnary(), true); }

        return parsePostfix();
    }


    // ── x++  /  x-- (postfix) ────────────────────────────────
    private ASTNode parsePostfix() {
        ASTNode expr = parseCallOrMember();
        if (check(Token.Type.PLUS_PLUS))  { advance(); return new ASTNode.UpdateExpression("++", expr, false); }
        if (check(Token.Type.MINUS_MINUS)){ advance(); return new ASTNode.UpdateExpression("--", expr, false); }
        return expr;
    }


    // ── obj.prop  /  obj[expr]  /  fn(args) ──────────────────
    private ASTNode parseCallOrMember() {
        ASTNode expr = parsePrimary();

        while (true) {
            if (check(Token.Type.DOT)) {
                advance();
                String prop = expect(Token.Type.IDENTIFIER).value;
                expr = new ASTNode.MemberExpression(expr, prop, null);

            } else if (check(Token.Type.LBRACKET)) {
                advance();
                ASTNode computed = parseExpression();
                expect(Token.Type.RBRACKET);
                expr = new ASTNode.MemberExpression(expr, null, computed);

            } else if (check(Token.Type.QUESTION_DOT)) {
                advance();
                if (check(Token.Type.LBRACKET)) {
                    advance();
                    ASTNode computed = parseExpression();
                    expect(Token.Type.RBRACKET);
                    expr = new ASTNode.MemberExpression(expr, null, computed, true);
                } else if (check(Token.Type.LPAREN)) {
                    List<ASTNode> args = parseArgList();
                    expr = new ASTNode.CallExpression(expr, args);
                } else {
                    String prop = expect(Token.Type.IDENTIFIER).value;
                    expr = new ASTNode.MemberExpression(expr, prop, null, true);
                }

            } else if (check(Token.Type.LPAREN)) {
                List<ASTNode> args = parseArgList();
                expr = new ASTNode.CallExpression(expr, args);

            } else {
                break;
            }
        }

        return expr;
    }


    // ── Parse a member-access chain WITHOUT consuming call args ─
    // Used for `new Foo.Bar(args)` — only `.prop` / `[expr]` are
    // part of the constructor name; the trailing `(args)` belongs
    // to the NewExpression itself, not a nested CallExpression.
    private ASTNode parseMemberOnly() {
        ASTNode expr = parsePrimary();

        while (true) {
            if (check(Token.Type.DOT)) {
                advance();
                String prop = expect(Token.Type.IDENTIFIER).value;
                expr = new ASTNode.MemberExpression(expr, prop, null);

            } else if (check(Token.Type.LBRACKET)) {
                advance();
                ASTNode computed = parseExpression();
                expect(Token.Type.RBRACKET);
                expr = new ASTNode.MemberExpression(expr, null, computed);

            } else {
                break;
            }
        }

        return expr;
    }


    // ── Leaf nodes: literals, identifiers, grouped exprs ─────
    private ASTNode parsePrimary() {

        Token t = current();

        // Number literal
        if (t.type == Token.Type.NUMBER) {
            advance();
            return new ASTNode.Literal(Double.parseDouble(t.value));
        }

        // String literal
        if (t.type == Token.Type.STRING) {
            advance();
            return new ASTNode.Literal(t.value);
        }

        // Template literal  `text ${expr} text`
        if (t.type == Token.Type.TEMPLATE) {
            advance();
            return parseTemplateLiteral(t.value);
        }

        // Boolean literals
        if (t.type == Token.Type.TRUE)  { advance(); return new ASTNode.Literal(true); }
        if (t.type == Token.Type.FALSE) { advance(); return new ASTNode.Literal(false); }

        // null / undefined
        if (t.type == Token.Type.NULL)      { advance(); return new ASTNode.Literal(null); }
        if (t.type == Token.Type.UNDEFINED) { advance(); return new ASTNode.Identifier("undefined"); }

        // Array literal  [a, b, c]
        if (t.type == Token.Type.LBRACKET) {
            return parseArrayLiteral();
        }

        // Object literal  { key: value }
        if (t.type == Token.Type.LBRACE) {
            return parseObjectLiteral();
        }

        // Grouped expression or arrow function  (x) => ...
        if (t.type == Token.Type.LPAREN) {
            return parseGroupOrArrow();
        }

        // Anonymous function expression
        if (t.type == Token.Type.FUNCTION) {
            advance();
            String name = null;
            if (check(Token.Type.IDENTIFIER)) name = advance().value;
            List<ASTNode.Param> params = parseParamList();
            ASTNode.Block body = parseBlock();
            return new ASTNode.FunctionExpression(params, body, false);
        }

        // new Foo(args)  or  new Foo.Bar(args)  (member access only, no calls)
        if (t.type == Token.Type.NEW) {
            advance();
            ASTNode callee = parseMemberOnly();
            List<ASTNode> args = check(Token.Type.LPAREN) ? parseArgList() : new ArrayList<>();
            return new ASTNode.NewExpression(callee, args);
        }

        // Spread  ...expr
        if (t.type == Token.Type.SPREAD) {
            advance();
            return new ASTNode.SpreadElement(parseExpression());
        }

        // Identifier or arrow function  x => ...
        if (t.type == Token.Type.IDENTIFIER) {
            advance();
            // Single-param arrow: x => expr
            if (check(Token.Type.ARROW)) {
                advance();
                ASTNode body = check(Token.Type.LBRACE) ? parseBlock() : parseExpression();
                return new ASTNode.FunctionExpression(
                    List.of(new ASTNode.Param(new ASTNode.Identifier(t.value), null, false)),
                    body, true);
            }
            return new ASTNode.Identifier(t.value);
        }

        throw new RuntimeException("Unexpected token: " + t);
    }


    // ══════════════════════════════════════════════════════════
    //  HELPERS FOR PARSING LISTS & GROUPS
    // ══════════════════════════════════════════════════════════


    // ── Split raw template text into literal/expr parts ───────
    // raw still contains escapes (\n etc.) and ${...} placeholders
    private ASTNode parseTemplateLiteral(String raw) {
        List<Object> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int i = 0;

        while (i < raw.length()) {
            char c = raw.charAt(i);

            // Escape sequence
            if (c == '\\' && i + 1 < raw.length()) {
                char esc = raw.charAt(i + 1);
                switch (esc) {
                    case 'n'  -> cur.append('\n');
                    case 't'  -> cur.append('\t');
                    case '\\' -> cur.append('\\');
                    case '`'  -> cur.append('`');
                    case '$'  -> cur.append('$');
                    case '\'' -> cur.append('\'');
                    case '"'  -> cur.append('"');
                    default   -> cur.append(esc);
                }
                i += 2;
                continue;
            }

            // ${ expr }
            if (c == '$' && i + 1 < raw.length() && raw.charAt(i + 1) == '{') {
                parts.add(cur.toString());
                cur = new StringBuilder();

                int depth = 1;
                int j = i + 2;
                while (j < raw.length() && depth > 0) {
                    char cc = raw.charAt(j);
                    if (cc == '{')      depth++;
                    else if (cc == '}') { depth--; if (depth == 0) break; }
                    j++;
                }

                String exprSrc = raw.substring(i + 2, j);
                Lexer subLexer = new Lexer(exprSrc);
                Parser subParser = new Parser(subLexer.tokenize());
                parts.add(subParser.parseExpression());

                i = j + 1;
                continue;
            }

            cur.append(c);
            i++;
        }

        parts.add(cur.toString());
        return new ASTNode.TemplateLiteral(parts);
    }


    // ── [elem, ...elem] ───────────────────────────────────────
    private ASTNode parseArrayLiteral() {
        expect(Token.Type.LBRACKET);
        List<ASTNode> elements = new ArrayList<>();
        while (!check(Token.Type.RBRACKET) && !check(Token.Type.EOF)) {
            if (check(Token.Type.SPREAD)) {
                advance();
                elements.add(new ASTNode.SpreadElement(parseExpression()));
            } else {
                elements.add(parseExpression());
            }
            if (!check(Token.Type.RBRACKET)) expect(Token.Type.COMMA);
        }
        expect(Token.Type.RBRACKET);
        return new ASTNode.ArrayLiteral(elements);
    }


    // ── { key: value, ... } ───────────────────────────────────
    private ASTNode parseObjectLiteral() {
        expect(Token.Type.LBRACE);
        Map<String, ASTNode> props = new LinkedHashMap<>();
        while (!check(Token.Type.RBRACE) && !check(Token.Type.EOF)) {
            String key;
            boolean isIdentifierKey = check(Token.Type.IDENTIFIER);
            if (check(Token.Type.STRING))     key = advance().value;
            else if (check(Token.Type.NUMBER)) key = advance().value;
            else key = expect(Token.Type.IDENTIFIER).value;

            if (isIdentifierKey && check(Token.Type.LPAREN)) {
                // Method shorthand:  { greet() { ... } }
                List<ASTNode.Param> params = parseParamList();
                ASTNode.Block body = parseBlock();
                props.put(key, new ASTNode.FunctionExpression(params, body, false));
            } else if (isIdentifierKey && (check(Token.Type.COMMA) || check(Token.Type.RBRACE))) {
                // Property shorthand:  { myX, myY }  →  { myX: myX, myY: myY }
                props.put(key, new ASTNode.Identifier(key));
            } else {
                expect(Token.Type.COLON);
                ASTNode value = parseExpression();
                props.put(key, value);
            }

            if (!check(Token.Type.RBRACE)) expect(Token.Type.COMMA);
        }
        expect(Token.Type.RBRACE);
        return new ASTNode.ObjectLiteral(props);
    }


    // ── (expr)  or  (a, b) => expr ────────────────────────────
    private ASTNode parseGroupOrArrow() {
        int saved = pos;
        try {
            // Try to parse as arrow function parameter list
            List<ASTNode.Param> params = parseParamList();
            if (check(Token.Type.ARROW)) {
                advance();
                ASTNode body = check(Token.Type.LBRACE) ? parseBlock() : parseExpression();
                return new ASTNode.FunctionExpression(params, body, true);
            }
            // Not an arrow function — restore and fall through
            pos = saved;
        } catch (Exception e) {
            pos = saved;
        }

        // Just a grouped expression  (expr)
        expect(Token.Type.LPAREN);
        ASTNode expr = parseExpression();
        expect(Token.Type.RPAREN);
        return expr;
    }


    // ── Leaf pattern: identifier, [array pattern], {object pattern} ─
    private ASTNode parsePattern() {
        if (check(Token.Type.LBRACKET)) return parseArrayPattern();
        if (check(Token.Type.LBRACE))   return parseObjectPattern();
        return new ASTNode.Identifier(expect(Token.Type.IDENTIFIER).value);
    }


    // ── [a, b = 1, ...rest] ────────────────────────────────────
    private ASTNode.ArrayPattern parseArrayPattern() {
        expect(Token.Type.LBRACKET);
        List<ASTNode.Param> elements = new ArrayList<>();
        while (!check(Token.Type.RBRACKET) && !check(Token.Type.EOF)) {
            if (check(Token.Type.COMMA)) {
                // Elision: a "hole" in the pattern, e.g. [, second]
                elements.add(null);
            } else if (check(Token.Type.SPREAD)) {
                advance();
                elements.add(new ASTNode.Param(parsePattern(), null, true));
            } else {
                ASTNode pat = parsePattern();
                ASTNode def = null;
                if (check(Token.Type.ASSIGN)) { advance(); def = parseExpression(); }
                elements.add(new ASTNode.Param(pat, def, false));
            }
            if (!check(Token.Type.RBRACKET)) expect(Token.Type.COMMA);
        }
        expect(Token.Type.RBRACKET);
        return new ASTNode.ArrayPattern(elements);
    }


    // ── { a, b: bAlias = 1, ...rest } ──────────────────────────
    private ASTNode.ObjectPattern parseObjectPattern() {
        expect(Token.Type.LBRACE);
        Map<String, ASTNode.Param> props = new LinkedHashMap<>();
        String restName = null;
        while (!check(Token.Type.RBRACE) && !check(Token.Type.EOF)) {
            if (check(Token.Type.SPREAD)) {
                advance();
                restName = expect(Token.Type.IDENTIFIER).value;
            } else {
                String key = expect(Token.Type.IDENTIFIER).value;
                ASTNode pat;
                if (check(Token.Type.COLON)) {
                    advance();
                    pat = parsePattern();
                } else {
                    pat = new ASTNode.Identifier(key);
                }
                ASTNode def = null;
                if (check(Token.Type.ASSIGN)) { advance(); def = parseExpression(); }
                props.put(key, new ASTNode.Param(pat, def, false));
            }
            if (!check(Token.Type.RBRACE)) expect(Token.Type.COMMA);
        }
        expect(Token.Type.RBRACE);
        return new ASTNode.ObjectPattern(props, restName);
    }


    // ── Parse (param1, param2 = default, ...rest) ─────────────
    private List<ASTNode.Param> parseParamList() {
        expect(Token.Type.LPAREN);
        List<ASTNode.Param> params = new ArrayList<>();
        while (!check(Token.Type.RPAREN) && !check(Token.Type.EOF)) {
            if (check(Token.Type.SPREAD)) {
                advance();
                params.add(new ASTNode.Param(parsePattern(), null, true));
            } else {
                ASTNode pat = parsePattern();
                ASTNode def = null;
                if (check(Token.Type.ASSIGN)) { advance(); def = parseExpression(); }
                params.add(new ASTNode.Param(pat, def, false));
            }
            if (!check(Token.Type.RPAREN)) expect(Token.Type.COMMA);
        }
        expect(Token.Type.RPAREN);
        return params;
    }


    // ── Parse (arg1, arg2, ...) ───────────────────────────────
    private List<ASTNode> parseArgList() {
        expect(Token.Type.LPAREN);
        List<ASTNode> args = new ArrayList<>();
        while (!check(Token.Type.RPAREN) && !check(Token.Type.EOF)) {
            if (check(Token.Type.SPREAD)) {
                advance();
                args.add(new ASTNode.SpreadElement(parseExpression()));
            } else {
                args.add(parseExpression());
            }
            if (!check(Token.Type.RPAREN)) expect(Token.Type.COMMA);
        }
        expect(Token.Type.RPAREN);
        return args;
    }


    // ══════════════════════════════════════════════════════════
    //  TOKEN NAVIGATION UTILITIES
    // ══════════════════════════════════════════════════════════


    // ── Return current token without consuming it ─────────────
    private Token current() { return tokens.get(pos); }


    // ── Consume and return current token, move forward ────────
    private Token advance() { return tokens.get(pos++); }


    // ── Return true if current token matches the given type ───
    private boolean check(Token.Type type) { return current().type == type; }


    // ── Consume current token, throw if it doesn't match ─────
    private Token expect(Token.Type type) {
        if (!check(type)) {
            throw new RuntimeException("Expected " + type + " but got " + current());
        }
        return advance();
    }


    // ── Consume a semicolon if present, otherwise ignore ──────
    private void skipSemicolon() {
        if (check(Token.Type.SEMICOLON)) advance();
    }
}
