# Basic Usage

## 1 Load a new model instance

create a new `UserModel` instance

```
val user = UserModel();
```

## 2 Inserting

To insert a new row into the database, create a new instance of the model:

```
val user = UserModel();
```

Then, assign values for each of the properties;

```
user.firstName = "Trent";
user.lastName = "Reznor";
user.city = "Mercer";
user.state = "PA";
```

Insert the new row into the database by running `Orm::save()`:

```
user.save();
```

`Orm::save()` checks to see if a value is set for the primary key (`id` by default). If the primary key is set, then ORM will execute an `UPDATE` otherwise it will execute an `INSERT`.

## 3 Finding an object

To find an object you can call the `Orm.queryBuilder()` method to get a query builder or pass the id into the model constructor:

```
// Find user with ID 20
val user = UserModel.queryBuilder()
    .where("id", "=", 20)
    .findModel<UserModel>();
// Or
val user = UserModel(20);
```

## 4 Check that ORM loaded a row

Use the `Orm::loaded` property to check that ORM successfully loaded a row.

```
if (user.loaded)
{
    // Load was successful
}
else
{
    // Error
}
```

## 5 Updating and Saving

Once an ORM model has been loaded, you can modify a model's properties like this:

```
user.firstName = "Trent";
user.lastName = "Reznor";
```

And if you want to save the changes you just made back to the database, just run a `save()` call like this:

```
user.save();
```

## 6 Deleting

To delete an object, you can call the `Orm::delete()` method on a loaded ORM model.

```
val user = UserModel(20);
user.delete();
```
	
## 7 Mass assignment

1. from `Map`

To set multiple values at once, use `Orm::fromMap(values: Map<String, Any?>, expected: List<String> = emptyList())`

```
val user = UserModel(20)
val values = mapOf("username" to "shi", "password" to "123456")
val expected = listOf("username","password")
user.fromMap(values, expected)
user.create()
```

2. from `HttpRequest`

We usually get values from request, just use `net.jkcode.jkmvc.http.fromRequest` method.

But the data in the request is string, and  the model's property may not be string, so `net.jkcode.jkmvc.http.fromRequest` will intelligently convert the value in the request, and assigned to the model's property.

```	
import net.jkcode.jkmvc.http.fromRequest

try
{
    val user = UserModel(20);
    val expected = listOf("username","password")
    user.fromRequest(req, expected)
    user.update();
}
catch (e: OrmException)
{
    // Handle errors ...
}
```

[!!] Although the second argument is optional, it is *highly recommended* to specify the list of columns you expect to change. Not doing so will leave your code _vulnerable_ in case the attacker adds fields you didn't expect.

