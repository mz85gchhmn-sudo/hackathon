# [Project Name] - JavaScript Interpreter in Java

A lightweight JavaScript interpreter built entirely in Java. This project parses and executes a subset of JavaScript, designed as an educational tool to demonstrate lexical analysis, parsing (AST generation), and evaluation.

## Features

Currently, the interpreter supports the following JavaScript features:
* **Variables**: `var`, `let`, `const` declarations.
* **Data Types**: Numbers, Strings, Booleans, and basic Arrays.
* **Control Flow**: `if`, `else if`, `else`, `while`, and `for` loops.
* **Functions**: Standard function declarations and return statements.
* **Built-in Functions**: `console.log()` for standard output.

*(Note: This is a lightweight interpreter and does not currently support advanced ES6+ features like Promises, async/await, or classes).*

## Prerequisites

To build and run this project, you will need:
* **Java Development Kit (JDK) 11** or higher.
* [Optional] **Maven / Gradle** (if you used a build tool).

## Project Structure

```text
├── src/
│   ├── main/java/         # Java source code (Lexer, Parser, Evaluator)
│   └── Main.java          # Entry point for the interpreter
├── tests/                 # Sample JS files for testing
│   ├── basic_math.js
│   ├── loops.js
│   └── functions.js
└── README.md

Getting Started
1. Clone the repository
code
Bash
git clone https://github.com/[Your-Username]/[Repository-Name].git
cd [Repository-Name]
2. Build the project
If you are using standard javac:
code
Bash
javac -d bin src/main/java/**/*.java
(Or, if you are using Maven: mvn clean package)


3. Run the Interpreter
You can run the interpreter and pass a JavaScript file as an argument:
code
Bash
java -cp bin Main tests/basic_math.js
Running the Test Files
This repository comes with several pre-written .js files in the tests/ directory to demonstrate the interpreter's capabilities.
To test a specific feature, simply pass the file to the interpreter. For example:
Testing Loops (tests/loops.js)
code
Bash
java -cp bin Main tests/loops.js
Expected Output:
code
Text
Iteration: 1
Iteration: 2
Iteration: 3
Loop finished!
Testing Functions (tests/functions.js)
code
Bash
java -cp bin Main tests/functions.js
Expected Output:
code
Text
The sum of 5 and 10 is 15
Writing Your Own Tests
You can easily write your own test scripts. Create a new .js file in the tests/ directory:
code
JavaScript
// tests/hello.js
let greeting = "Hello, World!";
console.log(greeting);
And run it: java -cp bin Main tests/hello.js
Architecture overview
Lexer: Tokenizes the incoming JavaScript source code.
Parser: Takes tokens and builds an Abstract Syntax Tree (AST).
Environment: Manages variable scopes and closures.
Evaluator: Traverses the AST and executes the corresponding Java logic.
Limitations / TODO

Add support for Arrow Functions (=>)

Implement Objects and Prototypes

Add support for try/catch blocks

Better syntax error highlighting