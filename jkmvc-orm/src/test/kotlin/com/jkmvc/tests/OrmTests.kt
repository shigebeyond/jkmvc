package com.jkmvc.tests

import com.jkmvc.db.Db
import com.jkmvc.orm.*
import org.junit.Test
import java.util.HashMap
import kotlin.reflect.jvm.jvmName

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
            addRule("name", "姓名", "notEmpty");
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
    public var id:Int by property<Int>();

    public var name:String by property<String>();

    public var age:Int by property<Int>();

    public var avatar:String? by property<String?>();

    // 关联地址：一个用户有一个地址
    // relate to AddressModel: user has an address
    public var address:AddressModel by property<AddressModel>();

    // 关联地址：一个用户有多个地址
    // relate to AddressModel: user has many addresses
    public var addresses:List<AddressModel> by property<List<AddressModel>>();

    public var parcelSenders:List<UserModel> by property();

    public var parcelReceivers:List<UserModel> by property();
}

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
        /*public override val rules: MutableMap<String, IRuleMeta> = hashMapOf(
                "userId" to RuleMeta("用户", "notEmpty"),
                "age" to RuleMeta( "年龄", "between(1,120)")
        )*/
    }

    // 代理属性读写
    // delegate property
    public var id:Int by property<Int>();

    public var userId:Int by property<Int>();

    public var addr:String by property<String>();

    public var tel:String by property<String>();

    // 关联用户：一个地址从属于一个用户
    public var user:UserModel by property<UserModel>()
}

/**
 * 包裹模型
 *
 * @ClassName: ParcelModel
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-11-24 09:42:34
 */
class ParcelModel(id:Int? = null): Orm(id) {
    // 伴随对象就是元数据
    companion object m: OrmMeta(ParcelModel::class, "包裹模型", "parcel", "id"){

        init {
            // 添加标签 + 规则
            // add label and rule for field
            addRule("sender_id", "寄件人", "notEmpty");
            addRule("receiver_id", "收件人", "notEmpty");
            addRule("content", "内容", "notEmpty");

            // 添加关联关系
            // add relaction for other model
            belongsTo("sender", UserModel::class, "sender_id") // 寄件人
            belongsTo("receiver", UserModel::class, "receiver_id") // 收件人
        }

    }

    // 代理属性读写
    public var id:Int by property() // 包裹id

    public var senderId:Int by property() // 寄件人id

    public var receiverId:Int by property() // 收件人id

    public var content:String by property() // 寄件内容

}

class OrmTests{

    val id: Int by lazy {
        val minId = Db.instance().queryInt("select id from user order by id limit 1")!!
        println("随便选个id: " + minId)
        minId
    }

    @Test
    fun testMeta(){
//        println(UserModel.m::class.jvmName) // com.jkmvc.tests.MyTests$m
//        println(UserModel.m::class.qualifiedName) // com.jkmvc.tests.UserModel.m -- dot
//        println(UserModel.m::class.simpleName) // m
        println(UserModel.m::name) // m
        println(UserModel.m.name) // user
        println(UserModel::class.modelOrmMeta.name) // user
//        println(UserModel.m::class.java.name) // com.jkmvc.tests.MyTests$m
//        println(UserModel.m::class.java.typeName) // com.jkmvc.tests.MyTests$m
//        println(UserModel.m::class.java.canonicalName) // com.jkmvc.tests.UserModel.m -- dot
//        println(UserModel.m::class.java.simpleName) // m
    }

    @Test
    fun testFind(){
//        val user = UserModel.queryBuilder().where("id", 1).find<UserModel>()
        val user = UserModel(id)
        println("查找用户: $user" )
    }

    @Test
    fun testFindAll(){
//        val users = UserModel.queryBuilder().findAll<UserModel>()
//        val users = UserModel.queryBuilder().where("id", "=", 6).findAll<UserModel>()
//        val users = UserModel.queryBuilder().where("id", "IN", arrayOf(6, 7)).findAll<UserModel>()
        val users = UserModel.queryBuilder(true).where("id", "IN", arrayOf("6", "7")).findAll<UserModel>()
        println("查找所有用户: $users" )
    }

