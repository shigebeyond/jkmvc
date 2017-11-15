package com.jkmvc.tests

import com.jkmvc.util.ModelGenerator
import org.junit.Test


class ExampleTests {

    @Test
    fun testCodeModel() {
        val generator = ModelGenerator("/home/shi/code/java/jkmvc/jkmvc-example/src/main/kotlin")
        generator.genenateModelFile("com.jkmvc.example.model", "UserModel", "用户模型", "user")
    }
}





