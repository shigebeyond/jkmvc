package com.jkmvc.tests

import com.jkmvc.util.ModelGenerator
import org.junit.Test


class ExampleTests {

    @Test
    fun testCodeModel() {
        val generator = ModelGenerator("/home/shi/code/java/jkmvc/jkmvc-example/src/main/kotlin" /* 源码目录 */, "default" /* 数据库名 */)
        generator.genenateModelFile("com.jkmvc.example.model" /* 模型包 */, "UserModel" /* 模型类名 */, "用户模型" /* 模型名 */, "user" /* 表名 */)
    }
}





