# Relations

Jkmvc ORM supports 5 types of object relations: `belongsTo`, `hasMany`, `hasOne`, `hasManyThrough` and `hasOneThrough`. The `hasManyThrough` relation use middle table to save many-to-many relation

## 1 belongsTo

A `belongsTo` relation should be used when you have one model that belongs to another. For example, a `Child` model belongsTo a `Parent` or a `Flag` model `belongsTo` a `Country`.

This is the base `belongsTo` relation defined in `OrmMeta::init()`:

```
companion object m: OrmMeta(...){
	init {
		belongsTo("user" /* relation name */, UserModel::class /* relatedModel */, "user_id" /* foreignKey */, "id" /* primaryKey */)
	}
}
```

Let's have a look at the method definition `IOrmMeta::belongsTo(name:String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyColumn, primaryKey:DbKeyColumn, conditions:Map<String, Any?>): IOrmMeta`

`name` parameter is the relation name, it's what is used to access the related model in your code. If you had a `Post` model that belonged to a `User` model and wished to use the default values of the `belongsTo` configuration then your code would look like this:

```
belongsTo("user", UserModel::class)
// equals
belongsTo("user", UserModel::class, "user_id", "id")
```

To access the user model, you would use `post["user"]`.  The `foreignKey` and `primaryKey` has a default value. The `foreignKey` in the post table will be the model name followed by `_id`, in this case it would be `user_id`.

Let's say your `Post` database table schema doesn't have a `user_id` column but instead has an `author_id` column which is a foreign key for a row in the `User` table. You could use code like this:

```
belongsTo("user", UserModel::class, "author_id" /* foreignKey */)
```

If you wanted access a post's author by using code like `post["author"]` then you would simply need to change the relation name:

```
belongsTo("author" /* relationName */, UserModel::class, "author_id")
```
## 2 hasMany

The standard `hasMany` relation will likely fall on the other side of a `belongsTo` relation.  In the above examples, a post belongs to a user.  From the user's perspective, a user has many posts. A hasMany relation is defined below:

```
hasMany("posts" /* relation name */, PostModel::class /* relatedModel */, "post_id" /* foreignKey */, "id" /* primaryKey */)
```
Let's have a look at the method definition 

```
IOrmMeta::hasMany(name: String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyColumn, primaryKey:DbKeyColumn, conditions: Map<String, Any?>): IOrmMeta
```
Again, for our user and post example, this would look like the following in the user model:

```
hasMany("posts", PostModel::class)
// equals
hasMany("posts", PostModel::class, "post_id", "id")
```

Using the above, the posts could be access using `user["posts"]`.

I usually use the model name (plurality form) as the `hasMany` relation name.  In this case, use `posts` as the relation name.  The foreign key used by default is the owner model's name followed by `_id`.  In this case, the foreign key will be `user_id` and it must exist in the post table as before.

Let's assume now you want to access the posts using the name `stories` instead, and are still using the `author_id` key as in the `belongsTo` example.  You would define your hasMany relation as:

```
hasMany("stories", PostModel::class, "post_id", "id")
```

## 3 hasOne

A `hasOne` relation is almost identical to a `hasMany` relation.  In a `hasOne` relation, there can be 1 and only 1 relation (rather than 1 or more in a hasMany). If a user can only have one post or story, rather than many then the code would look like this:

```
hasOne("story", PostModel::class, "post_id", "id")
// equals
hasOne("story", PostModel::class)

```

## 4 hasManyThrough

A `hasManyThrough` relation is used for many-to-many relations.  For instance, let's assume now we have an additional model, called `Category`.  Posts may belong to more than one category, and each category may have more than one post.  To link them together, an additional table is needed with columns for a `post_id` and a `category_id` (sometimes called a pivot table).  We'll name the model for this `Category_Post` and the corresponding table `categories_post`.

To define the `hasManyThrough` relation, use `hasManyThrough()` method with the same syntax for standard hasMany relations is used with the addition of 3 parameter.  

Let's have a look at the method definition 

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

Let's assume we're working with the Post model:

```
hasManyThrough("categories", CategoryModel::class, "post_id", "id", "categories_posts", "category_id", "id") 
```

In the Category model:

```
hasManyThrough("posts", PostModel::class, "category_id", "id", "categories_posts", "post_id", "id") 
```

To access the categories and posts, you simply use `post["categories"]` and `category["posts"]`

## 5 hasOneThrough

A `hasOneThrough` relation is used for 1-to-1 relations through a middle table. It just like the `hasManyThrough` relation, and it's defined using `hasOneThrough()` method which also like `hasManyThrough()` method. 

Use above example, we define our `hasOneThrough` relation.

```
hasOneThrough("category", CategoryModel::class, "post_id", "id", "categories_posts", "category_id", "id") 
```

## 6 Manage relation

Methods are available to check for, count, add, and remove relations for relations.  Let's assume you have a post model loaded, and a category model loaded as well. 

### 6.1 check relation

You can check to see if the post is related to this category with the following call:

```
post.hasRelation('categories', category);
```

The first parameter is the relation name to use (in case your post model has more than one relation to the category model) and the second is the model to check for a relation with.

### 6.2 add relation

Assuming you want to add the relation (by creating a new row in the categories_posts table), you would simply do:

```
post.addRelation('categories', category);
```

### 6.3 remove relation

To remove:

```
post.removeRelation('categories', category);
```


# Query related object

use `OrmQueryBuilder.with()` to query related object

```
// Query 1:1 related object, it will merge into 1 sql
val post = PostModel.queryBuilder()
    .with("author")
    .where("id", "=", 20)
    .findModel<PostModel>();
// Generate sql: SELECT ... FROM `post` `post` JOIN `user` `author` ON `post`.`author_id` = `author`.`id` WHERE `post`.`id` = 20:

// -------------------------------
// Query 1:N related objects, it will split into 2 sql
val user = UserModel.queryBuilder()
    .with("posts")
    .where("id", "=", 20)
    .findModel<UserModel>();
// Generate 2 sql:
// SELECT  `user`.* FROM `user` `user` WHERE `user`.`id` = 20
// SELECT  * FROM `post` `post` WHERE  `post`.`author_id` IN (1)

// -------------------------------
// When you query 1:N related objects, you can dynamic call related query object
val user = UserModel.queryBuilder()
    .with("posts"){ query -> // call related query object, eg. add where condition or add order
        query.where("visible", 1).orderBy("created")
    }
    .where("id", "=", 20)
    .findModel<UserModel>();
// Generate 2 sql:
// SELECT  `user`.* FROM `user` `user` WHERE `user`.`id` = 20
// SELECT  * FROM `post` `post` WHERE  `post`.`author_id` IN (1) AND `visible` = 1  ORDER BY `created`
```