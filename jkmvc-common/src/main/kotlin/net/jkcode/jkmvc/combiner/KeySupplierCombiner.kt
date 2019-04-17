package net.jkcode.jkmvc.combiner

import net.jkcode.jkmvc.common.toFutureSupplier

/**
 * 针对每个key的取值操作合并, 也等同于取值操作去重
 *    如请求合并/cache合并等
 *    实现就是改进 ConcurrentMap.getOrPutOnce(key, defaultValue), 补上在多个取值操作处理完后删除key, 以便下一个轮回
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-10 9:47 AM
 */
class KeySupplierCombiner<RequestArgumentType /* 请求参数类型 */, ResponseType /* 响应类型 */>(supplier: (RequestArgumentType) -> ResponseType)
    : KeyFutureSupplierCombiner<RequestArgumentType, ResponseType>(toFutureSupplier(supplier)) {

}