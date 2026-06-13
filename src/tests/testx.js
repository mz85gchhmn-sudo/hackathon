// ══════════════════════════════════════
// 1. Variables & Scoping
// ══════════════════════════════════════
let a = 1;
const b = 2;
var c = 3;
console.log(a + b + c); // 6

{
  let block = 99;
  console.log(block); // 99
}

// ══════════════════════════════════════
// 2. Template Literals
// ══════════════════════════════════════
let name = "World";
console.log(`Hello, ${name}!`);           // Hello, World!
console.log(`Sum: ${1 + 2 + 3}`);         // Sum: 6
console.log(`\n tab:\t done`);            // newline + tab
let x = 5;
console.log(`${x > 3 ? "big" : "small"}`); // big

// ══════════════════════════════════════
// 3. Arithmetic & Operators
// ══════════════════════════════════════
console.log(10 % 3);   // 1
console.log(2 ** 8);   // 256
console.log(10 / 3);   // 3.3333333333333335
console.log(0.1 + 0.2 > 0.3); // true (floating point)
let n = 5;
n += 3; console.log(n); // 8
n -= 2; console.log(n); // 6
n *= 4; console.log(n); // 24
n /= 6; console.log(n); // 4

// ══════════════════════════════════════
// 4. Comparison & Equality
// ══════════════════════════════════════
console.log(1 === 1);     // true
console.log(1 === "1");   // false
console.log(1 == "1");    // true (coercion)
console.log(null == undefined); // true
console.log(null === undefined); // false
console.log(NaN === NaN); // false

// String comparison (KNOWN BUG: will give wrong result)
// console.log("b" > "a"); // should be true, returns false

// ══════════════════════════════════════
// 5. Logical Operators & Short-circuit
// ══════════════════════════════════════
console.log(true && "yes");   // yes
console.log(false && "yes");  // false
console.log(false || "fallback"); // fallback
console.log(null || 0 || "found"); // found
console.log(!false);  // true
console.log(!!0);     // false
console.log(!!"hi");  // true

// ?? NOT SUPPORTED - will fail
// console.log(null ?? "default"); // should be: default

// ══════════════════════════════════════
// 6. Control Flow: if/else
// ══════════════════════════════════════
let score = 75;
if (score >= 90) {
  console.log("A");
} else if (score >= 80) {
  console.log("B");
} else if (score >= 70) {
  console.log("C");
} else {
  console.log("F");
}
// C

// ══════════════════════════════════════
// 7. Ternary
// ══════════════════════════════════════
let age = 20;
console.log(age >= 18 ? "adult" : "minor"); // adult
let val = null;
let result = val !== null ? val : "default";
console.log(result); // default

// ══════════════════════════════════════
// 8. switch
// ══════════════════════════════════════
let day = 2;
switch (day) {
  case 1: console.log("Mon"); break;
  case 2: console.log("Tue"); break;
  case 3: console.log("Wed"); break;
  default: console.log("Other");
}
// Tue

// Fall-through test
switch (1) {
  case 1:
  case 2:
    console.log("one or two"); break;
  default:
    console.log("other");
}
// one or two

// ══════════════════════════════════════
// 9. while / do...while
// ══════════════════════════════════════
let i = 0;
while (i < 3) {
  console.log(i); // 0 1 2
  i++;
}

let j = 5;
do {
  console.log(j); // 5  (runs once even though condition is false)
  j++;
} while (j < 5);

// ══════════════════════════════════════
// 10. for loop
// ══════════════════════════════════════
for (let k = 0; k < 5; k++) {
  if (k === 3) break;
  console.log(k); // 0 1 2
}

// continue - NOT SUPPORTED, skip this
// for (let k = 0; k < 5; k++) {
//   if (k === 2) continue;
//   console.log(k); // 0 1 3 4
// }

// ══════════════════════════════════════
// 11. for...of
// ══════════════════════════════════════
for (let ch of "hello") {
  process.stdout ? process.stdout.write(ch) : console.log(ch);
}
// h e l l o (each on own line)

let fruits = ["apple", "banana", "cherry"];
for (let fruit of fruits) {
  console.log(fruit);
}

// ══════════════════════════════════════
// 12. for...in
// ══════════════════════════════════════
let obj = { x: 1, y: 2, z: 3 };
for (let key in obj) {
  console.log(key + ": " + obj[key]);
}
// x: 1  y: 2  z: 3

// ══════════════════════════════════════
// 13. Functions
// ══════════════════════════════════════
function add(a, b) { return a + b; }
console.log(add(3, 4)); // 7

