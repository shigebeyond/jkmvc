package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.db.Db
import net.jkcode.jkmvc.model.GeneralModel
import net.jkcode.jkmvc.orm.*
import net.jkcode.jkmvc.query.DbExpr
import net.jkcode.jkmvc.tests.model.*
import net.jkcode.jkutil.common.getConstructorOrNull
import org.junit.Test

class OrmTests{

    val id: Int by lazy {
        val minId = Db.instance().queryValue<Int>("select id from user order by id limit 1")!!
        println("随便选个id: " + minId)
        minId
    }

    @Test
    fun testMeta(){
//        println(UserModel.m::class.jvmName) // net.jkcode.jkmvc.tests.MyTests$m
//        println(UserModel.m::class.qualifiedName) // net.jkcode.jkmvc.tests.UserModel.m -- dot
//        println(UserModel.m::class.simpleName) // m
        println(UserModel.m::name) // m
        println(UserModel.m.name) // user
        println(UserModel::class.modelOrmMeta.name) // user
//        println(UserModel.m::class.java.name) // net.jkcode.jkmvc.tests.MyTests$m
//        println(UserModel.m::class.java.typeName) // net.jkcode.jkmvc.tests.MyTests$m
//        println(UserModel.m::class.java.canonicalName) // net.jkcode.jkmvc.tests.UserModel.m -- dot
//        println(UserModel.m::class.java.simpleName) // m
    }

    @Test
    fun testFind(){
        val user = UserModel.findByPk<UserModel>(id)
//        val user = UserModel.queryBuilder().where("id", id).findModel<UserModel>()
//        val user = UserModel(id)
        println("查找用户: $user" )
    }

    @Test
    fun testFindAll(){
//        val users = UserModel.queryBuilder().findModels<UserModel>()
//        val users = UserModel.queryBuilder().where("id", "=", 6).findModels<UserModel>()
//        val users = UserModel.queryBuilder().where("id", "IN", arrayOf(6, 7)).findModels<UserModel>()
//        val users = UserModel.queryBuilder(true).where("id", "IN", arrayOf("6", "100")).findModels<UserModel>()
        val users = UserModel.queryBuilder()
                .where("age", ">=", 20)
                .orderBy("age")
                .findModels<UserModel>()
        println("查找所有用户: $users" )
    }

    @Test
    fun testCreate(){
        val user = UserModel()
        user.name = "shi";
        user.age = 12
        //user.fromMap(mapOf("name" to "li", "age" to 13))
        val id = user.create();
        println("创建用户[$id]: $user")
    }

    @Test
    fun testUpdate(){
        val user = UserModel.queryBuilder().where("id", id).findModel<UserModel>()
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
        val user = UserModel.queryBuilder().where("id", id).findModel<UserModel>()
        if(user == null || !user.isLoaded()){
            println("用户[$id]不存在")
            return
        }
        println("删除用户：$user, result: ${user.delete()}")
    }

    @Test
    fun testClone(){
        val query = UserModel.queryBuilder().where("id", id).limit(1)
        val query2 = query.copy()
        val user = query.findModel<UserModel>()
        query2.set("age", 0).update()
    }

    @Test
    fun testGeneralModelConstructor(){
        val c = GeneralModel::class.java.getConstructorOrNull() // 无参数构造函数
        println(c)
    }

    @Test
    fun testGeneralModel(){
        // 插入
        val table = "user"
        val primaryKey = "id"
        val user1 = GeneralModel(table, primaryKey)
        user1["name"] = "tom"
        user1["age"] = 13
        user1.create()
        val id:Int = user1[primaryKey]
        println("插入用户: $user1")

        // 查询
        val user2 = GeneralModel(table, primaryKey)
        user2.loadByPk(id)
        if(!user2.loaded)
            println("用户[$id]不存在")
        else
            println("查询用户: $user2")

        // 更新
        user2["age"] = 23
        user2.update()
        println("更新用户: $user2")

        // 删除
        user2.delete()
        println("删除用户: $id")

        // query builder
        val users = GeneralModel(table, primaryKey).queryBuilder().limit(2).findModels<GeneralModel>()
        println(users)
    }