    @Test
    fun testCreate(){
        val user = UserModel()
        user.name = "shi";
        user.age = 12
        val id = user.create();

        println("创建用户: $user")
    }

    @Test
    fun testUpdate(){
        val user = UserModel.queryBuilder().where("id", id).find<UserModel>()
        if(!user.isLoaded()){
            println("用户[$id]不存在")
            return
        }
        user!!.name = "li";
        user!!.age = 13;
        user!!.update();
        println("更新用户：$user")
    }

    @Test
    fun testDelete(){
        val user = UserModel.queryBuilder().where("id", id).find<UserModel>()
        if(user == null || !user.isLoaded()){
            println("用户[$id]不存在")
            return
        }
        println("删除用户：$user, result: ${user.delete()}")
    }

    @Test
    fun testIncr(){
        val user = UserModel.queryBuilder().where("id", id).find<UserModel>()
        if(user == null || !user.isLoaded()){
            println("用户[$id]不存在")
            return
        }
        println("用户年龄+1：$user, result: ${user.incr("age", 1)}")
    }

    @Test
    fun testRelateFind(){
        val user = UserModel(id)
        val address = user.address
        println(address)
    }

    @Test
    fun testRelateFindMany(){
        // 一个user，联查多个address
//        val user = UserModel(id)
//        val addresses = user.addresses
//        println(addresses)

        // 多个user，联查多个address
        var users = UserModel.queryBuilder().with("addresses").limit(100).findAll<UserModel>()
        for (user in users){
            println("user[${user.id}]: ${user.name}")
            for(address in user.addresses){
                println(" ---- address[${address.id}] : ${address.addr}")
            }
        }
    }

    @Test
    fun testRelateCreate(){
        val user = UserModel(id)
        val address = AddressModel()
        address.addr = "nanning"
//        address.tel = "110a" // wrong
        address.tel = "110"
        address.user = user;
        address.create()
        println("创建地址: $address")
    }

    @Test
    fun testRelateUpdate(){
        val user = UserModel(id)
        val address = user.address
        if(!address.isLoaded()){
            println("用户[$user]没有地址")
            return
        }

        address.addr = "gx"
        address.tel = "119"
        address.update()
        println("更新地址: $address")
    }

    @Test
    fun testRelateDelete(){
        val user = UserModel(id)
        val address = user.address
        if(!address.isLoaded()){
            println("用户[$user]没有地址")
            return
        }

        println("删除用户：$address, result: ${address.delete()}")
    }

    @Test
    fun testRelationManage(){
        // 有几个关系
        val user = UserModel(id)
        val count = user.countRelation("addresses")
        println("用户[${user.name}]有 $count 个地址")

        // 删除关系： 清空外键，不删除关联对象
//        val bool = user.removeRelations("addresses", 0)
//        println("用户[${user.name}] 删除地址的关系")

        // 删除关联对象
        val bool = user.deleteRelated("addresses")
        println("用户[${user.name}] 删除地址的关联对象")
    }

    @Test
    fun testMiddleRelationManage(){
        val users = UserModel.queryBuilder().limit(2).findAll<UserModel>()
        val (u1, u2) = users

        println("${u1.id} 给 ${u2.id} 寄快递")
        u1.addRelation("parcelReceivers", u2)
        println("${u1.id} 的收件人 " + u1.parcelReceivers.collectColumn("id"))
        println("${u2.id} 的寄件人 " + u2.parcelSenders.collectColumn("id"))

        u2.removeRelations("parcelSenders", u1)
        println("${u1.id} 有收件人${u2.id}? " + u1.hasRelation("parcelReceivers", u2))
        println("${u2.id} 有寄件人${u1.id}? " + u2.hasRelation("parcelSenders", u1))
    }

}



