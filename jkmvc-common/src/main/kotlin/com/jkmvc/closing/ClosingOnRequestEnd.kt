package com.jkmvc.closing

import java.io.Closeable
import java.util.*

/**
 * 请求结束时要关闭的资源
 *
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-12-16 3:48 PM
 */
abstract class ClosingOnRequestEnd : Closeable {

    companion object {

        /**
         * 记录要关闭的资源
         */
        protected val closings: MutableList<ClosingOnRequestEnd> = Vector()

        /**
         * 添加要关闭的资源
         * @param c
         */
        public fun addClosing(c: ClosingOnRequestEnd){
            closings.add(c)
        }

        /**
         * 触发资源关闭
         *    在请求结束时调用, 如http请求/rpc请求
         */
        public fun triggerClosings() {
            println("--- Request End --- ")
            for (c in closings) {
                // 关闭资源
                c.close()
                // 清空记录
                if(c.once)
                    closings.remove(c)
            }

        }
    }

    /**
     * 是否只触发一次, 触发后就清空记录
     */
    protected val once: Boolean = false

    init {
        addClosing(this)
    }
}