package net.jkcode.jkmvc.tests.model

import net.jkcode.jkmvc.orm.Orm
import net.jkcode.jkmvc.orm.OrmMeta

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
    public var addresses:List<AddressModel> by listProperty();

    public var parcelSenders:List<UserModel> by listProperty();

    public var parcelReceivers:List<UserModel> by listProperty();

    /**
     * 处理create前置事件
     */
    public override fun beforeCreate(){
        println("处理 beforeCreate 事件")
    }

    /**
     * 处理create后置事件
     */
    /*public override fun afterCreate(){
        println("处理 afterCreate 事件")
    }*/

    /**
     * 处理update前置事件
     */
    public override fun beforeUpdate(){
        println("处理 beforeUpdate 事件")
    }

    /**
     * 处理update后置事件
     */
    public override fun afterUpdate(){
        println("处理 afterUpdate 事件")
    }

    /**
     * 处理save前置事件
     */
    /*public override fun beforeSave(){
        println("处理 beforeSave 事件")
    }*/

    /**
     * 处理save后置事件
     */
    public override fun afterSave(){
        println("处理 afterSave 事件")
    }

    /**
     * 处理delete前置事件
     */
    public override fun beforeDelete(){
        println("处理 beforeDelete 事件")
    }

    /**
     * 处理delete后置事件
     */
    public override fun afterDelete(){
        println("处理 afterDelete 事件")
    }
}