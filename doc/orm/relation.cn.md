# 关联关系

Jkmvc的ORM模块，支持5种对象关联关系：`belongsTo`, `hasMany`, `hasOne`, `hasManyThrough` 和 `hasOneThrough`。其中 `hasManyThrough` 关系是利用中间表来保存多对多的关联关系。

## 1 belongsTo 从属于的关系

`belongsTo` 关系用在一个模型从属于其他模型的情况。例如： `Child` 模型从属于 `Parent` 模型，`Flag` 模型从属于 `Country` 模型。

我们在 `OrmMeta::init()` 中定义 `belongsTo` 关系:

```
companion object m: OrmMeta(...){
	init {
		belongsTo("user" /* 关系名 */, UserModel::class /* 关联模型的类 */, "user_id" /* 外键 */, "id" /* 主键 */)
	}
}
```

让我们来看看方法定义 `IOrmMeta::belongsTo(name:String, relatedModel: KClass<out IOrm>, 外键:String, primaryKey:DbKeyColumn, conditions:Map<String, Any?>): IOrmMeta`

`name` 参数是关系名, 是用来在代码中访问关联模型。譬如你有`Post` 模型，从属于`User`模型（就是一个文章从属于一个用户），则你可以定义 `belongsTo`关系如下:

```
belongsTo("user", UserModel::class)
// 等价于
belongsTo("user", UserModel::class, "user_id", "id")
```

以后，你就可以使用 `post["user"]`来访问`User`模型。 `foreignKey`外键与 `primaryKey`主键参数是有默认值的。 表 post 的外键默认是模型名+`_id`，在当前例子是`user_id`.

如果表post中关联user表的字段不是`user_id`，而是字段`author_id`，则你可以修改`foreignKey`参数

```
belongsTo("user", UserModel::class, "author_id" /* 外键 */)
```

如果你想通过`post["author"]`来访问`User`模型（就是文章作者啦），那么你可以修改`name`参数:

```
belongsTo("author" /* 关系名 */, UserModel::class, "author_id")
```
## 2 hasMany 有多个的关系

`hasMany`关系，其实是`belongsTo`关系的另一面。还是上面的例子， `Post` 模型从属于`User`模型（就是一个文章从属于一个用户）。从用户角度，`User` 模型有多个`Post`模型（就是一个用户有多个文章），我们可以这样定义`hasMany`关系：

```
hasMany("posts" /* 关系名 */, PostModel::class /* 关联模型的类 */, "post_id" /* 外键 */, "id" /* 主键 */)
```
让我们来看看方法定义： 

```
IOrmMeta::hasMany(name: String, relatedModel: KClass<out IOrm>, foreighKey: String, primaryKey:DbKeyColumn, conditions: Map<String, Any?>): IOrmMeta
```

接上面的例子，`User`模型的的关系定义如下

```
hasMany("posts", PostModel::class)
// 等价于
hasMany("posts", PostModel::class, "post_id", "id")
```

以后，你就可以通过`user["posts"]`来访问`Post`模型（就是某用户的文章啦）

一般来说，我会使用模型名的复数形式，作为`hasMany`的关系名。在该例中，我们使用`posts`作为关系名。另外，外键的默认值是模型名+`_id`。 当然`user_id`字段必须先存在于post表中。

如果我们想用关系名 `stories`，而不是`posts`。你可以修改`name`参数：

```
hasMany("stories" /* 关系名 */, PostModel::class, "post_id", "id")
```

## 3 hasOne 有一个的关系

`hasOne`关系基本等同于`hasMany`关系。只是`hasOne`关系是一对一的，而`hasMany`是一对多。如果一个用户只有一个文章，则我们的代码是这样的：

```
hasOne("story", PostModel::class, "post_id", "id")
// 等价于
hasOne("story", PostModel::class)

```

## 4 hasManyThrough 通过中间表来构建的有多个的关系

`hasManyThrough`关系主要用于多对多的关联情况。例子现在我们有一个新的模型 `Category`（文章分类）。一个文章可以从属于多个分类，每个分类可以有多个文章。要让这两个表建立关联，则需要另外的表来保存关联关系，这个表必须包含2个字段： 文章主键`post_id` 与分类主键 `category_id`。中间表一般命名为`categories_post`，对应的模型名一般为 `Category_Post`。

