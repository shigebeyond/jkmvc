package com.jkmvc.tests

import com.jkmvc.db.Db
import com.jkmvc.db.DbQueryBuilder
import com.jkmvc.db.Record
import com.jkmvc.tests.UserModel
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.jvm.jvmName

open class A() {}
class B():A() {}

fun A.echo(){
    println("hi, I'm A")
}

fun B.echo(){
    println("hi, I'm B")
}

class MyTests{

  /*  @Test
    fun testClassName(){
        println(MyTests::class.qualifiedName)
        println(Int::class.qualifiedName)
        println(String::class.qualifiedName)
    }

    @Test
    fun testMap(){
        val a:A = A()
        a.echo() // A
        val b:B = B()
        b.echo() // B
        val c:A = B()
        c.echo() // A
    }*/

    @Test
    fun testMeta(){
        println(UserModel.m::class.jvmName) // com.jkmvc.tests.UserModel$m
        println(UserModel.m::class.qualifiedName) // com.jkmvc.tests.UserModel.m -- dot
        println(UserModel.m::class.simpleName) // m
        //        println(UserModel.m::class.java.name) // com.jkmvc.tests.UserModel$m
//        println(UserModel.m::class.java.typeName) // com.jkmvc.tests.UserModel$m
//        println(UserModel.m::class.java.canonicalName) // com.jkmvc.tests.UserModel.m -- dot
//        println(UserModel.m::class.java.simpleName) // m
    }
}