// Default params
function greet(name = "stranger") {
  return `Hello, ${name}`;
}
console.log(greet());        // Hello, stranger
console.log(greet("Ali"));   // Hello, Ali

// Rest params
function sum(...nums) {
  return nums.reduce((acc, n) => acc + n, 0);
}
console.log(sum(1, 2, 3, 4)); // 10

// Function expression
const multiply = function(a, b) { return a * b; };
console.log(multiply(3, 5)); // 15

// ══════════════════════════════════════
// 14. Arrow Functions
// ══════════════════════════════════════
const square = n => n * n;
console.log(square(9)); // 81

const addArr = (a, b) => a + b;
console.log(addArr(10, 5)); // 15

const makeObj = x => ({ value: x });
console.log(makeObj(42).value); // 42

// ══════════════════════════════════════
// 15. Closures
// ══════════════════════════════════════
function makeCounter() {
  let count = 0;
  return () => ++count;
}
const counter = makeCounter();
console.log(counter()); // 1
console.log(counter()); // 2
console.log(counter()); // 3

function makeAdder(x) {
  return y => x + y;
}
const add5 = makeAdder(5);
console.log(add5(3));  // 8
console.log(add5(10)); // 15

// ══════════════════════════════════════
// 16. Recursion
// ══════════════════════════════════════
function fib(n) {
  if (n <= 1) return n;
  return fib(n - 1) + fib(n - 2);
}
console.log(fib(10)); // 55

function factorial(n) {
  return n <= 1 ? 1 : n * factorial(n - 1);
}
console.log(factorial(6)); // 720

// ══════════════════════════════════════
// 17. Spread
// ══════════════════════════════════════
let arr1 = [1, 2, 3];
let arr2 = [...arr1, 4, 5];
console.log(arr2); // 1,2,3,4,5

function spreadTest(a, b, c) { return a + b + c; }
console.log(spreadTest(...arr1)); // 6

// ══════════════════════════════════════
// 18. Destructuring - Array
// ══════════════════════════════════════
let [p, q, r = 99] = [10, 20];
console.log(p, q, r); // 10 20 99

let [first, ...rest] = [1, 2, 3, 4];
console.log(first); // 1
console.log(rest);  // 2,3,4

let [, second] = [10, 20, 30];
console.log(second); // 20

// ══════════════════════════════════════
// 19. Destructuring - Object
// ══════════════════════════════════════
let { foo, bar = 7 } = { foo: 42 };
console.log(foo); // 42
console.log(bar); // 7

let { a: renamed, b: other = 5 } = { a: 100 };
console.log(renamed); // 100
console.log(other);   // 5

let { x: ox, ...remaining } = { x: 1, y: 2, z: 3 };
console.log(ox);        // 1
console.log(remaining); // [object Object]

// Destructuring in function params
function display({ name, age = 0 }) {
  console.log(`${name} is ${age}`);
}
display({ name: "Ali", age: 25 }); // Ali is 25
display({ name: "Bob" });           // Bob is 0

// ══════════════════════════════════════
// 20. Array Methods
// ══════════════════════════════════════
let nums = [1, 2, 3, 4, 5];
console.log(nums.map(n => n * 2));           // 2,4,6,8,10
console.log(nums.filter(n => n % 2 === 0));  // 2,4
console.log(nums.reduce((a, b) => a + b, 0)); // 15
console.log(nums.find(n => n > 3));          // 4
console.log(nums.findIndex(n => n > 3));     // 3
console.log(nums.some(n => n > 4));          // true
console.log(nums.every(n => n > 0));         // true
console.log(nums.includes(3));               // true

let arr = [3, 1, 4, 1, 5];
arr.sort((a, b) => a - b);
console.log(arr); // 1,1,3,4,5

let nested = [[1, 2], [3, 4], [5]];
console.log(nested.flat()); // 1,2,3,4,5

let copy = nums.slice(1, 3);
console.log(copy); // 2,3

let mutable = [1, 2, 3];
mutable.splice(1, 1, 99, 100);
console.log(mutable); // 1,99,100,3

console.log([1, 2].concat([3, 4], [5])); // 1,2,3,4,5

// forEach
let sum2 = 0;
[10, 20, 30].forEach(n => { sum2 += n; });
console.log(sum2); // 60