我们使用`hasManyThrough()`方法来定义`hasManyThrough`关系，调用语法跟`hasMany()`差不多，只是多了3个参数：

我们先来看看函数定义

```
IOrmMeta::hasManyThrough(name: String, 
	relatedModel: KClass<out IOrm>,
	foreignKey:DbKeyColumn = this.defaultForeignKey,
	primaryKey:DbKeyColumn = this.primaryKey,
	middleTable:String = table + '_' + relatedModel.modelOrmMeta.table,
	farForeignKey:DbKeyColumn = relatedModel.modelOrmMeta.defaultForeignKey,
	farPrimaryKey:DbKeyColumn = relatedModel.modelOrmMeta.primaryKey,
	conditions: Map<String, Any?> = emptyMap()
): IOrmMeta
```

在`Post`模型，我们这么定义：

```
hasManyThrough("categories", CategoryModel::class, "post_id", "id", "categories_posts", "category_id", "id") 
```

在`Category`模型，定义是这样的：

```
hasManyThrough("posts", PostModel::class, "category_id", "id", "categories_posts", "post_id", "id") 
```

以后，你只需要简单调用`post["categories"]`与`category["posts"]`，就可以访问其关联的分类与文章。

## 5 hasOneThrough 通过中间表来构建的有一个的关系

`hasOneThrough`关系适用于通过中间表来关联的一对一的关联关系。它基本上跟`hasManyThrough`关系是一样的。你可以通过调用`hasOneThrough()`方法来定义该关系。

接着上面的例子，我们这样定义`hasOneThrough`关系

```
hasOneThrough("category", CategoryModel::class, "post_id", "id", "categories_posts", "category_id", "id") 
```

## 6 自定义关联条件或关联查询

自定义关联条件:
```
hasOne("home", AddressModel::class, conditions = mapOf("is_home" to 1))
```

等价于以下的自定义关联查询:
```
hasOne("home", AddressModel::class){ query, lazy ->
    if(lazy) // 是否懒加载
        query.where("is_home", 1)
    else
        query.on("is_home", 1, false)
}
```

这样可以非常容易的定义各种复杂的关联关系.

## 7 管理关联关系

Jkmvc提供了便捷的方法来管理关联关系，如检查关系/关系计数/添加关系/删除关系。首先，我们先假定有2个对象：已加载的`Post`模型对象post，与已加载的`Category`模型对象category。

### 7.1 检查关系
你可以这样来检查post是否关联了category:

```
post.hasRelation('categories', category);
```

有2个参数： 1 关系名 2 要检查的关联模型对象

### 7.2 添加关系

接下来我们添加一下关系，就是在中间表 categories_posts 中插入一条记录，就这么简单调用下就行了

```
post.addRelation('categories', category);
```

### 7.3 删除关系

删除关系，就是删除中间表 categories_posts 中对应的记录，简单调用以下代码即可：

```
post.removeRelation('categories', category);
```

# 关联对象查询

使用 `OrmQueryBuilder.with()` 来联查关联对象, 框架会自动帮你解决联查放大问题

```
// 联查一对一关联对象, 合并为1条sql查询
val post = PostModel.queryBuilder()
    .with("author")
    .where("id", "=", 20)
    .findModel<PostModel>();
// 生成sql: SELECT ... FROM `post` `post` JOIN `user` `author` ON `post`.`author_id` = `author`.`id` WHERE `post`.`id` = 20:

// -------------------------------
// 联查一对多关联对象, 分开2条sql查询
val user = UserModel.queryBuilder()
    .with("posts")
    .where("id", "=", 20)
    .findModel<UserModel>();
// 生成2条sql:
// SELECT  `user`.* FROM `user` `user` WHERE `user`.`id` = 20
// SELECT  * FROM `post` `post` WHERE  `post`.`author_id` IN (1)

// -------------------------------
// 联查一对多关联对象时, 可动态操作关联对象的查询对象
val user = UserModel.queryBuilder()
    .with("posts"){ query -> // 动态操作查询对象, 如添加条件与顺序
        query.where("visible", 1).orderBy("created")
    }
    .where("id", "=", 20)
    .findModel<UserModel>();
// 生成2条sql:
// SELECT  `user`.* FROM `user` `user` WHERE `user`.`id` = 20
// SELECT  * FROM `post` `post` WHERE  `post`.`author_id` IN (1) AND `visible` = 1  ORDER BY `created`
```