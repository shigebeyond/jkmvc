package net.jkcode.jkmvc.example.server

import net.jkcode.jkmvc.server.JettyServer

/**
 * jetty server启动器
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-29 5:01 PM
 */
object JettyServerLaunch {

    @JvmStatic
    fun main(args: Array<String>) {
        JettyServer().start()
    }

}