// ══════════════════════════════════════
// 21. String Methods
// ══════════════════════════════════════
console.log("  hello  ".trim());           // hello
console.log("hello".toUpperCase());        // HELLO
console.log("WORLD".toLowerCase());        // world
console.log("hello world".includes("world")); // true
console.log("hello".startsWith("he"));    // true
console.log("hello".endsWith("lo"));      // true
console.log("ha".repeat(3));              // hahaha
console.log("5".padStart(3, "0"));        // 005
console.log("abc".padEnd(5, "."));        // abc..
console.log("a-b-c".split("-"));          // a,b,c
console.log("abc".split(""));             // a,b,c
console.log("hello".slice(1, 3));         // el
console.log("hello".slice(-3));           // llo
console.log("hello world".replace("world", "JS")); // hello JS
console.log("aabbaa".replaceAll("a", "x")); // xxbbxx
console.log("hello".indexOf("l"));        // 2
console.log("hello".lastIndexOf("l"));    // 3
console.log("hello".charAt(1));           // e
console.log("hello".charCodeAt(0));       // 104
console.log("hello"[0]);                  // h
console.log("hello".substring(1, 3));     // el

// ══════════════════════════════════════
// 22. Objects
// ══════════════════════════════════════
let person = {
  name: "Ali",
  age: 25,
  greet: function() { return `Hi, I'm ${this.name}`; }
};
console.log(person.name);     // Ali
console.log(person["age"]);   // 25
console.log(person.greet());  // Hi, I'm Ali
person.city = "London";
console.log(person.city);     // London

// Nested objects
let nested2 = { a: { b: { c: 42 } } };
console.log(nested2.a.b.c); // 42

// ══════════════════════════════════════
// 23. Optional Chaining
// ══════════════════════════════════════
let user = { profile: { name: "Ali" } };
console.log(user?.profile?.name);  // Ali
console.log(user?.missing?.name);  // undefined
let nullVal = null;
console.log(nullVal?.toString());  // undefined

// ══════════════════════════════════════
// 24. typeof
// ══════════════════════════════════════
console.log(typeof 42);          // number
console.log(typeof "hi");        // string
console.log(typeof true);        // boolean
console.log(typeof undefined);   // undefined
console.log(typeof null);        // object
console.log(typeof {});          // object
console.log(typeof []);          // object
console.log(typeof function(){}); // function

// ══════════════════════════════════════
// 25. try/catch/finally/throw
// ══════════════════════════════════════
try {
  throw { message: "custom error", code: 42 };
} catch (e) {
  console.log(e.message); // custom error
  console.log(e.code);    // 42
} finally {
  console.log("cleanup"); // cleanup
}

// Catching runtime errors
try {
  let obj2 = null;
  obj2.x; // TypeError
} catch (e) {
  console.log("caught: " + e.message);
}

// Nested try
try {
  try { throw "inner"; }
  catch (e) { console.log("inner catch: " + e); throw "rethrown"; }
} catch (e) {
  console.log("outer catch: " + e);
}

// ══════════════════════════════════════
// 26. Constructor functions (new)
// ══════════════════════════════════════
function Person(name, age) {
  this.name = name;
  this.age = age;
  this.greet = function() {
    return `I'm ${this.name}, age ${this.age}`;
  };
}
let p1 = new Person("Ali", 25);
console.log(p1.name);     // Ali
console.log(p1.greet());  // I'm Ali, age 25

let p2 = new Person("Sara", 30);
console.log(p2.age);      // 30

// ══════════════════════════════════════
// 27. Math built-ins
// ══════════════════════════════════════
console.log(Math.floor(4.9));    // 4
console.log(Math.ceil(4.1));     // 5
console.log(Math.round(4.5));    // 5
console.log(Math.abs(-7));       // 7
console.log(Math.max(1, 5, 3));  // 5
console.log(Math.min(1, 5, 3));  // 1
console.log(Math.pow(2, 10));    // 1024
console.log(Math.sqrt(144));     // 12
console.log(Math.PI.toFixed(4)); // 3.1416
console.log(Math.trunc(-4.9));   // -4
console.log(Math.sign(-5));      // -1

// ══════════════════════════════════════
// 28. Type coercion
// ══════════════════════════════════════
console.log(1 + "2");    // 12
console.log("3" - 1);   // 2
console.log(+"42");      // 42
console.log(+true);      // 1
console.log(+false);     // 0
console.log(+null);      // 0
console.log(Number("3.14")); // 3.14
console.log(String(42));     // 42
console.log(Boolean(0));     // false
console.log(Boolean("hi")); // true

// ══════════════════════════════════════
// 29. instanceof - WILL FAIL
// ══════════════════════════════════════
function Animal(name) { this.name = name; }
let dog = new Animal("Rex");
console.log(dog instanceof Animal); // should be true — throws Unknown operator

// ══════════════════════════════════════
// 30. Shorthand object props - WILL FAIL
// ══════════════════════════════════════
let myX = 5, myY = 10;
let point = { myX, myY }; // Parser expects colon — throws
console.log(point.myX); // 5