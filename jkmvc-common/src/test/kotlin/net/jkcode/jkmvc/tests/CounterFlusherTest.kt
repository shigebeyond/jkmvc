package net.jkcode.jksoa.guard

import net.jkcode.jkmvc.common.makeThreads
import net.jkcode.jkmvc.flusher.CounterFlusher
import org.junit.Test
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-18 4:36 PM
 */
class CounterFlusherTest {

    val counter = object: CounterFlusher(90, 100) {
        // 处理刷盘
        override fun handleFlush(reqCount: Long): Boolean {
            print(if(reqCount < flushSize) "定时" else "定量")
            println("sync, 请求计数 from [$reqCount] to [${requestCount()}] ")
            return true
        }
    }

    @Test
    fun testAdd(){
        val futures = LinkedList<CompletableFuture<*>>()
        for(i in 0 until 100){
            val future = counter.add()
            futures.add(future)
        }
        CompletableFuture.allOf(*futures.toTypedArray()).get()
        println("over")
    }

    @Test
    fun testAdd2(){
        val futures = LinkedList<CompletableFuture<*>>()
        makeThreads(10){i ->
            for(j in 0 until 100) {
                val future = counter.add()
                futures.add(future)
                Thread.sleep(100)
            }
        }
        CompletableFuture.allOf(*futures.toTypedArray()).get()
        println("over")
    }
}