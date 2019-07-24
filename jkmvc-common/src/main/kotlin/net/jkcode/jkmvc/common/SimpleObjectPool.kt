package net.jkcode.jkmvc.common

import org.apache.commons.pool2.BasePooledObjectFactory
import org.apache.commons.pool2.PooledObject
import org.apache.commons.pool2.impl.DefaultPooledObject
import org.apache.commons.pool2.impl.GenericObjectPool

/**
 * 使用lambda实现的对象池
 *    用来优化某些频繁调用的方法中的对象创建, 如ArrayList/HashMap/ConcurrentLinkedQueue对象的频繁创建, 而且这些对象自身也会占用大量的内存(如ArrayList/HashMap在操作大量元素时会创建大数组)
 *    使用对象池中的对象, 必须是先借对象 borrowObject() 后归还对象 returnObject(), 这一般在同一个方法中, 这带来一个限制:
 *    就是借来的对象不能溢出该方法, 如果对象溢出了(如作为方法的返回值被调用方使用), 则对象的控制权不在该方法中, 也就无法归还对象
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-23 11:03 AM
 */
class SimpleObjectPool<T>(maxTotal: Int = -1, factory: () -> T): GenericObjectPool<T>(PooledObjectLambdaFactory(factory)){

    companion object{

        /**
         * 所有对象属性
         */
        protected val allObjectsField = GenericObjectPool::class.java.getWritableFinalField("allObjects")

        /**
         * 空闲对象属性
         */
        protected val idleObjectsField = GenericObjectPool::class.java.getWritableFinalField("idleObjects")
    }

    init {
        setMaxTotal(maxTotal)
    }

    /**
     * 输出空闲对象
     */
    internal fun printIdleObjects(){
        val idleObjects = idleObjectsField.get(this) as Collection<PooledObject<T>>
        val msg = idleObjects.joinToString(", ", "idleObjects[" + idleObjects.size + "] = [", "]") {
            "" + System.identityHashCode(it.`object`) + " - " + it.`object`
        }
        println(msg)
    }

    /**
     * 输出所有对象
     */
    internal fun printAllObjects(){
        val allObjects = allObjectsField.get(this) as Map<*, PooledObject<T>>
        val msg = allObjects.values.joinToString(", ", "idleObjects[" + allObjects.size + "] = [", "]") {
            "" + System.identityHashCode(it.`object`) + " - " + it.`object`
        }
        println(msg)
    }

}

// 使用lambda实现的池化对象工厂
internal class PooledObjectLambdaFactory<T>(protected val lambda: () -> T): BasePooledObjectFactory<T>() {

    /**
     * 包装对象
     */
    public override fun wrap(obj: T): PooledObject<T> {
        return DefaultPooledObject<T>(obj)
    }

    /**
     * 创建对象
     */
    public override fun create(): T {
        return lambda.invoke()
    }

}