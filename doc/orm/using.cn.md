# 基础用户

## 1 创建模型实例

创建 `UserModel` 实例

```
val user = UserModel();
```

## 2 插入

如果你要往数据库中插入一条新的记录，首先，要创建一个模型对象

```
val user = UserModel();
```

然后，给对象属性赋值

```
user.firstName = "Trent";
user.lastName = "Reznor";
user.city = "Mercer";
user.state = "PA";
```

使用 `Orm::save()` 来往数据库中插入就来：

```
user.save();
```

`Orm::save()` checks to see if a value is set for the primary key (`id` by default). If the primary key is set, then ORM will execute an `UPDATE` otherwise it will execute an `INSERT`.

## 3 查询单个对象

要查询单个对象有2种方法
1. 调用 `Orm.queryBuilder()` 方法来获得查询构建器，使用查询构建器来查询
2. 在模型构造函数中传递`id`参数

```
// 查询id为20的用户
val user = UserModel.queryBuilder()
    .where("id", "=", 20)
    .findModel<UserModel>();
// 或者
val user = UserModel(20);
```

## 4 检查模型对象是否已加载了数据

使用 `Orm::loaded` 属性来检查模型对象是否从数据库中加载了数据

```
if (user.loaded)
{
    // 加载数据成功
}
else
{
    // 失败
}
```

## 5 更新数据

一旦模型加载了数据，你就可以更新模型的属性，如下文

```
user.firstName = "Trent";
user.lastName = "Reznor";
```

如果你想保存更新到数据库中，只要调用`save()` 即可:

```
user.save();
```

## 6 删除

如果你要在数据库中删除记录，你只要对一个加载了数据的模型，调用 `Orm::delete()` 方法即可。

```
val user = UserModel(20);
user.delete();
```
	
## 7 批量赋值

1. 从 `Map` 中赋值

如果你要批量赋值，请使用 `Orm::values(values: Map<String, Any?>, expected: List<String>? = null)`

```
val user = UserModel(20)
val values = mapOf("username" to "shi", "password" to "123456")
val expected = listOf("username","password")
user.values(values, expected)
user.create()
```

2. 从 `HttpRequest` 中赋值

通常，我们要从请求中读取相关的属性值，只要使用 `com.jkmvc.http.valuesFromRequest` 方法即可。

当然，请求中的数据都是字符串类型，但是我们的属性却不一定是字符串类型的，`com.jkmvc.http.valuesFromRequest` 会智能转换请求中的值，并赋值给属性。

```	
import com.jkmvc.http.valuesFromRequest

try
{
    val user = UserModel(20);
    val expected = listOf("username","password")
    user.valuesFromRequest(req, expected)
    user.update();
}
catch (e: OrmException)
{
    // 处理异常...
}
```

[!!] 虽然方法的第二个参数是可选的，如果你不填的话，则将请求中的所有数据都赋值给模型对象。但我还是建议你要指定哪些字段要赋值，否则你的代码是不安全的，会让黑客会注入你不想赋值的属性。

