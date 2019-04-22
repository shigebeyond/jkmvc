# Validation expression

校验表达式是由多个(函数调用的)子表达式与运算符组成, 形式为 `a(1) & b(1,2) && c(3,4) | d(2) . e(1) > f(5)`

## 1 (函数调用的)子表达式

子表达式是函数调用, 形式为 a(1,2)

### 1.1 函数

在子表达式中调用的所有函数，都是定义在类`net.jkcode.jkmvc.validator.Validation` 中的方法，我们将方法分成2类

1. 校验值的函数
方法 | 作用
--- | ---
notEmpty(value:Any?): Boolean | 检查非空
length(value:String, min:Int, max:Int): Boolean | 检查长度
min(value:Int, min:Int): Boolean | 检查最小值
max(value:Int, max:Int): Boolean | 检查最大值
between(value:Int, min:Int, max:Int): Boolean | 检查是否在某个范围内
range(value:Int, min:Int, max:Int, step:Int): Boolean | 检查是否在某个范围内
email(value:String): Boolean | 检查是否邮件格式
digit(value:String): Boolea | 检查是否数字，不包含`.-`
numeric(value:String): Boolea | 检查是否数值，包含`.-`
strEquals(value:String, other: String, ignoreCase: Boolean = false): Boolean | 检查字符串是否相等
startsWith(value:String, prefix: String, ignoreCase: Boolean = false): Boolean | 检查是否有前缀
endsWith(value:String, suffix: String, ignoreCase: Boolean = false): Boolean | 检查是否有后缀

2. 转换值的函数
方法 | 作用
--- | ---
trim(value:String): String | 除两边的空白字符
toUpperCase(value:String): String | 换为大写
toLowerCase(value:String): String | 换为小写
substring(value:String, startIndex: Int, endIndex: Int): String | 截取子字符串

### 1.2 调用函数

每个函数都是作用在一个值上的，因此对每个函数，总是有着第一个参数`value`，此外也会存在其他辅佐的参数，辅佐参数可分成两类：静态参数 与 动态参数

1. `value`参数

`value`参数是不需要写在函数调用的子表达式中，jkmvc会在执行校验表达式时自动传递。因此，函数调用是这样的 `notEmpty()` 或 `notEmpty` （如果只有一个）

接下来我们来看看其他辅佐的参数

2. 静态的辅佐参数

就是一个简单的值，如数字/字符串等，例子 `between(1,120)`.

3. 动态的辅佐参数

以`:`为前缀，标识一个变量名，其变量值从外部变量中获得，例子 `strEquals(:passwordConfirm)`.

## 2 操作符

子表达式之间用运算符连接, 运算符有 & && | || . >

运算符 | 含义
--- | ---
`&` | 与
`&&` | 短路与
`|` | 或
`||` | 短路或
`.` | 字符串连接
`>` | 累积结果

无意于实现完整语义的布尔表达式, 暂时先满足于输入校验与orm保存数据时的校验, 因此:
运算符没有优先级, 只能按顺序执行, 不支持带括号的子表达式

