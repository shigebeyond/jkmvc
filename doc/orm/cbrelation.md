# Relations using callback

Relations using db query related objects with sql

Relations using callback query related objects using callback

Jkmvc ORM supports 2 types of callback relations: `hasMany`, `hasOne`.

## 1 hasMany

The standard `hasMany` relation, eg: a post belongs to a user.  From the user's perspective, a user has many posts. A hasMany relation is defined below:

```
hasMany("posts" /* relation name */, UserModel::id /* this model's primary key getter */, PostModel::userId /* related model's foreign key getter */){ // callback to query related objects
    PostModel.queryBuilder().where("user_id", "IN", it).findModels<PostModel>()
}
```
Let's have a look at the method definition 

```
fun <M:IOrm, K, R> IOrmMeta.cbHasMany(name: String, pkGetter: (M)->K, fkGetter: (R)->K, relatedSupplier:(List<K>) -> List<R>): IOrmMeta
```
Again, for our user and post example, this would look like the following in the user model:

```
hasMany("posts", UserModel::id, PostModel::userId){
    // you can also call rpc
    PostModel.queryBuilder().where("user_id", "IN", it).findModels<PostModel>()
}
```

Using the above, the posts could be access using `user["posts"]`.

## 2 hasOne

A `hasOne` relation is almost identical to a `hasMany` relation.  In a `hasOne` relation, there can be 1 and only 1 relation (rather than 1 or more in a hasMany). If a user can only have one post or story, rather than many then the code would look like this:

```
hasOne("story", UserModel::id, PostModel::userId){
    // you can also call rpc
    PostModel.queryBuilder().where("user_id", "IN", it).findModels<PostModel>()
}
```

# Query related object

use `OrmQueryBuilder.with()` to query related object

```
// Query 1:1 related object, it will merge into 1 sql
val post = PostModel.queryBuilder()
    .with("author")
    .where("id", "=", 20)
    .findModel<PostModel>();

// -------------------------------
// Query 1:N related objects, it will split into 2 sql
val user = UserModel.queryBuilder()
    .with("posts")
    .where("id", "=", 20)
    .findModel<UserModel>();
```