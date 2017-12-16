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
interface IShutdownHook {
    /**
     * 关闭所有对象
     */
    fun closeAll()

    /**
     * 添加要关闭的对象
     *
     * @param obj
     */
    fun addClosing(obj: Closeable)
}