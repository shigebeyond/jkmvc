package com.jkmvc.common

/**
 * 程序退出事件的钩子，用于关闭资源
 *
 * @ClassName: ShutdownHook
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-12-16 3:48 PM
 */
object ShutdownHook : ClosingHook() {

    init {
        // 添加程序退出事件钩子
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            public override fun run() {
                println("程序结束")
                // 关闭资源
                closeAll()
            }
        })
    }
}