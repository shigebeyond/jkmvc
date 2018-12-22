package com.jkmvc.tests

import com.jkmvc.validator.Validator
import com.jkmvc.validator.RuleValidator
import org.junit.Test

class ValidationTests{

    @Test
    fun testValidate(){
        val exp = RuleValidator.from("min(1)");
        val result = exp.execute("3");
        println(result)
    }

    @Test
    fun testTransform(){
        val result = Validator.validate("trim toUpperCase substring(2,-1)", " model ");
        println(result)
    }
}