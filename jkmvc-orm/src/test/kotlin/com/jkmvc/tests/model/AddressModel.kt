package net.jkcode.jkmvc.tests.model

import net.jkcode.jkmvc.orm.Orm
import net.jkcode.jkmvc.orm.OrmMeta

/**
 * 地址模型
 */
class AddressModel(id:Int? = null): Orm(id) {
    // 伴随对象就是元数据
    // company object is ormMeta data for model
    companion object m: OrmMeta(AddressModel::class){
        init {
            // 添加标签 + 规则
            // add label and rule for field
            addRule("userId", "用户", "notEmpty");
            addRule("addr", "地址", "notEmpty");
            addRule("tel", "电话", "notEmpty && digit");

            // 添加关联关系
            // add relaction for other model
            belongsTo("user", UserModel::class, "user_id")
        }

        // 重写规则
        /*public override val rules: MutableMap<String, IValidator> = hashMapOf(
                "userId" to RuleValidator("用户", "notEmpty"),
                "age" to RuleValidator( "年龄", "between(1,120)")
        )*/
    }

    // 代理属性读写
    // delegate property
    public var id:Int by property();

    public var userId:Int by property();

    public var addr:String by property();

    public var tel:String by property();

    // 关联用户：一个地址从属于一个用户
    public var user:UserModel by property()
}