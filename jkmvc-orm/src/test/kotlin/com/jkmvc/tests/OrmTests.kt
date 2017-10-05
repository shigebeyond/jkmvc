package com.jkmvc.tests

import com.jkmvc.db.Db
import com.jkmvc.orm.*
import org.junit.Test
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

class OrmTests{

    val id: Int by lazy {
        val (hasNext, minId) = Db.getDb().queryCell("select id from user order by id limit 1")
        println("随便选个id: " + minId)
        (minId as Long).toInt();
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
        val users = UserModel.queryBuilder().findAll<UserModel>()
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
    fun testRelateFind(){
        val user = UserModel(id)
        val address = user.address
        println(address)
    }

    @Test
    fun testRelateFindMany(){
        val user = UserModel(id)
        val addresses = user.addresses
        println(addresses)
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

}



