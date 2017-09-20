package com.jkmvc.example.model

import com.jkmvc.orm.MetaData
import com.jkmvc.orm.Orm
import com.jkmvc.session.Auth

/**
 * 用户模型
 * User　model
 */
class UserModel(id:Int? = null): Orm(id) {
    // 伴随对象就是元数据
    // company object is meta data for model
    companion object m: MetaData(UserModel::class){
        init {
            // 添加标签 + 规则
            // add label and rule for field
            addRule("username", "用户名", "notEmpty");
            addRule("password", "密码", "notEmpty");
            addRule("name", "姓名", "notEmpty");
            addRule("age", "年龄", "between(1,120)");

            // 添加关联关系
            // add relaction for other model
            hasOne("address", AddressModel::class)
            hasMany("addresses", AddressModel::class)
        }
    }

    // 代理属性读写
    // delegate property
    public var id:Int by property<Int>();

    public var username:String by property<String>();

    public var password:String by property<String>();

    public var name:String by property<String>();

    public var age:Int by property<Int>();

    public var avatar:String? by property<String?>();

    // 关联地址：一个用户有一个地址
    // relate to AddressModel: user has an address
    public var address:AddressModel by property<AddressModel>();

    // 关联地址：一个用户有多个地址
    // relate to AddressModel: user has many addresses
    public var addresses:List<AddressModel> by property<List<AddressModel>>();

    /**
     * create前置处理
     */
    public fun beforeCreate(){
        // 加密密码
        this["password"] = Auth.hash(this["password"])
    }
}
