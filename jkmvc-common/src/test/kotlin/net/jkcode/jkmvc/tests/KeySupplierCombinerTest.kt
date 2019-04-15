package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.combiner.KeySupplierCombiner
import net.jkcode.jkmvc.common.randomInt
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class KeySupplierCombinerTest{

    @Test
    fun testCombine(){
        val c = KeySupplierCombiner()
        val key = "test"
        var i = AtomicInteger(0)

        val run: () -> Unit ={
            val r = c.combine(key, this::supplyInt)
            println("第${i.getAndIncrement()}个调用者: $r")
        }
        Thread(run).start()
        Thread(run).start()
        Thread(run).start()

        Thread.sleep(2000)
    }

    @Test
    fun testCombineAsync(){
        val c = KeySupplierCombiner()
        val key = "test"
        var i = AtomicInteger(0)

        val run: () -> Unit = {
            val f = c.combineAsync(key, this::supplyInt)
            f.thenAccept {
                println("第${i.getAndIncrement()}个调用者: $it")
            }

        }
        Thread(run).start()
        Thread(run).start()
        Thread(run).start()

        Thread.sleep(2000)
    }

    @Test
    fun testCombineFuture(){
        val c = KeySupplierCombiner()
        val key = "test"
        var i = AtomicInteger(0)

        val run: () -> Unit = {
            val f = c.combineAsync(key, this::supplyIntFuture)
            f.thenAccept {
                println("第${i.getAndIncrement()}个调用者: " + it.get())
            }

        }
        Thread(run).start()
        Thread(run).start()
        Thread(run).start()

        Thread.sleep(2000)
    }

    /**
     * 同步的supplier
     */
    fun supplyInt(): Int {
        println("调用 supply, 只调用一次")
        Thread.sleep(100)
        return randomInt(100)
    }

    /**
     * 异步的supplier
     */
    fun supplyIntFuture(): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync(this::supplyInt)
    }
}