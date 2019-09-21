package net.jkcode.jkmvc.ttl

/**
 * 当前持有者
 *    只是简单包装 ThreadLocal, 方便放到伴随对象中调用, 暴露简单明了的api
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-27 2:13 PM
 */
open class SttlCurrentHolder<T>(protected val holder:ScopedTransferableThreadLocal<T> /* 线程安全的对象缓存 */) {

    /**
     * 获得当前者
     * @return
     */
    public fun currentOrNull(): T?{
        return holder.get(false) // 不初始化
    }

    /**
     * 获得当前者
     * @return
     */
    public fun current(): T{
        return holder.get(true) // 初始化, 仅当 holder.supplier 不为空才能初始化
    }

    /**
     * 设置当前者
     * @param obj
     */
    public fun setCurrent(obj: T){
        holder.set(obj)
    }
}