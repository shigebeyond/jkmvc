package com.jkmvc.tests

import com.jkmvc.orm.MetaData
import com.jkmvc.orm.Orm
import com.jkmvc.orm.isLoaded
import org.junit.Test

class UserModel: Orm() {
    // 伴随用户就是元数据
    companion object m: MetaData(UserModel::class)

    public var name:String by m.property<String>();

    public var age:Int by m.property<Int>();
}

class OrmTests{

    var id = 1;

    @Test
    fun testFind(){
        val user = UserModel.queryBuilder().where("id", 1).find<UserModel>()
        println("查找用户: $user" )
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
}



