package net.jkcode.jkmvc.combiner

import net.jkcode.jkmvc.common.toFutureSupplier

/**
 * 针对每个group的取值操作合并, 每个group攒够一定数量/时间的请求才执行
 *    如请求合并/cache合并等
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-10 9:47 AM
 */
class GroupSupplierCombiner<RequestArgumentType /* 请求参数类型 */, ResponseType /* 响应类型 */, BatchItemType: Any /* 批量取值操作的返回列表的元素类型 */>(
        reqArgField: String, /* 请求参数对应的响应字段名 */
        respField: String = "", /* 要返回的响应字段名, 如果为空则取响应对象 */
        one2one: Boolean = true, /* 请求对响应是一对一(ResponseType是非List), 还是一对多(ResponseType是List) */
        flushSize: Int = 100 /* 触发刷盘的队列大小 */,
        flushTimeoutMillis: Long = 100 /* 触发刷盘的定时时间 */,
        batchSupplier:(List<RequestArgumentType>) -> List<BatchItemType> /* 批量取值操作 */
): GroupFutureSupplierCombiner<RequestArgumentType, ResponseType, BatchItemType>(reqArgField, respField, one2one, flushSize, flushTimeoutMillis, toFutureSupplier(batchSupplier)){

}