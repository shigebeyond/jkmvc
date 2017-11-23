# Relations

Jkmvc ORM supports 5 types of object relations: `belongsTo`, `hasMany`, `hasManyThrough` , `hasOne` and `hasOneThrough`. The `hasManyThrough` relation can be used to function like Active Record's `hasMany_and_belongsTo` relation type.

## belongsTo

A `belongsTo` relation should be used when you have one model that belongs to another. For example, a `Child` model belongsTo a `Parent` or a `Flag` model `belongsTo` a `Country`.

This is the base `belongsTo` relation defined in `OrmMeta::init()`:

```
companion object m: OrmMeta(...){
	init {
		belongsTo("user" /* name */, UserModel::class /* relatedModel */, "user_id" /* foreignKey */, "id" /* primaryKey */)
	}
}
```

Let's have a look at the method definition `IOrmMeta::belongsTo(name:String, relatedModel: KClass<out IOrm>, foreignKey:String, primaryKey: String, conditions:Map<String, Any?>): IOrmMeta`

`name` parameter is the relation name, it's what is used to access the related model in your code. If you had a `Post` model that belonged to a `User` model and wished to use the default values of the `belongsTo` configuration then your code would look like this:

```
belongsTo("user", UserModel::class)
// equals
belongsTo("user", UserModel::class, "user_id", "id")
```

To access the user model, you would use `post["user"]`.  Since we're using the defaults above, the `name` will be used for the model name, and the foreign key in the post table will be the `name` followed by `_id`, in this case it would be `user_id`.

Let's say your `Post` database table schema doesn't have a `user_id` column but instead has an `author_id` column which is a foreign key for a record in the `User` table. You could use code like this:

```
belongsTo("user", UserModel::class, "author_id")
```

If you wanted access a post's author by using code like `post["author"]` then you would simply need to change the alias and add the `model` index:

```
belongsTo("author", UserModel::class, "author_id")
```
## hasMany

The standard `hasMany` relation will likely fall on the other side of a `belongsTo` relation.  In the above examples, a post belongs to a user.  From the user's perspective, a user has many posts. A hasMany relation is defined below:

```
hasMany("posts" /* name */, PostModel::class /* relatedModel */, "post_id" /* foreignKey */, "id" /* primaryKey */)
```
Let's have a look at the method definition 

```
IOrmMeta::hasMany(name: String, relatedModel: KClass<out IOrm>, foreignKey: String, primaryKey: String, conditions: Map<String, Any?>): IOrmMeta
```
Again, for our user and post example, this would look like the following in the user model:

```
hasMany("posts", PostModel::class)
// equals
hasMany("posts", PostModel::class, "post_id", "id")
```

Using the above, the posts could be access using `user["posts"]`.

The model name used by default will be the singular name of the alias using the `inflector` class.  In this case, `posts` uses `post` as the model name.  The foreign key used by default is the owner model's name followed by `_id`.  In this case, the foreign key will be `user_id` and it must exist in the posts table as before.

Let's assume now you want to access the posts using the name `stories` instead, and are still using the `author_id` key as in the `belongsTo` example.  You would define your hasMany relation as:

```
hasMany("stories", PostModel::class, "post_id", "id")
```

## hasOne

A `hasOne` relation is almost identical to a `hasMany` relation.  In a `hasOne` relation, there can be 1 and only 1 relation (rather than 1 or more in a hasMany). If a user can only have one post or story, rather than many then the code would look like this:

```
hasOne("story", PostModel::class, "post_id", "id")
// equals
hasOne("story", PostModel::class)

```

## hasManyThrough

A `hasManyThrough` relation is used for many-to-many relations.  For instance, let's assume now we have an additional model, called `Category`.  Posts may belong to more than one category, and each category may have more than one post.  To link them together, an additional table is needed with columns for a `post_id` and a `category_id` (sometimes called a pivot table).  We'll name the model for this `Post_Category` and the corresponding table `categories_posts`.

To define the `hasManyThrough` relation, use `hasManyThrough` method with the same syntax for standard hasMany relations is used with the addition of 3 parameter.  

Let's have a look at the method definition 

```
IOrmMeta::hasManyThrough(name: String, 
	relatedModel: KClass<out IOrm>,
	foreignKey: String = this.defaultForeignKey /* 主表_主键 = 本表_主键 */,
	primaryKey: String = this.primaryKey /* 本表的主键 */,
	middleTable:String = table + '_' + relatedModel.modelOrmMeta.table /* 主表_从表 */,
	farForeignKey:String = relatedModel.modelOrmMeta.defaultForeignKey /* 远端主表_主键 = 从表_主键 */,
	farPrimaryKey:String = relatedModel.modelOrmMeta.primaryKey,/* 从表的主键 */
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

Methods are available to check for, add, and remove relations for many-to-many relations.  Let's assume you have a post model loaded, and a category model loaded as well.  You can check to see if the post is related to this category with the following call:

```
post.has('categories', category);
```

The first parameter is the alias name to use (in case your post model has more than one relation to the category model) and the second is the model to check for a relation with.

Assuming you want to add the relation (by creating a new record in the categories_posts table), you would simply do:

```
post.add('categories', category);
```

To remove:

```
post.remove('categories', category);
```

## hasOneThrough

A `hasOneThrough` relation is used for 1-to-1 relations through a middle table. It just like the `hasManyThrough` relation, and it's defined using `hasOneThrough()` method which also like `hasManyThrough()` method. 

Use above example, we define our `hasOneThrough` relation.

```
hasOneThrough("category", CategoryModel::class, "post_id", "id", "categories_posts", "category_id", "id") 
```



