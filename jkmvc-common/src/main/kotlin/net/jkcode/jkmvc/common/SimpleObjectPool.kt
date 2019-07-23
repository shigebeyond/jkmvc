package net.jkcode.jkmvc.common

import org.apache.commons.pool2.BasePooledObjectFactory
import org.apache.commons.pool2.PooledObject
import org.apache.commons.pool2.impl.DefaultPooledObject
import org.apache.commons.pool2.impl.GenericObjectPool

/**
 * 使用lambda实现的对象池
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