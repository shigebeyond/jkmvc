package com.jkmvc.tests

import com.jkmvc.orm.*
import org.junit.Test

/**
 * 用户模型
 */
class UserModel(id:Int? = null): Orm(id) {
    // 伴随用户就是元数据
    companion object m: MetaData(UserModel::class)

    public var name:String by m.property<String>();

    public var age:Int by m.property<Int>();

    // 关联地址：一个用户有一个地址
    public var address:AddressModel by m.relatedProperty<AddressModel>(RelationType.HAS_ONE, "user_id");
}

/**
 * 地址模型
 */
class AddressModel(id:Int? = null): Orm(id) {
    // 伴随用户就是元数据
    companion object m: MetaData(AddressModel::class)

    public var user_id:Int by m.property<Int>();

    public var addr:String by m.property<String>();

    public var tel:String by m.property<String>();

    // 关联用户：一个地址从属于一个用户
    public var user:UserModel by m.relatedProperty<UserModel>(RelationType.BELONGS_TO,  "user_id");
}



class OrmTests{

    var id = 10;
/*
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
    }
 */

    @Test
    fun testRelateFind(){
        val user = UserModel(id)
        val address = user.address
        println(address)
    }

    @Test
    fun testRelateCreate(){
        val user = UserModel(id)
        val address = AddressModel()
        address.addr = "nanning"
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



