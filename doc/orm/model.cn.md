# 模型

## 1 创建模型

假如你要创建表 `user` 相关的模型，只需要按照以下的语法来创建类 `UserModel`:
1. 继承类 `net.jkcode.jkmvc.orm.Orm` 
2. 定义伴随对象为元数据
3. 使用`property()`来定义代理属性

```
class UserModel(id:Int? = null): Orm(id) {
	// 伴随对象就是元数据
 	companion object m: OrmMeta(UserModel::class /* 模型类 */, "User Model" /* 模型名 */, "user" /* 表名 */, "id" /* 表主键 */){}
	
	// 代理属性读写
	public var id:Int by property() // 用户编号 

	public var username:String by property() // 用户名 

	public var password:String by property() // 密码 
}
```

## 2 元数据

ORM 的元数据，就是模型相关的数据库信息，包含以下内容：数据库名, 表名, 主键等等。

ORM 的元数据是用类 `net.jkcode.jkmvc.orm.OrmMeta` 来表示的，它有以下的属性:
1. `model`: 模型类
2. `label`: 模型名, 默认值是模型名
3. `table`: 表名, 默认值是模型名
4. `primaryKey`: 主键, 默认值是 `id`
5. `dbName`：数据库名, 定义在配置文件`database.yaml` 中, 默认值是 `default`

当你创建 `net.jkcode.jkmvc.orm.OrmMeta` 对象时，你必须传递上述的属性，就如下面的代码:

```
OrmMeta(UserModel::class /* 模型类 */, "User Model" /* 模型名 */, "user" /* 表名 */, "id" /* 表主键 */, "default" /* 数据库名 */){}
```

## 3 为模型绑定元数据

对每个模型，定义伴随对象为元数据

```
companion object m: OrmMeta(UserModel::class, "User Model", "user", "id"){}
``` 

## 4 自动生成模型代码

Jkmvc提供了`net.jkcode.jkmvc.util.ModelGenerator` 来自动生成模型代码

```
val generator = ModelGenerator("/home/shi/code/java/jkmvc/jkmvc-example/src/main/kotlin" /* 源码目录 */, "net.jkcode.jkmvc.example.model" /* 包路径 */ "default" /* 数据库名 */, "shijianhang" /* 作者 */)
generator.genenateModelFile("UserModel" /* 模型类名 */, "用户模型" /* 模型名 */, "user" /* 表名 */)
```

它会根据指定的数据库与指定的表，来生成模型代码。其中根据`database.yaml`中的配置项 `columnUnderline` 和 `columnUpperCase` 来将数据库的字段名转换为对象属性名。

生成模型代码如下：

```
class UserModel(id:Int? = null): Orm(id) {
	// 伴随对象就是元数据
 	companion object m: OrmMeta(UserModel::class /* 模型类 */, "User Model" /* 模型名 */, "user" /* 表名 */, "id" /* 表主键 */){}
	
	// 代理属性读写
	public var id:Int by property() // 用户编号 

	public var username:String by property() // 用户名 

	public var password:String by property() // 密码 
}
```