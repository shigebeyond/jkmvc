package net.jkcode.jkmvc.ttl

import java.util.*

/**
 * 值, 有作用域的可传递的 ThreadLocal 中的值
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-17 8:52 AM
 */
class SttlValue(public var value: Any? = null) {

    /**
     * 被传递的线程
     */
    public val threads: MutableList<Thread> = LinkedList()

    init {
        // 添加创建的线程
        addThread(Thread.currentThread())
    }

    /**
     * 添加被传递的线程
     */
    public fun addThread(t: Thread = Thread.currentThread()){
        //println("addThread: " + t.name)
        threads.add(t)
    }

    /**
     * 删除被传递线程
     */
    public fun removeThread(t: Thread = Thread.currentThread()) {
        //println("removeThread: " + t.name)
        threads.remove(t)
    }

    /**
     * 清空所有被传递的线程
     */
    public fun clearThreads(){
        threads.clear()
    }

}