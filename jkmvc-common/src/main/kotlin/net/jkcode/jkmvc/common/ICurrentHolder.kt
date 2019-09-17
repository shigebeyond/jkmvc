package net.jkcode.jkmvc.common

import java.util.function.Supplier

/**
 * 当前持有者
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-27 2:13 PM
 */
abstract class ICurrentHolder<T>(supplier: (()->T)? = null) {

    /**
     * 线程安全的对象缓存
     */
    protected val holder:ThreadLocal<T> =
            if(supplier == null)
                ThreadLocal()
            else
                ThreadLocal.withInitial { supplier.invoke() }

    /**
     * 获得当前者
     * @return
     */
    public fun currentOrNull(): T?{
        return holder.get()
    }

    /**
     * 获得当前者
     * @return
     */
    public fun current(): T{
        return currentOrNull()!!
    }

    /**
     * 设置当前者
     * @param obj
     */
    public fun setCurrent(obj: T){
        holder.set(obj)
    }

    /**
     * 删除当前者
     */
    public fun removeCurrent(){
        holder.remove()
    }
}