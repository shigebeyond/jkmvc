package com.jkmvc.common

import java.io.Closeable

/**
 * 要关闭资源的事件钩子
 *
 * @ClassName: ShutdownHook
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-12-16 3:48 PM
 */
abstract class ClosingHook : IClosingHook {

    /**
     * 要关闭的对象
     */
    protected val closings: MutableList<Closeable> = ArrayList()

    /**
     * 关闭所有对象
     */
    override fun closeAll() {
        for (c in ShutdownHook.closings)
            c.close()
        closings.clear()
    }

    /**
     * 添加要关闭的对象
     *
     * @param obj
     */
    override fun addClosing(obj: Closeable){
        closings.add(obj)
    }

    /**
     * 添加要关闭的对象
     *
     * @param objs
     */
    override fun addClosings(objs: Collection<Closeable>){
        closings.addAll(objs)
    }
}