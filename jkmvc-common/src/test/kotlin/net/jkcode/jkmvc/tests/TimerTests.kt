package net.jkcode.jkmvc.tests

import io.netty.util.HashedWheelTimer
import io.netty.util.Timeout
import io.netty.util.TimerTask
import net.jkcode.jkmvc.common.CommonMilliTimer
import net.jkcode.jkmvc.common.format
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 测试定时器
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-04-18 5:38 PM
 */
class TimerTests {

    @Test
    fun testTimer(){
        //val timer = CommonMilliTimer
        val timer = HashedWheelTimer(1, TimeUnit.SECONDS, 3 /* 内部会调用normalizeTicksPerWheel()转为2的次幂, 如3转为4 */)
        timer.newTimeout(object : TimerTask {
            override fun run(timeout: Timeout) {
                println("定时处理: " + Date().format())
                timer.newTimeout(this, 3, TimeUnit.SECONDS)
            }
        }, 3, TimeUnit.SECONDS)

        // HashedWheelTimer 是独立的线程, 需要在当前线程中等待他执行
        try {
            Thread.sleep(300L * 1000L)
        } catch (e: Exception) {
        }
    }

    @Test
    fun testTimer2(){
        newTimer()
        Thread.sleep(100000)
    }

    private fun newTimer() {
        val span: Long = 1000
        CommonMilliTimer.newTimeout(object : TimerTask {
            override fun run(timeout: Timeout) {
                println("每隔 $span ms执行一次")

                newTimer()
            }
        }, span, TimeUnit.MILLISECONDS)
    }

}