# Validation

ORM models are tightly integrated with the [Validation](../common/validation/validation) library and the module comes with a very flexible `com.jkmvc.validate.ValidationException` that helps you quickly handle validation errors from basic CRUD operations.

## Defining Rules

Validation rules are defined in the `OrmMeta::rules` property. This property is an array of rules to be executed by `Validation.execute(exp:String, value:Any, binds:Map<String, Any?>)`.

Each rule is `com.jkmvc.orm.RuleMeta` object, which has 2 properties:
1. `label`: A label is a human-readable version of the field name.
2. `rule`: A validation expression

There are 2 way to define rules
1. override `OrmMeta::rules` property

```
public override val rules: MutableMap<String, IRuleMeta> = hashMapOf(
	"userId" to RuleMeta("用户", "notEmpty"),
	"age" to RuleMeta( "年龄", "between(1,120)")
)
```

2. call `OrmMeta::addRule(name: String, label:String, rule: String?)` method to add rule

```
// 添加标签 + 规则
// add label and rule for field
addRule("name", "姓名", "notEmpty");
addRule("age", "年龄", "between(1,120)");
```

### Bound Values

ORM will automatically bind the following values with `Validation::bind()`:

- **:field** - The name of the field the rule is being applied to.
- **:value** - The value of the field the rule is being applied to.
- **:model** - The instance of the model that is being validated.

## Automatic Validation

All models automatically validate their own data when `ORM::save()`, `ORM::update()`, or `ORM::create()` is called. Because of this, you should always expect these methods to throw an `com.jkmvc.validate.ValidationException` when the model's data is invalid.

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

## External Validation

Certain forms contain information that should not be validated by the model, but by the controller. Information such as a [CSRF](http://en.wikipedia.org/wiki/Cross-site_request_forgery) token, password verification, or a [CAPTCHA](http://en.wikipedia.org/wiki/CAPTCHA) should never be validated by a model. However, validating information in multiple places and combining the errors to provide the user with a good experience is often quite tedius. For this reason, the [ORM_Validation_Exception] is built to handle multiple Validation objects and namespaces the array of errors automatically for you. `ORM::save()`, `ORM::update()`, and `ORM::create()` all take an optional first parameter which is a [Validation] object to validate along with the model.

```
public fun createAction()
{
	try
	{
		val user = UserModel()
		user.username = $_POST['username'];
		user.password = $_POST['password'];

		$extra_rules = Validation::factory($_POST)
			.rule('password_confirm', 'matches', array(
				':validation', ':field', 'password'
			));

		// Pass the extra rules to be validated with the model
		user.save($extra_rules);
	}
	catch (e: ValidationException)
	{
		// handle exception
	}
}
```

Because the validation object was passed as a parameter to the model, any errors found in that check will be namespaced into a sub-array called `_external`. The array of errors would look something like this:

	array(
		'username'  => 'This field cannot be empty.',
		'_external' => array(
			'password_confirm' => 'The values you entered in the password fields did not match.',
		),
	);

This ensures that errors from multiple validation objects and models will never overwrite each other.

[!!] The power of the [ORM_Validation_Exception] can be leveraged in many different ways to merge errors from related models. Take a look at the list of [Examples](examples) for some great use cases.