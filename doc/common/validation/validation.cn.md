# 校验

使用 `Validation::execute(exp:String, value:Any, variables:Map<String, Any?>): ValidationResult` 方法来执行校验。 

该方法需要3个参数：
1. `exp`: 校验表达式，[语法参考](validation_expression.cn.md)
2. `value`: 待校验的值
3. `variables`: 校验表达式中需要的其他变量

返回值类型是 `net.jkcode.jkmvc.validator.ValidationResult`，它有3个属性:
1. `result`: 执行结果
1. `unit`: 最后一个校验单元（函数调用）
1. `lastValue`: 最后的累积的值

## 例子

1. 校验值

代码:

```
val exp = ValidationExpr("min(1)");
        val result = exp.execute("3");
        println(result)
```

结果:

```
ValidationResult(result=true, unit=ValidationUint(operator=null, func=min, params=[1]), lastValue=3)
```

2. 转换值

代码:

```
val (result) = Validation.execute("trim . toUpperCase . substring(2,-1)", " model ");
println(result)
```

结果:

```
ValidationResult(result=model MODEL odel , unit=ValidationUint(operator=., func=substring, params=[2, -1]), lastValue= model )
```
