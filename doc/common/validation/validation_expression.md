# Validation expression

The validation expression consists of sub-expressions(function call) and operators, in the form of: `a(1) & b(1,2) && c(3,4) | d(2) . e(1) > f(5)`

## 1 Sub-expressions(function call)

The sub-expression is a function call expression, in the form of `a(1,2)`

### 1.1 Functions

Functions in sub-expression are defined in `net.jkcode.jkmvc.validator.Validation`. We divide the functions into two categories:

1. Functions to test a value

function | useage
--- | ---
notEmpty(value:Any?): Boolean | Check whether not empty
length(value:String, min:Int, max:Int): Boolean | Check whether `value.length` between min and max
min(value:Int, min:Int): Boolean | Check whether value > min
max(value:Int, max:Int): Boolean | Check whether value < max
between(value:Int, min:Int, max:Int): Boolean | Check whether value between min and max
range(value:Int, min:Int, max:Int, step:Int): Boolean | Check whether Check whether value is between min and max, with step
email(value:String): Boolean | Check whether value is mail format
digit(value:String): Boolea | Check whether digital, excluding `.-`
numeric(value:String): Boolea | Check whether numeric, including `.-`
strEquals(value:String, other: String, ignoreCase: Boolean = false): Boolean | Check whether equals
startsWith(value:String, prefix: String, ignoreCase: Boolean = false): Boolean | Check whether has a prefix
endsWith(value:String, suffix: String, ignoreCase: Boolean = false): Boolean | Check whether has a suffix

2. Functions to transform a value

function | useage
--- | ---
trim(value:String): String | Remove whitespace characters on both sides
toUpperCase(value:String): String | transform to uppercase
toLowerCase(value:String): String | transform to lowercase
substring(value:String, startIndex: Int, endIndex: Int): String | get the substring

### 1.2 Call a function

Each function is always performed on a value to test or transform. So for each function, there is always the first argument `value`. Besides, there are other auxiliary parameters, which is divided into two categories: static parameters and dynamic parameters

1. `value` parameter

It's not required in sub-expression. Jkmvc will pass it automatically when executing sub-expression. So sub-expression is something like : `notEmpty ()` or `notEmpty`.

2. static auxiliary parameters 

It's a simple value, such as number / string. eg: `between (1,120)`.

3. dynamic auxiliary parameter

以`:`为前缀，标识一个变量名，其变量值从外部变量中获得，例子 `strEquals(:passwordConfirm)`.

It has `:` prefix, and represents a variable which is obtained from the external variables. eg: ` strEquals (: passwordConfirm) `.

## 2 operator

The operator is used to connect between subexpressions, eg. `&` `&&` `|` `||` `.` `>`

operator | meaning
--- | ---
`&` | And
`&&` | Short-circuit and
`|` | Or
`||` | Short-circuit or
`.` | String concatenation
`>` | Accumulate value with last result

I am not intended to implement complete semantics of Boolean expressions, just to satisfy validation in Request and ORM
So operator has no priority, can only be executed sequentially, not supports parentheses sub-expressions.