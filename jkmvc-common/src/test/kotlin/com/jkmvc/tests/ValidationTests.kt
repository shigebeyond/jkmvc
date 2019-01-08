package com.jkmvc.tests

import com.jkmvc.validator.RuleValidator
import org.junit.Test

class ValidationTests{

    @Test
    fun testValidate(){
        val exp = RuleValidator("testValidate", "min(1)");
        val result = exp.validate("3");
        println(result)
    }

    @Test
    fun testTransform(){
        val exp = RuleValidator("testValidate", "trim toUpperCase substring(2,-1)");
        val result = exp.validate(" model ");
        println(result)
    }
}