    @Test
    fun testIncr(){
        val user = UserModel.queryBuilder().where("id", id).findModel<UserModel>()
        if(user == null || !user.isLoaded()){
            println("用户[$id]不存在")
            return
        }
        println("用户年龄+1：$user, result: ${user.incr("age", 1)}")
    }

    @Test
    fun testRelateFind(){
        // 1 延迟加载
        //val user = UserModel(id)
        // 2 with() 联查
        val user = UserModel.queryBuilder()
                .with("home")
                .with("addresses")
                .where("user.id", id)
                .findModel<UserModel>()
        // 3 selectWiths() 联查
        //val user = UserModel.queryBuilder().selectWiths("home" to listOf("*")).where("user.id", id).findModel<UserModel>()
        // 4 selectWiths() 联查 + 表别名
        //val user = UserModel.queryBuilder().selectWiths(DbExpr("home", "a") to listOf("*")).where("user.id", id).findModel<UserModel>()
        println(user?.home)
        println(user?.addresses)
    }

    @Test
    fun testRelateFindMany(){
        // 一个user，联查个address
//        val user = UserModel(id)
//        val addresses = user.addresses
//        println(addresses)

        // 多个user，联查多个address
        val query = UserModel.queryBuilder().with("addresses"){ query2, lazy ->
            // 动态操作查询对象, 如添加条件
            query2.where("tel", "110").orderBy("addr")
        }
        var users = query.limit(100).findModels<UserModel>()
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
        address.isHome = 1;
        address.create()
        println("创建地址: $address")
    }

    @Test
    fun testRelateUpdate(){
        val user = UserModel(id)
        val address = user.home
        if(!address.isLoaded()){
            println("用户[$user]没有地址")
            return
        }

        address!!.addr = "gx"
        address.tel = "119"
        address.update()
        println("更新地址: $address")
    }

    @Test
    fun testRelateDelete(){
        val user = UserModel(id)
        val address = user.home
        if(!address.isLoaded()){
            println("用户[$user]没有地址")
            return
        }

        println("删除用户：$address, result: ${address!!.delete()}")
    }

    @Test
    fun testRelationManage(){
        // 有几个关系
        val user = UserModel(id)
        val oldAddress = user.home
        var count = user.countRelation("addresses")
        println("用户[${user.name}]有 $count 个地址")

        // 删除关系： 清空外键，不删除关联对象
        val bool = user.removeRelations("addresses")
        println("用户[${user.name}] 删除地址的关系")

        // 添加关系
        if(oldAddress != null)
            user.addRelation("addresses", oldAddress.id)
        count = user.countRelation("addresses")
        println("用户[${user.name}]有 $count 个地址")

        // 删除关联对象
//        val bool = user.deleteRelated("addresses")
//        println("用户[${user.name}] 删除地址的关联对象")
    }

    @Test
    fun testMiddleRelationManage(){
        val users = UserModel.queryBuilder().limit(2).findModels<UserModel>()
        val (u1, u2) = users

        println("${u1.id} 给 ${u2.id} 寄快递")
        u1.addRelation("parcelReceivers", u2)
        println("${u1.id} 的收件人 " + u1.parcelReceivers.collectColumn("id"))
        println("${u2.id} 的寄件人 " + u2.parcelSenders.collectColumn("id"))

        //u2.removeRelations("parcelSenders", u1)
        u2.deleteRelated("parcelSenders", u1)
        println("${u1.id} 有收件人${u2.id}? " + u1.hasRelation("parcelReceivers", u2))
        println("${u2.id} 有寄件人${u1.id}? " + u2.hasRelation("parcelSenders", u1))
    }

    @Test
    fun testXstream() {
        val user = UserModel()
        user.name = "shi";
        user.age = 12

        val address = AddressModel()
        address.addr = "nanning"
        address.tel = "110"
        user.home = address // 关联对象引用

        val xstream = UserModel.initXStream()
        // 自定义转换器
        //xstream.registerConverter(OrmConverter(xstream))

        val xml = xstream.toXML(user)
        println("序列化到XML:\n$xml")

        val obj = xstream.fromXML(xml)
        println("反序列化Bean:\n$obj")
    }

}



