package com.jkmvc.tests

import com.jkmvc.validate.ValidationExpression
import org.junit.Test

class ValidationTests{


    @Test
    fun testValidate(){
        val exp = ValidationExpression("trim . toUpperCase . substring(2,-1)");
        val (value, message) = exp.execute(" model ");
        println(value)
    }
}