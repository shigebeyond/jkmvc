package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.combiner.KeySupplierCombiner
import net.jkcode.jkmvc.common.randomInt
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class KeySupplierCombinerTest{

    @Test
    fun testAdd(){
        val combiner = KeySupplierCombiner(this::supplyInt)
        val key = "test"
        var i = AtomicInteger(0)

        val run: () -> Unit ={
            val r = combiner.add(key).get()
            println("第${i.getAndIncrement()}个调用者: $r")
        }
        for(i in 0..2)
            Thread(run).start()

        Thread.sleep(2000)
    }

    /**
     * 同步的supplier
     */
    fun supplyInt(arg: String): Int {
        println("调用 supply, 只调用一次")
        Thread.sleep(100)
        return randomInt(100)
    }

}