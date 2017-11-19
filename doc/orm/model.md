# Creating your Model

To create a model for the table `user` in your database, create the `UserModel` class with the following syntax:
1. extends `com.jkmvc.orm.Orm` class
2. define companion object as meta data 

```
class UserModel(id:Int? = null): Orm(id) {
	// companion object is meta data
 	companion object m: OrmMeta(UserModel::class /* model class */, "User Model" /* model label */, "user" /* table name */, "id" /* table primary key */){}
}
```

## Meta data

Meta data is the database information about this model, including database name, table name, primary key, etc.

Orm's meta data reprensents by class `com.jkmvc.orm.OrmMeta`, which has the follow properties:
1. model class
2. model label, default is model name
3. table name, default is model name
4. primary key, , default is `id`
5. database name, which is defined in `database.yaml`, default is `default`

When you create a `com.jkmvc.orm.OrmMeta` object, you must pass these properties, just like:

```
OrmMeta(UserModel::class /* model class */, "User Model" /* model label */, "user" /* table name */, "id" /* table primary key */, "default" /* database name */){}
```

## Bind meta data to model

For each model, you must define meta data in companion object.

```
companion object m: OrmMeta(UserModel::class, "User Model", "user", "id"){}
``` 
