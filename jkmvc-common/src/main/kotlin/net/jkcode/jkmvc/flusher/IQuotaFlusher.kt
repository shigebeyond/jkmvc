package net.jkcode.jkmvc.flusher

import net.jkcode.jkmvc.common.CommonThreadPool
import net.jkcode.jkmvc.common.commonLogger
import net.jkcode.jkmvc.common.runAsync
import net.jkcode.jkmvc.lock.AtomicLock
import java.lang.IllegalArgumentException
import java.util.concurrent.ExecutorService

/**
 * 定量刷盘
 *    
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-22 3:37 PM
 */
abstract class IQuotaFlusher<RequestType /* 请求类型 */, ResponseType /* 响应值类型 */>(
        protected val flushQuota: Int // 触发刷盘的计数大小
): IFlusher<RequestType, ResponseType> {

    /**
     * 执行线程(池)
     */
    protected open val executor: ExecutorService = CommonThreadPool

    /**
     * 限制定时+定量并发调用flush()的锁
     */
    protected val lock: AtomicLock = AtomicLock()

    /**
     * 开关, 2值的轮换
     */
    @Volatile
    protected var switch: Boolean = false

    init {
        if(flushQuota <= 0)
            throw IllegalArgumentException("flushQuota 属性值不是正整数: $flushQuota")
    }

    /**
     * 获得当前索引
     * @return
     */
    protected fun currIndex(): Int {
        return if (switch) 1 else 0
    }

    /**
     * 空 -> 非空: 尝试定量刷盘
     *   在添加请求时调用
     *
     * @param currRequestCount
     */
    protected open fun tryFlushWhenAdd(currRequestCount: Int) {
        // 定量刷盘
        if (currRequestCount >= flushQuota)
            flush(false)
    }

    /**
     * 将积累的请求刷掉
     * @param byTimeout 是否定时触发 or 定量触发
     */
    public override fun flush(byTimeout: Boolean) {
        // 加锁: 同一时刻只有一个线程能处理
        val locked = lock.quickLockCleanly {
            // 记录旧索引
            val oldIndex = currIndex()

            // 切换开关
            val oldSwitch = switch
            switch = !oldSwitch
            commonLogger.debug("PeriodicFlusher.flush() : switch from [$oldSwitch] to [${!oldSwitch}]")

            //处理旧索引的请求, 扔到线程池
            executor.runAsync {
                try{
                    doFlush(oldIndex)
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 处理旧索引的请求
     * @param oldIndex 旧索引, 因为新索引已切换, 现在要处理旧索引的请求
     */
    protected abstract fun doFlush(oldIndex: Int)

}