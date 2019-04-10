package net.jkcode.jkmvc.common

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 结果的持有者
 */
class ResultHolder<T>{

    /**
     * 结果值
     */
    public var value: T? = null

    /**
     * 等待数
     */
    public val waiterNum: AtomicInteger = AtomicInteger(0)
}

/**
 * 针对每个key的操作合并, 也等同于操作去重
 *    如请求合并/cache合并等
 *    实现就是改进 ConcurrentMap.getOrPutOnce(key, defaultValue), 补上在多个操作处理完后删除key, 以便下一个轮回
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-10 9:47 AM
 */
class KeyCombiner<T> {

    /**
     * <键 to 结果>
     */
    protected val resultHolders: ConcurrentHashMap<Any, ResultHolder<T>> = ConcurrentHashMap()

    /**
     * 针对当前key合并操作
     * @param key
     * @param defaultValue 操作
     * @return
     */
    public fun combine(key: Any, defaultValue: () -> T): T {
        val holder = resultHolders.getOrPut(key){
            ResultHolder()
        }
        try {
            // 增加引用: 防止结果被删除
            if (holder.waiterNum.getAndIncrement() > 0 && holder.value != null)
                return holder.value!!

            // 加锁执行操作, 并缓存结果
            return synchronized(holder) {
                    if (holder.value == null)
                        holder.value = defaultValue()
                    holder.value!!
            }
        } finally {
            // 在多个操作处理完后删除结果, 以便下一个轮回
            if (holder.waiterNum.decrementAndGet() == 0)
                resultHolders.remove(key)
        }
    }
}