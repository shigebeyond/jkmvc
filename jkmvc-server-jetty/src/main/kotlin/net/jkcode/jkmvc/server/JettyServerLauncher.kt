package net.jkcode.jkmvc.server

import net.jkcode.jkmvc.server.JettyServer

/**
 * jetty server启动器
 *
 * 启动时报错: `java.lang.NoClassDefFoundError: javax/servlet/ServletRequest`
 * 原因: gradle的 war 插件自动将 javax.servlet-api 弄成 providedCompile, 你就算在工程的build.gradle 改为 compile 也没用
 * fix: project structure -> modules -> 选中 JettyServerLauncher 应用的工程 -> depencies -> 选中 Gradle: javax.servlet:javax.servlet-api:3.1.0 包, 将 scop 由 provided 改为 compile
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-29 5:01 PM
 */
object JettyServerLauncher {

    @JvmStatic
    fun main(args: Array<String>) {
        JettyServer().start()
    }

}