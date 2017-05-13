package com.jkmvc.example.model

import com.jkmvc.orm.MetaData
import com.jkmvc.orm.Orm

/**
 * 用户模型
 */
class UserModel(id:Int? = null): Orm(id) {
    // 伴随用户就是元数据
    companion object m: MetaData(UserModel::class){
        init {
            // 标签 + 规则
            addRule("name", "姓名", "notEmpty");
            addRule("age", "年龄", "between(1,120)");

            // 关联关系
            hasOne("address", AddressModel::class)
            hasMany("addresses", AddressModel::class)
        }
    }

    public var id:Int by property<Int>();

    public var name:String by property<String>();

    public var age:Int by property<Int>();

    // 关联地址：一个用户有一个地址
    public var address:AddressModel by property<AddressModel>();

    // 关联地址：一个用户有多个地址
    public var addresses:List<AddressModel> by property<List<AddressModel>>();
}
