package com.jkmvc.tests

import com.jkmvc.validator.Validator
import com.jkmvc.validator.ValidatorExpr
import org.junit.Test

class ValidationTests{

    @Test
    fun testValidate(){
        val exp = ValidatorExpr("min(1)");
        val result = exp.execute("3");
        println(result)
    }

    @Test
    fun testTransform(){
//        val exp = ValidationExpr("trim . toUpperCase . substring(2,-1)");
//        val (result) = exp.execute(" model ");

        val result = Validator.execute("trim . toUpperCase . substring(2,-1)", " model ");
        println(result)
    }
}