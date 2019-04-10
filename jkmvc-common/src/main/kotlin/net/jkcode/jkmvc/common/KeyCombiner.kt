package net.jkcode.jkmvc.common

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 值的持有者
 */
class ValueHolder{

    /**
     * 值
     */
    public var value: Any? = null

    /**
     * 等待人数
     */
    public val waiterNum: AtomicInteger = AtomicInteger(0)
}

/**
 * 请求合并者
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-10 9:47 AM
 */
class MultiKeyCombiner {

    /**
     * <键 to 值>
     */
    protected val valueHolders: ConcurrentHashMap<Any, ValueHolder> by lazy {
        ConcurrentHashMap<Any, ValueHolder>()
    }

    /**
     * 等待加锁, 锁不住要等待
     * @param key
     * @param block
     * @return
     */
    public fun <T> waitLock(key: Any, block: () -> T): T {
        val holder = valueHolders.getOrPutOnce(key){
            ValueHolder()
        }
        if(holder.waiterNum.getAndIncrement() > 0 && holder.value != null)
            return holder.value as T

        return synchronized(holder){
            try{
                if(holder.value == null)
                    holder.value = block()
                holder.value as T
            }finally {
                if(holder.waiterNum.decrementAndGet() == 0)
                    holder.value = null
            }
        }
    }
}