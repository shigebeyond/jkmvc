package com.jkmvc.tests

import com.jkmvc.common.to
import com.jkmvc.validate.Validation
import org.junit.Test

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
    }

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

    @Test
    fun testFunc(){
        val f: Lambda = { it:String ->
            it
        } as Lambda
        println(f.javaClass)
        println(f.javaClass.kotlin)
        println(f is KFunction<*>) // false
        println(f is KCallable<*>) // false
        println(f is Lambda) // true
        println(f.javaClass.superclass) // class kotlin.jvm.internal.Lambda
        println(f.javaClass.superclass.superclass) // Object
    }

    @Test
    fun testMember(){
        // kotlin.reflect.jvm.internal.KotlinReflectionInternalError: Reflection on built-in Kotlin types is not yet fully supported. No metadata found for public open val length: kotlin.Int defined in kotlin.String
        /*
        for(m in  String::class.declaredFunctions)
            println(m.name)
        */
        for(m in String.javaClass.methods)
            println(m.name)
    }

    @Test
    fun testType(){
        val type = Validation::trim.parameters[0].type
        println(type::class)
        println(type.javaClass)
        println(type.classifier)
    }
*/

    @Test
    fun testTo(){
        println("123".to(Int::class))
        println("123.45".to(Float::class))
        println("123.4567".to(Double::class))
        println("true".to(Boolean::class))
    }

    @Test
    fun testPattern(){
        val reg = "^\\d+$".toRegex()
//        println(reg.matches("123"));
//        println(reg.matches("123#"));
        println("hello".endsWith("")); // true
    }
}



