package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.server.JettyServer
import net.jkcode.jkmvc.util.ModelGenerator
import org.junit.Test


class ExampleTests {

    // right: 直接启动 JettyServerLauncher 类, 并设置 module 为 jkmvc-example_main
    @Test
    fun testJettyServer() {
        // wrong: 无法加载 jkmvc-example/src/main/webapp
        JettyServer().start()
    }

    @Test
    fun testCodeModel() {
        val generator = ModelGenerator("/home/shi/code/java/jkmvc/jkmvc-example/src/main/kotlin" /* 源码目录 */, "net.jkcode.jkmvc.example.model" /* 包路径 */, "default" /* 数据库名 */, "shijianhang" /* 作者 */)
        //generator.genenateModelFile("UserModel" /* 模型类名 */, "用户模型" /* 模型名 */, "user" /* 表名 */)
        //generator.genenateModelFile("AddressModel" /* 模型类名 */, "地址模型" /* 模型名 */, "address" /* 表名 */)
        generator.genenateModelFile("ParcelModel" /* 模型类名 */, "包裹模型" /* 模型名 */, "parcel" /* 表名 */)
    }
}





