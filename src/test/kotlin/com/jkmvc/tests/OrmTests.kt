package com.jkmvc.tests

import com.jkmvc.orm.MetaData
import com.jkmvc.orm.Orm
import com.jkmvc.orm.isLoaded
import org.junit.Test
import java.util.LinkedHashMap

/**
 * 用户模型
 */
class UserModel(id:Int? = null): Orm(id) {
    // 伴随用户就是元数据
    companion object m: MetaData(UserModel::class){
        init {
            // 规则
            addRule("name", "notEmpty");
            addRule("age", "between(1,120)");

            // 标签
            addLabel("name", "姓名")
            addLabel("age", "年龄")

            // 关联关系
            hasOne("address", AddressModel::class)
            hasMany("addresses", AddressModel::class)
        }
    }

    public var name:String by property<String>();

    public var age:Int by property<Int>();

    // 关联地址：一个用户有一个地址
    public var address:AddressModel by property<AddressModel>();

    // 关联地址：一个用户有多个地址
    public var addresses:List<AddressModel> by property<List<AddressModel>>();
}


/**
 * 地址模型
 */
class AddressModel(id:Int? = null): Orm(id) {
    // 伴随用户就是元数据
    companion object m: MetaData(AddressModel::class){
        init {
            belongsTo("user", UserModel::class, "user_id")
        }

        public override val rules: MutableMap<String, String> = mutableMapOf(
                "tel" to "digit"
        );

        /**
         * 每个字段的标签（中文名）
         * @var map
         */
        public override val labels: MutableMap<String, String>  = mutableMapOf(
                "user_id" to "用户",
                "addr" to "地址",
                "tel" to "电话"
        );
    }

    public var user_id:Int by property<Int>();

    public var addr:String by property<String>();

    public var tel:String by property<String>();

    // 关联用户：一个地址从属于一个用户
    public var user:UserModel by property<UserModel>()
}



class OrmTests{

    var id = 11;

    /*@Test
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
        id = user.create();

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
    }*/

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
        address.tel = "110a"
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



