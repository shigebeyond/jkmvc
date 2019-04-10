package net.jkcode.jkmvc.common

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 结果的持有者
 */
class ResultHolder{

    /**
     * 结果值
     */
    public var value: Any? = null

    /**
     * 等待数
     */
    public val waiterNum: AtomicInteger = AtomicInteger(0)
}

/**
 * 针对每个key合并操作
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-10 9:47 AM
 */
class KeyCombiner {

    /**
     * <键 to 结果>
     */
    protected val resultHolders: ConcurrentHashMap<Any, ResultHolder> by lazy {
        ConcurrentHashMap<Any, ResultHolder>()
    }

    /**
     * 针对当前key合并操作
     * @param key
     * @param action
     * @return
     */
    public fun <T> combine(key: Any, action: () -> T): T {
        val holder = resultHolders.getOrPutOnce(key){
            ResultHolder()
        }
        if(holder.waiterNum.getAndIncrement() > 0 && holder.value != null)
            return holder.value as T

        return synchronized(holder){
            try{
                if(holder.value == null)
                    holder.value = action()
                holder.value as T
            }finally {
                if(holder.waiterNum.decrementAndGet() == 0)
                    holder.value = null
            }
        }
    }
}