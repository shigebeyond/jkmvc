package com.jkmvc.example.model

import com.jkmvc.orm.MetaData
import com.jkmvc.orm.Orm

/**
 * 地址模型
 */
class AddressModel(id:Int? = null): Orm(id) {
    // 伴随对象就是元数据
    // company object is meta data for model
    companion object m: MetaData(AddressModel::class){
        init {
            // 添加标签 + 规则
            // add label and rule for field
            addRule("user_id", "用户", "notEmpty");
            addRule("addr", "地址", "notEmpty");
            addRule("tel", "电话", "notEmpty && digit");

            // 添加关联关系
            // add relaction for other model
            belongsTo("user", UserModel::class, "user_id")
        }
    }

    // 代理属性读写
    // delegate property
    public var id:Int by property<Int>();

    public var user_id:Int by property<Int>();

    public var addr:String by property<String>();

    public var tel:String by property<String>();

    // 关联用户：一个地址从属于一个用户
    public var user:UserModel by property<UserModel>()
}