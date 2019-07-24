package net.jkcode.jkmvc.flusher

import io.netty.util.Timeout
import io.netty.util.TimerTask
import net.jkcode.jkmvc.common.CommonMilliTimer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 定时刷盘
 *    使用 startTimernewTimeout() 即 newTimeout() 来设置一次定时flush(), 但是不需要递归调用, 因为flush()会清理所有当前所有请求, 也就暂时没有再来 startTimer() 即 newTimeout() 的必要
 *    以后只要调用 add() 添加请求, 就主动触发定时器, 参考 tryFlushWhenAdd(), 这样就很节省定时器资源, 虽然说定时不及时
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-17 8:27 AM
 */
abstract class ITimeFlusher<RequestType /* 请求类型 */, ResponseType /* 响应值类型 */>(
        flushSize: Int, // 触发刷盘的计数大小
        protected val flushTimeoutMillis: Long // 触发刷盘的定时时间
) : IQuotaFlusher<RequestType, ResponseType>(flushSize) {

    /**
     * 定时器状态: 0: 已停止 / 非0: 进行中
     *   用于控制是否停止定时器
     */
    protected val timerState: AtomicInteger = AtomicInteger(0)

    /**
     * 空 -> 非空: 启动定时
     *   在添加请求时调用
     *
     * @param currRequestCount
     */
    protected override fun tryFlushWhenAdd(currRequestCount: Int){
        // 调用父类实现: 尝试定量刷盘
        super.tryFlushWhenAdd(currRequestCount)

        // 空 -> 非空: 启动定时
        if (timerState.get() == 0 && timerState.getAndIncrement() == 0)
            startTimer()
    }

    /**
     * 启动刷盘的定时任务
     */
    protected fun startTimer(){
        CommonMilliTimer.newTimeout(object : TimerTask {
            override fun run(timeout: Timeout) {
                // 刷盘
                flush(true)

                // 非空: 继续定时
                if(timerState.decrementAndGet() > 0)
                    startTimer()
            }
        }, flushTimeoutMillis, TimeUnit.MILLISECONDS)
    }

}