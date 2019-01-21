package com.jkmvc.tests.model

import com.jkmvc.orm.Orm
import com.jkmvc.orm.OrmMeta
import com.jkmvc.tests.AddressModel
import com.jkmvc.tests.ParcelModel

/**
 * 用户模型
 * User　model
 */
class UserModel(id:Int? = null): Orm(id) {

    // 伴随对象就是元数据
    // company object is ormMeta data for model
    companion object m: OrmMeta(UserModel::class){
        init {
            // 添加标签 + 规则
            // add label and rule for field
            addRule("name", "姓名", "notEmpty")
            addRule("age", "年龄", "between(1,120)");

            // 添加关联关系
            // add relaction for other model
            hasOne("address", AddressModel::class)
            hasMany("addresses", AddressModel::class)

            hasMany("parcels", ParcelModel::class)
            hasManyThrough("parcelSenders", UserModel::class, "receiver_id"/*外键*/, "id"/*主键*/, "parcel", "sender_id"/*远端外键*/, "id"/*远端主键*/)
            hasManyThrough("parcelReceivers", UserModel::class, "sender_id"/*外键*/, "id"/*主键*/, "parcel", "receiver_id"/*远端外键*/, "id"/*远端主键*/)
        }
    }

    // 代理属性读写
    // delegate property
    public var id:Int by property();

    public var name:String by property();

    public var age:Int by property();

    public var avatar:String? by property();

    // 关联地址：一个用户有一个地址
    // relate to AddressModel: user has an address
    public var address: AddressModel by property();

    // 关联地址：一个用户有多个地址
    // relate to AddressModel: user has many addresses
    public var addresses:List<AddressModel> by property();

    public var parcelSenders:List<UserModel> by property();

    public var parcelReceivers:List<UserModel> by property();
}