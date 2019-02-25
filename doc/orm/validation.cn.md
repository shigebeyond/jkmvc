＃ 校验

Orm模型与[Validation](../common/validation/validation.cn.md)库紧密集成，该库提供了异常类`net.jkcode.jkmvc.validate.ValidationException`，来帮助您快速处理基本CRUD操作的验证错误。

## 1 定义校验规则

验证规则是定义在`OrmMeta::rules`属性中。 这个属性包含多个字段的规则，每个规则都是由`Validation.execute(exp:String, value:Any, binds:Map<String, Any?>)`来执行。

每个规则是`net.jkcode.jkmvc.orm.RuleValidator`对象，它有2个属性：
1. `label`：字段中文名
2. `rule`：验证表达式

有两种方法来定义规则
1.重写`OrmMeta :: rules`属性

```
public override val rules: MutableMap<String, IValidator> = hashMapOf(
	"userId" to RuleValidator("用户", "notEmpty"),
	"age" to RuleValidator( "年龄", "between(1,120)")
)
```

2. 调用 `OrmMeta::addRule(field: String, label:String, rule: String?)` 方法来添加单个规则

```
// 添加标签 + 规则
addRule("name", "姓名", "notEmpty");
addRule("age", "年龄", "between(1,120)");
```

## 2执行验证

Jkmvc通过`Orm.validate（）`方法执行验证。

它会遍历`OrmMeta::rules`中的每个字段的规则，并使用`Validation.execute(exp:String, value:Any, binds:Map<String, Any?>)`方法来对字段值执行规则

该方法有3个实际参数：
1. `exp`: 字段规则
2. `value`: 字段值
3. `binds`: 其他字段的值

## 3自动验证

当调用`Orm.validate()`/`Orm::save()`/`Orm::update()`方法时，模型都会自动调用`Orm.validate()`来验证自己的数据。 因此当发现模型的数据无效时，会抛出校验异常`net.jkcode.jkmvc.validate.ValidationException`。

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
		// 处理校验异常
	}
}
```