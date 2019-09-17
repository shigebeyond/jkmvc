package net.jkcode.jkmvc.ttl

import java.util.*

/**
 * 本地值映射: <ThreadLocal, 值>
 */
typealias Local2Value = HashMap<ScopedTransferableThreadLocal<*>, SttlValue>

/**
 * 有作用域的可传递的 ThreadLocal
 *    1. 实现 Scopable 接口, 标识有作用域, 保证值的创建与删除无误
 *    1.1 在作用域开始时创建, 保证多线程切换作用域时不污染新的作用域
 *    1.2 在作用域开始时删除, 防止内存泄露
 *    1.3 Scopable的 begin()/end() 必须保证被调用
 *    2. 切换线程时传输, 参考 SttlInterceptor
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-17 8:32 AM
 */
class ScopedTransferableThreadLocal<T>(public val supplier: (()->T)? = null): Scoped {

    companion object{
        /**
         * 线程安全的本地值映射
         *   本地值映射: <ThreadLocal, 值>
         */
        protected val local2Values: JkThreadLocal<Local2Value> = JkThreadLocal() {
            Local2Value()
        }

        /**
         * 获得当前线程线的本地值映射
         *    本地值映射: <ThreadLocal, 值>
         *
         * @return
         */
        public fun getLocal2Value(): Local2Value {
            return local2Values.get()
        }

        /**
         * 设置当前线程线的本地值映射
         *    本地值映射: <ThreadLocal, 值>
         *
         * @param local2Value
         */
        public fun setLocal2Value(local2Value: Local2Value) {
            return local2Values.set(local2Value)
        }

    }

    /**
     * 设置值
     * @param value
     */
    public fun set(value: T) {
        val local2Value = local2Values.get()
        // 有旧值则删除当前线程
        val oldValue = local2Value.get(this)
        if(oldValue != null)
            oldValue.removeThread()

        // 设置新值
        local2Value.put(this, SttlValue(value))
    }

    /**
     * 获得值
     * @return
     */
    public fun get(): T {
        return local2Values.get().getOrPut(this){
            SttlValue(supplier?.invoke())
        }.value as T
    }

    /**
     * 作用域开始
     *   开始新值, 如果有旧值, 就删掉
     */
    public override fun begin() {
        // 删除当前线程的旧值
        val v = local2Values.get().remove(this)
        // 旧值也删除当前线程
        v?.removeThread()
    }

    /**
     * 作用域结束
     *    删除值, 要删除所有被传递的线程的值
     */
    public override fun end() {
        // 删除所有被传递的线程的值
        // 获得值
        val value = local2Values.get().get(this)
        if(value == null)
            return

        // 遍历每个被传递线程来删除值
        for(t in value.threads){
            // 获得该线程的值
            val local2Value = local2Values.get(t)
            if(local2Value == null)
                continue
            val value2 = local2Value.get(this)

            // 如果值没有改变, 则删除
            if(value2 == value)
                local2Value.remove(this)
        }
        // 值也删除所有线程
        value.clearThreads()
    }

}