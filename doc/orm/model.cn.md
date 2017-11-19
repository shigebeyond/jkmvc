# 创建模型

假如你要创建表 `user` 相关的模型，只需要按照以下的语法来创建类 `UserModel`:
1. 继承类 `com.jkmvc.orm.Orm` 
2. 定义伴随对象为元数据

```
class UserModel(id:Int? = null): Orm(id) {
	// 伴随对象就是元数据
 	companion object m: OrmMeta(UserModel::class /* 模型类 */, "User Model" /* 模型名 */, "user" /* 表名 */, "id" /* 表主键 */){}
}
```

## 元数据

ORM 的元数据，就是模型相关的数据库信息，包含以下内容：数据库名, 表名, 主键等等。

ORM 的元数据是用类 `com.jkmvc.orm.OrmMeta` 来表示的，它有以下的属性:
1. `model`: 模型类
2. `label`: 模型名, 默认值是模型名
3. `table`: 表名, 默认值是模型名
4. `primaryKey`: 主键, 默认值是 `id`
5. `dbName`：数据库名, 定义在配置文件`database.yaml` 中, 默认值是 `default`

当你创建 `com.jkmvc.orm.OrmMeta` 对象时，你必须传递上述的属性，就如下面的代码:

```
OrmMeta(UserModel::class /* 模型类 */, "User Model" /* 模型名 */, "user" /* 表名 */, "id" /* 表主键 */, "default" /* 数据库名 */){}
```

## 为模型绑定元数据

对每个模型，定义伴随对象为元数据

```
companion object m: OrmMeta(UserModel::class, "User Model", "user", "id"){}
``` 
