package net.jkcode.jkmvc.combiner

import net.jkcode.jkmvc.common.getProperty
import net.jkcode.jkmvc.common.isSuperClass
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KProperty1

/**
 * 针对每个group的取值操作合并, 每个group攒够一定数量/时间的请求才执行
 *    如请求合并/cache合并等
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-10 9:47 AM
 */
open class GroupFutureSupplierCombiner<RequestArgumentType /* 请求参数类型 */, ResponseType /* 响应类型 */, BatchItemType: Any /* 批量取值操作的返回列表的元素类型 */>(
        public val reqArgField: String, /* 请求参数对应的响应字段名 */
        public val respField: String = "", /* 要返回的响应字段名, 如果为空则取响应对象 */
        public val one2one: Boolean = true /* 请求对响应是一对一(ResponseType是非List), 还是一对多(ResponseType是List) */,
        flushSize: Int = 100 /* 触发刷盘的队列大小 */,
        flushTimeoutMillis: Long = 100 /* 触发刷盘的定时时间 */,
        public val batchFutureSupplier:(List<RequestArgumentType>) -> CompletableFuture<List<BatchItemType>> /* 批量取值操作 */
): RequestQueueFlusher<RequestArgumentType, ResponseType>(flushSize, flushTimeoutMillis){

    /**
     * 处理刷盘的请求
     *     如果 ResponseType != Void, 则需要你主动设置异步响应
     * @param args
     * @param reqs
     * @return 是否处理完毕, 同步处理返回true, 异步处理返回false
     */
    protected override fun handleFlush(args: List<RequestArgumentType>, reqs: ArrayList<Pair<RequestArgumentType, CompletableFuture<ResponseType>>>): Boolean {
        // 1 执行批量操作
        val resultFuture: CompletableFuture<List<BatchItemType>> = batchFutureSupplier.invoke(args)

        // 2 设置异步响应
        resultFuture.thenAccept { result ->
            // 空响应
            if(result.isEmpty()) {
                reqs.forEach {
                    it.second.complete(null)
                }
                return@thenAccept
            }

            // 非空响应
            val clazz = result.first().javaClass

            // 获得的响应字段的getter
            var reqArgGetter: (Any) -> RequestArgumentType = getGetter(clazz, reqArgField) // 请求参数的getter
            var respGetter: (Any) -> Any? = getGetter(clazz, respField) // 返回值的getter

            // 根据请求参数来分组响应
            var arg2resp: Map<RequestArgumentType, Any?>
            var defaultReps: Any?
            if(one2one) { // 一对一
                arg2resp = result.associate {
                    reqArgGetter(it)!! to respGetter(it)
                }
                defaultReps = null
            }else { // 一对多
                arg2resp = result.groupBy(reqArgGetter, respGetter)
                defaultReps = emptyList<Any?>()
            }


            // 设置异步响应
            reqs.forEach {
                // 根据请求参数来获得响应
                val resp = arg2resp.getOrDefault(it.first, defaultReps) as ResponseType
                it.second.complete(resp)
            }
        }


        return false
    }

    /**
     * 获得getter
     * @param clazz
     * @param field
     * @return
     */
    protected fun <T> getGetter(clazz: Class<BatchItemType>, field: String?): (Any) -> T {
        if(field.isNullOrEmpty())
            return {item: Any ->
                item as T
            }

        if (Map::class.java.isSuperClass(clazz)) {
            return { item: Any ->
                (item as Map<*, *>)[field] as T
            }
        }

        val prop = clazz.kotlin.getProperty(field) as KProperty1<Any, T>
        return prop::get
    }
}