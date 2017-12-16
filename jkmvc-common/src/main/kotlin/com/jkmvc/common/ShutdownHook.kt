package com.jkmvc.common

import java.io.Closeable

/**
 * 程序退出事件钩子
 *
 * @ClassName: ShutdownHook
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-12-16 3:48 PM
 */
object ShutdownHook : IShutdownHook {

    /**
     * 要关闭的对象
     */
    private val closings: MutableList<Closeable> = ArrayList()

    init {
        // 添加程序退出事件钩子
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            public override fun run() {
                println("程序结束")
                // 关闭所有对象
                closeAll()
            }
        })
    }

    /**
     * 关闭所有对象
     */
    override fun closeAll() {
        for (c in closings) {
            c.close()
        }
    }

    /**
     * 添加要关闭的对象
     *
     * @param obj
     */
    override fun addClosing(obj: Closeable){
        closings.add(obj)
    }
}