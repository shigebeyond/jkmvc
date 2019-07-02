package net.jkcode.jkmvc.example.model 

import net.jkcode.jkmvc.http.session.IAuthUserModel
import net.jkcode.jkmvc.orm.OrmMeta
import net.jkcode.jkmvc.orm.Orm

/**
 * 用户模型
 *
 * @ClassName: UserModel
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-09-29 6:56 PM
 */
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