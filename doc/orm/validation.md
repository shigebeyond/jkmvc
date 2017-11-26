# Validation

Orm models are tightly integrated with the [Validation](../common/validation/validation) library which comes with a very flexible `com.jkmvc.validate.ValidationException` that helps you quickly handle validation errors from basic CRUD operations.

## 1 Defining Rules

Validation rules are defined in the `OrmMeta::rules` property. This property is the rules for each field to be executed by `Validation.execute(exp:String, value:Any, binds:Map<String, Any?>)` method.

Each rule is `com.jkmvc.orm.RuleMeta` object, which has 2 properties:
1. `label`: A label is a human-readable version of the field name.
2. `rule`: A validation expression

There are 2 way to define rules
1. override `OrmMeta::rules` property

```
public override val rules: MutableMap<String, IRuleMeta> = hashMapOf(
	"userId" to RuleMeta("Id label", "notEmpty"),
	"age" to RuleMeta( "Age label", "between(1,120)")
)
```

2. call `OrmMeta::addRule(field: String, label:String, rule: String?)` method to add rule

```
// add label and rule for field
addRule("name", "Name label", "notEmpty");
addRule("age", "Age label", "between(1,120)");
```

## 2 Execute validation

Jkmvc execute validation by `Orm.validate() ` method.

It will visit each field's rule in `OrmMeta::rules`, and execute the rule on the field's value using `Validation.execute(exp:String, value:Any, binds:Map<String, Any?>)` method.

The method has actual parameter:
1. `exp`: the field's rule
2. `value`: the field's value
3. `binds`: the other fields' values

## 3 Automatic Validation

All models automatically validate their own data by calling `Orm.validate()` when `Orm::save()`, `Orm::update()`, or `Orm::create()` is called. Because of this, you should always expect these methods to throw an `com.jkmvc.validate.ValidationException` when the model's data is invalid.

```
public fun createAction()
{
	try
	{
		val user = UserModel()
		user.username = 'invalid username';
		user.save();
	}
	catch (e: ValidationException)
	{
		// handle exception
	}
}
```