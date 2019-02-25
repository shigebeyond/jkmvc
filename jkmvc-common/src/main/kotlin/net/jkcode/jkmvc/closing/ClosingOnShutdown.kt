package net.jkcode.jkmvc.closing

import java.io.Closeable
import java.util.*

/**
 * 关机时要关闭的资源
 *
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-12-16 3:48 PM
 */
abstract class ClosingOnShutdown : Closeable {

    companion object {

        /**
         * 记录要关闭的资源
         */
        protected val closings: MutableList<Closeable> = Vector()

        /**
         * 添加要关闭的资源
         * @param c
         */
        public fun addClosing(c: Closeable){
            closings.add(c)
        }

        init {
            // 添加关机事件钩子
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                public override fun run() {
                    println("--- Shutdown --- ")
                    // 关闭资源
                    for (c in closings)
                        c.close()
                    // 清空记录
                    closings.clear()
                }
            })
        }
    }

    init {
        addClosing(this)
    }
}