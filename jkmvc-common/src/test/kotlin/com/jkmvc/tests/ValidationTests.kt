package com.jkmvc.tests

import com.jkmvc.validate.Validation
import com.jkmvc.validate.ValidationExpression
import org.junit.Test

class ValidationTests{

    @Test
    fun testValidate(){
        val exp = ValidationExpression("min(1)");
        val result = exp.execute("3");
        println(result)
    }

    @Test
    fun testTransform(){
//        val exp = ValidationExpression("trim . toUpperCase . substring(2,-1)");
//        val (result) = exp.execute(" model ");

        val result = Validation.execute("trim . toUpperCase . substring(2,-1)", " model ");
        println(result)
    }
}