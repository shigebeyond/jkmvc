package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.combiner.GroupSupplierCombiner
import net.jkcode.jkmvc.common.randomInt
import net.jkcode.jkmvc.common.randomLong
import net.jkcode.jkmvc.common.randomString
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class GroupSupplierCombinerTest{

    companion object {

        val arg =  AtomicInteger(0)

        val count =  AtomicInteger(0)

        /**
         * 添加参数
         */
        fun addArgs(c: GroupSupplierCombiner<Int, String, Map<String, Any>>): List<CompletableFuture<String>> {
            return (1..randomInt(500)).map { _ ->
                println("添加参数: ${arg.incrementAndGet()}")
                c.add(arg.get())!!
            }
        }

        fun waitPrintFutures(futures: ArrayList<CompletableFuture<*>>) {
            val f: CompletableFuture<Void> = CompletableFuture.allOf(*futures.toTypedArray())
            f.get() // 等待
            println(futures.joinToString(", ", "结果: [", "]") {
                it.get().toString()
            })
        }
    }

    val combiner = GroupSupplierCombiner<Int, String, Map<String, Any>>("key", "value", true, this::batchSupplyInt)

    /**
     * 批量supplier
     */
    fun batchSupplyInt(args: List<Int>): List<Map<String, Any>> {
        println("第${count.incrementAndGet()}次调用批量取值函数: $args")
        return args.map {
            mapOf("key" to it, "value" to randomString(1))
        }
    }

    @Test
    fun testAdd1(){
        //println(combiner.javaClass.getSuperClassGenricType()) // 由于泛型确定是在子类, 因此无法获得父类的泛型类型

        val f = combiner.add(1)!!
        println(f.get())
        Thread.sleep(10000)
    }

    @Test
    fun testAddMany(){
        val futures = ArrayList<CompletableFuture<String>>()
        futures.addAll(addArgs(combiner)) // 添加请求
        waitPrintFutures(futures)
        Thread.sleep(10000)
    }

    @Test
    fun testAddConcurrent(){
        val threads = (0..1).map {
            val t = AddThread(combiner)
            t.start()
            t
        }

        Thread.sleep(10000)

        threads.forEach {
            waitPrintFutures(it.futures)
        }
    }

    final class AddThread(val combiner: GroupSupplierCombiner<Int, String, Map<String, Any>>): Thread(){
        public val futures = ArrayList<CompletableFuture<String>>()

        override fun run(){
            for(i in 0..9) {
                futures.addAll(addArgs(combiner))
                Thread.sleep(randomLong(500))
            }
        }
    }



}