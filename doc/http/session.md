# Session

## Session configuration

vim src/main/resources/session.properties

```
# Auth type: 1 session: use session to store login user 2 token: use token to store user
authType = session
# Token cache type, only use when authType = token
tokenCache = jedis
# UserModel class，must implements IAuthUserModel
userModel = net.jkcode.jkmvc.example.model.UserModel
# username field name
usernameField = username
# password field name
passwordField = password
# password salt
salt = .$%^#*!)06zth
```

`userModel` is UserModel class, which represents the logined user, and you should define it yourself.

## UserModel class
must implements`IAuthUserModel`, there is a demo:

```
class UserModel(id:Int? = null): Orm(id), IAuthUserModel {
	// orm meta
 	companion object m: OrmMeta(UserModel::class, "User model", "user", "id"){}

	// delegate property access
	public var id:Int by property() // 用户编号

	public var username:String by property() // 用户名

	public var password:String by property() // 密码

	public var name:String by property() // 中文名

	public var age:Int by property() // 年龄

	public var avatar:String by property() // 头像

}
```