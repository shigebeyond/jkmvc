package net.jkcode.jkmvc.ttl

import net.jkcode.jkmvc.scope.Scope
import java.util.*

/**
 * 本地值映射: <ThreadLocal, 值>
 */
typealias Local2Value = MutableMap<ScopedTransferableThreadLocal<*>, SttlValue>

/**
 * 有作用域的可传递的 ThreadLocal
 *    1. 实现 Scopable 接口, 标识有作用域, 保证值的创建与删除无误
 *    1.1 在作用域开始时创建, 保证多线程切换作用域时不污染新的作用域
 *    1.2 在作用域开始时删除, 防止内存泄露
 *    1.3 Scopable的 beginScope()/endScope() 必须保证被调用
 *
 *    2. 自动刷新与删除
 *    2.1 beginScope()中刷新
 *    2.2 endScope()中删除
 *
 *    3. 切换线程时传输, 参考 SttlInterceptor
 *
 *    4. 所有的get()/set()操作必须在作用域内执行, 也就是说必须先调用 beginScope()
 *
 *    5. 值的变动 vs 超过作用域的引用
 *       endScope() 可能随时随地调用, 也就是说 SttlValue 随时可能被删除, 但可能某个线程调用了 SttlInterceptor.intercept(回调), 但此时回调还没触发, 也就是旧的 ScopedTransferableThreadLocal 对象还未恢复, 等恢复后引用的 SttlValue 却应该被删掉, 因此添加 deleted 属性来做是否已删除的判断
 *       可能同一个线程读两次get(), 读的值不一样, 第一次不为null, 第二次为null, 具体以其他线程修改的时间或endScope()调用的时间为准
 *       不过我本意是将 endScope() 交给开发者来自行调用, 从而保证 endScope() 是最后调用的, 往后不会有后续引用了, 从而保证不会作用域逃逸
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-17 8:32 AM
 */
open class ScopedTransferableThreadLocal<T>(public val supplier: (()->T)? = null): Scope() {

    companion object{

        init {
            // 修改 CompletableFuture.asyncPool 属性为 ThreadLocalInheritableThreadPool.commonPool
            SttlThreadPool.applyCommonPoolToCompletableFuture()
        }

        /**
         * 线程安全的本地值映射
         *   本地值映射: <ThreadLocal, 值>
         */
        protected val local2Values: JkThreadLocal<Local2Value> = JkThreadLocal() {
            HashMap<ScopedTransferableThreadLocal<*>, SttlValue>()
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
         * 复制当前线程线的本地值映射
         *    本地值映射: <ThreadLocal, 值>
         *
         * @return
         */
        public fun weakCopyLocal2Value(): Local2Value {
            val map = WeakHashMap<ScopedTransferableThreadLocal<*>, SttlValue>()
            map.putAll(getLocal2Value())
            return map
        }

        /**
         * 设置当前线程线的本地值映射
         *    本地值映射: <ThreadLocal, 值>
         *    在传递/恢复本地值时使用
         *    endScope() 可能随时随地调用, 也就是说 SttlValue 随时可能被删除, 但可能某个线程调用了 SttlInterceptor.intercept(回调), 但此时回调还没触发, 也就是旧的 ScopedTransferableThreadLocal 对象还未恢复, 等恢复后引用的 SttlValue 却应该被删掉, 因此添加 deleted 属性来做是否已删除的判断
         *
         * @param local2Value
         */
        public fun putLocal2Value(local2Value: Local2Value) {
            //return local2Values.set(local2Value)
            val map = local2Values.get()
            map.clear()
            // map.putAll(local2Value)
            for((local, value) in local2Value)
                if(!value.deleted) // 未删除
                    map[local] = value
        }

    }

    /**
     * 设置值
     * @param value
     */
    public fun set(value: T) {
        val local2Value = local2Values.get()
        // 有旧值,则更新
        val oldValue: SttlValue? = local2Value.get(this)
        if(oldValue != null) {
            oldValue.value = value
            return
        }

        // 无旧值, 则添加新值
        local2Value.put(this, SttlValue(value))
    }

    /**
     * 获得值
     *    endScope() 可能随时随地调用, 也就是说 SttlValue 随时可能被删除, 但可能某个线程调用了 SttlInterceptor.intercept(回调), 但此时回调还没触发, 也就是旧的 ScopedTransferableThreadLocal 对象还未恢复, 等恢复后引用的 SttlValue 却应该被删掉, 因此添加 deleted 属性来做是否已删除的判断
     *    可能同一个线程读两次get(), 读的值不一样, 第一次不为null, 第二次为null, 具体以其他线程修改的时间或endScope()调用的时间为准
     * @param initOnNull 当值为null时初始化值
     * @return
     */
    public fun get(initOnNull: Boolean = true): T {
        val local2Value = local2Values.get()
        var value: SttlValue? = local2Value.get(this)
        // 已删除
        if(value != null && value.deleted)
            value = null
        // 当值为null时初始化值
        if(value == null && initOnNull){
            value = SttlValue(supplier?.invoke())
            local2Value.put(this, value)
        }

        return value?.value as T
    }

    /**
     * 作用域开始
     *   开始新值, 如果有旧值, 就删掉
     */
    public override fun doBeginScope() {
        //println("beginScope")
        // 删除当前线程的旧值
        val v = local2Values.get().remove(this)
        // 旧值也删除当前线程
        v?.removeThread()
    }

    /**
     * 作用域结束
     *    删除值, 要删除所有被传递的线程的值
     *    endScope() 可能随时随地调用, 也就是说 SttlValue 随时可能被删除, 但可能某个线程调用了 SttlInterceptor.intercept(回调), 但此时回调还没触发, 也就是旧的 ScopedTransferableThreadLocal 对象还未恢复, 等恢复后引用的 SttlValue 却应该被删掉, 因此添加 deleted 属性来做是否已删除的判断
     */
    public override fun doEndScope() {
        //println("endScope")
        // 删除当前线程的值 -- 漏掉删除其他线程的值
        //local2Values.get().remove(this)

        // 删除所有被传递的线程的值
        // 获得值
        val value: SttlValue? = local2Values.get().get(this)
        if(value == null)
            return

        // 标记已删除
        value.deleted = true

        // 出队每个被传递线程来删除值
        value.pollEachThread { t ->
            // 获得该线程的map
            val local2Value = local2Values.get(t)
            if(local2Value != null) {
                // 获得该线程的值
                val value2: SttlValue? = local2Value.get(this)

                // 如果值没有改变, 则删除
                if (value2 == value)
                    local2Value.remove(this)
            }
        }
    }

}