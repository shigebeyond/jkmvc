package net.jkcode.jkmvc.ttl

import net.jkcode.jkmvc.common.pollEach
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

/**
 * 值, 有作用域的可传递的 ThreadLocal 中的值
 *    ScopedTransferableThreadLocal.endScope() 可能随时随地调用, 也就是说 SttlValue 随时可能被删除, 但可能某个线程调用了 SttlInterceptor.intercept(回调), 但此时回调还没触发, 也就是旧的 ScopedTransferableThreadLocal 对象还未恢复, 等恢复后引用的 SttlValue 却应该被删掉, 因此添加 deleted 属性来做是否已删除的判断
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-17 8:52 AM
 */
class SttlValue(public var value: Any? = null) {

    /**
     * 被传递的线程
     */
    private val threads: Queue<Thread> = LinkedBlockingQueue()

    /**
     * 是否已删除
     */
    @Volatile
    public var deleted: Boolean = false

    init {
        // 添加创建的线程
        addThread(Thread.currentThread())
    }

    /**
     * 添加被传递的线程
     */
    public fun addThread(t: Thread = Thread.currentThread()){
        if(!deleted)
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
     * 逐个出队被传递线程, 并访问
     * @param action 访问的回调
     * @return
     */
    public fun pollEachThread(action: (Thread) -> Unit){
        threads.pollEach(action)
    }

}