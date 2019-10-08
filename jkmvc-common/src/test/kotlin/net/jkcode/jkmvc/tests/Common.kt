package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.common.cloneProperties
import net.jkcode.jkmvc.common.generateId

open class A() {
    open fun echo(){}
}
class B():A() {
    /*override fun echo(){
        println("Ah")
    }*/
}

fun A.sayHi(){
    println("hi, I'm A")
}

fun B.sayHi(){
    println("hi, I'm B")
}

enum class NumType {
    Byte,
    Short,
    INT,
    LONG
}

class Lambda {
}

data class Thing(val name: String, val weight: Int)


open class Man(var name: String, var age: Int): Cloneable{
    val id = generateId("man")

    /*public override fun clone(): Any {
        return super.clone()
    }*/

    override fun toString(): String {
        return "${javaClass.name}: id=$id, name=$name, age=$age"
    }
}

class Family(val master: Man, val members: List<Man>): Cloneable{

    var deepClone: Boolean = false

    public override fun clone(): Any {
        val o = super.clone()

        // 深克隆
        if(deepClone)
            o.cloneProperties("master", "members")
        return o
    }
}
