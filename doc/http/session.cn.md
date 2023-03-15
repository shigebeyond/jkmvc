# 会话

## 会话配置

vim src/main/resources/session.properties

```
# 认证处理的类型: 1 session: 使用session来保存登录用户 2 token: 使用token来保存登录用户
authType = session
# token缓存方式, 仅在 authType = token 时有效
tokenCache = jedis
# 用户模型的类，必须是实现 IAuthUserModel
userModel = net.jkcode.jkmvc.example.model.UserModel
# 用户名字段
usernameField = username
# 密码字段
passwordField = password
# 加密的盐
salt = .$%^#*!)06zth
```

其中`userModel`是开发者自己实现的用户模型类, 表示当前会话中登录的用户

## UserModel实现
必须实现接口`IAuthUserModel`, 参考demo

```
class UserModel(id:Int? = null): Orm(id), IAuthUserModel {
	// 伴随对象就是元数据
 	companion object m: OrmMeta(UserModel::class, "用户模型", "user", "id"){}

	// 代理属性读写
	public var id:Int by property() // 用户编号

	public var username:String by property() // 用户名

	public var password:String by property() // 密码

	public var name:String by property() // 中文名

	public var age:Int by property() // 年龄

	public var avatar:String by property() // 头像

}
```