# JavaScript Interpreter in Java
### Thunder Hackathon 2 — Build Your Own JavaScript


---


## Overview

A JavaScript interpreter built from scratch in Java.
It reads a `.js` file and executes it correctly —
without using Node.js or any existing JavaScript engine.

Pipeline:
```
JS Source Code → Lexer → Tokens → Parser → AST → Interpreter → Output
```


---


## Project Structure

```
hackathon/
├── hackathon.iml               IntelliJ project file
└── src/
    ├── Main.java               Entry point — reads JS file, runs the pipeline
    ├── Token.java              Defines all token types (NUMBER, IF, PLUS, etc.)
    ├── Lexer.java              Converts raw JS source into a list of tokens
    ├── ASTNode.java            All AST node types (IfStatement, ForLoop, etc.)
    ├── Parser.java             Converts token list into an Abstract Syntax Tree
    ├── Environment.java        Variable scopes and closure chain
    ├── JSFunction.java         Represents a JS function as a Java object
    ├── Interpreter.java        Walks the AST and executes every node
    └── tests/
        ├── test1.js            Odd / Even Checker
        ├── test2.js            Triangle Pattern using For Loop
        ├── test3.js            Armstrong Number
        ├── test4.js            Array Reverse
        └── test5.js            String Palindrome Check
```


---


## How To Compile

Make sure JDK 11 or higher is installed.

```bash
cd hackathon/src
javac *.java
```

You will see deprecation warnings — these are safe to ignore, not errors.


---


## How To Run

```bash
java -cp . Main tests/test1.js
java -cp . Main tests/test2.js
java -cp . Main tests/test3.js
java -cp . Main tests/test4.js
java -cp . Main tests/test5.js
```

Or run any custom JS file:

```bash
java -cp . Main path/to/yourfile.js
```

You can also pipe JS directly:

```bash
echo "console.log('Hello World')" | java -cp . Main
```


---


## Test Case Results

| TC | Test Case            | Expected Output                          | Status |
|----|----------------------|------------------------------------------|--------|
| 1  | Odd / Even Checker   | 7 is Odd                                 | ✅     |
| 2  | Triangle Pattern     | * / ** / *** / **** / *****              | ✅     |
| 3  | Armstrong Number     | true / false                             | ✅     |
| 4  | Array Reverse        | Original: 1, 2, 3, 4, 5                 | ✅     |
|    |                      | Reversed: 5, 4, 3, 2, 1                  |        |
| 5  | String Palindrome    | racecar is a Palindrome                  | ✅     |


---


## Supported JavaScript Features

```
Variables         let, const, var
Types             number, string, boolean, null, undefined
Operators         arithmetic, comparison, logical, assignment,
                  increment/decrement, ternary, spread
Conditions        if, else if, else, switch / case / default
Loops             for, while, do...while, for...of, for...in
Functions         declarations, expressions, arrow functions,
                  closures, callbacks, recursion, rest params
Error Handling    try, catch, finally, throw
Arrays            push, pop, shift, unshift, slice, splice,
                  concat, includes, indexOf, sort, reverse,
                  map, filter, reduce, forEach, find,
                  findIndex, some, every, flat, fill
Strings           split, join, replace, replaceAll, toUpperCase,
                  toLowerCase, trim, includes, startsWith,
                  endsWith, indexOf, lastIndexOf, slice,
                  substring, charAt, charCodeAt, repeat,
                  padStart, padEnd
Objects           literals, property access, method calls,
                  new keyword, object patterns
Template Literals `Hello ${name}`
Math              floor, ceil, round, abs, sqrt, pow, max,
                  min, random, log, trunc, sign, PI, E
Other             typeof, instanceof, parseInt, parseFloat,
                  isNaN, isFinite, optional chaining (?.)
```


---


## How It Works

**Lexer** (`Lexer.java`)
Scans JS source character by character.
Produces a flat list of Token objects.
Handles strings, template literals, operators, keywords.

**Parser** (`Parser.java`)
Reads tokens using recursive descent parsing.
Builds a nested Abstract Syntax Tree (AST).
Handles operator precedence correctly.

**Environment** (`Environment.java`)
Manages variable scopes as a chain of HashMaps.
Each scope links to its parent for closure support.

**JSFunction** (`JSFunction.java`)
Stores a function's parameters, body AST, and
the environment where it was defined (closure).

**Interpreter** (`Interpreter.java`)
Walks the AST node by node.
Executes every statement and expression.
Maps all JS built-in functions to Java equivalents.


---


## Language

```
Java JDK 23
No external libraries
No JavaScript engine used
Built entirely from scratch
```
