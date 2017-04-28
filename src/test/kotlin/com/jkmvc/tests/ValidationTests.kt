package com.jkmvc.tests

import org.junit.Test

class ValidationTests{


    @Test
    fun testValidate(){
        val exp = ValidationExpression("trim . toUpperCase . substring(2)");
        val (value, message) = exp.execute(" model ");
        println(value)
    }
}