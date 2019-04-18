package net.jkcode.jkmvc.combiner

import net.jkcode.jkmvc.common.toFutureSupplier

/**
 * 针对每个group的无值操作合并, 每个group攒够一定数量/时间的请求才执行
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-10 9:47 AM
 */
class GroupRunCombiner<RequestArgumentType/* 请求参数类型 */>(
        flushSize: Int = 100 /* 触发刷盘的队列大小 */,
        flushTimeoutMillis: Long = 100 /* 触发刷盘的定时时间 */,
        batchRun:(List<RequestArgumentType>) -> Void /* 批量无值操作 */
): GroupFutureRunCombiner<RequestArgumentType>(flushSize, flushTimeoutMillis, toFutureSupplier(batchRun)){
}