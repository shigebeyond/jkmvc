# Validation

Validation can be performed on a value using the `Validation::execute(exp:String, value:Any, variables:Map<String, Any?>): ValidationResult` method. 

It needs 3 paramters:
1. `exp`: validation expression，[syntax reference](validation_expression.md)
2. `value`: value to validate 
3. `variables`: other values used as validate paramerters

The return type is `net.jkcode.jkmvc.validator.ValidationResult`, it has 3 properties:
1. `result`: the execution result
1. `unit`: the last validation unit(function call)
1. `lastValue`: the accumulate value 

## Example

1. Test a value

Codes:

```
val exp = ValidationExpr("min(1)");
        val result = exp.execute("3");
        println(result)
```

Result:

```
ValidationResult(result=true, unit=ValidationUint(operator=null, func=min, params=[1]), lastValue=3)
```

2. Transform a value

Codes:

```
val (result) = Validation.execute("trim . toUpperCase . substring(2,-1)", " model ");
println(result)
```

Result:

```
ValidationResult(result=model MODEL odel , unit=ValidationUint(operator=., func=substring, params=[2, -1]), lastValue= model )
```
