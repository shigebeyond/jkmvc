# 基于回调的关联关系

基于db实现的关联关系, 是通过拼接主键外键条件的联查sql, 来查询关联对象的

而基于回调实现的关联关系, 是使用回调来查询关联对象的.

Jkmvc的ORM模块，支持2种回调实现的关联关系：`hasMany`, `hasOne`

## 1 hasMany 有多个的关系

`hasMany`关系，如 `Post` 模型从属于`User`模型（就是一个文章从属于一个用户）。从用户角度，`User` 模型有多个`Post`模型（就是一个用户有多个文章），我们可以这样定义`hasMany`关系：

```
hasMany("posts" /* 关系名 */, UserModel::id /* 主模型的主键的getter */, PostModel::userId /* 从对象的外键的getter */){ // 批量获取关联对象的回调
    PostModel.queryBuilder().where("user_id", "IN", it).findModels<PostModel>()
}
```
让我们来看看方法定义： 

```
fun <M:IOrm, K, R> IOrmMeta.cbHasMany(name: String, pkGetter: (M)->K, fkGetter: (R)->K, relatedSupplier:(List<K>) -> List<R>): IOrmMeta
```

接上面的例子，`User`模型的的关系定义如下

```
hasMany("posts", UserModel::id, PostModel::userId){
     // 自定义的查询, 可以是rpc
     PostModel.queryBuilder().where("user_id", "IN", it).findModels<PostModel>()
}
```

以后，你就可以通过`user["posts"]`来访问`Post`模型（就是某用户的文章啦）

## 2 hasOne 有一个的关系

`hasOne`关系基本等同于`hasMany`关系。只是`hasOne`关系是一对一的，而`hasMany`是一对多。如果一个用户只有一个文章，则我们的代码是这样的：

```
hasOne("story", UserModel::id, PostModel::userId){
     // 自定义的查询, 可以是rpc
     PostModel.queryBuilder().where("user_id", "IN", it).findModels<PostModel>()
}
```

# 关联对象查询

使用 `OrmQueryBuilder.with()` 来联查关联对象

```
// 联查一对一关联对象
val post = PostModel.queryBuilder()
    .with("author")
    .where("id", "=", 20)
    .findModel<PostModel>();

// -------------------------------
// 联查一对多关联对象
val user = UserModel.queryBuilder()
    .with("posts")
    .where("id", "=", 20)
    .findModel<UserModel>();
```