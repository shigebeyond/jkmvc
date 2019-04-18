package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.common.makeThreads
import net.jkcode.jkmvc.common.randomBoolean
import net.jkcode.jkmvc.common.randomInt
import org.junit.Test
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class CompletableFutureTests{

    /**
     * CompletableFuture的 thenAccept() / exceptionally() / whenComplete()
     */
    @Test
    fun testBasic() {
        val f = CompletableFuture.supplyAsync {
            if(randomBoolean())
                throw Exception("随机的异常")
            randomInt(100)
        }
        // 处理响应
        f.thenAccept {
            println("成功了有结果: $it")
        }
        f.exceptionally {
            println("失败了有异常: $it")
            null
        }
        f.whenComplete{ r, e ->
            println("最后汇总: 结果=" + r + "，异常=" + e)
        }
        Thread.sleep(10000)
    }

    /**
     * CompletableFuture.allOf()
     */
    @Test
    fun testAllOf() {
        val start = System.currentTimeMillis()
        // 结果集
        val list = ArrayList<String>()

        //val taskList = listOf(2, 1, 3, 4, 5, 6, 7, 8, 9, 10)
        val taskList = listOf(2)
        // 全流式处理转换成CompletableFuture[]+组装成一个无返回值CompletableFuture，join等待执行完毕。返回结果whenComplete获取
        val cfs = taskList.stream()
                .map<Any> { i ->
                    CompletableFuture.supplyAsync{
                                Thread.sleep(1000L* randomInt(5))
                                println("task线程：" + Thread.currentThread().name
                                        + "任务i=" + i + ",完成！+" + Date())
                                i
                            }
                            .thenApplyAsync{
                                Integer.toString(it.toInt())
                            }
                            .whenComplete{ r, e ->
                                println("任务" + r + "完成!result=" + r + "，异常 e=" + e + "," + Date())
                                list.add(r)
                            }
                }
                .toArray(){
                    arrayOfNulls<CompletableFuture<String>>(it)
                }
        // 封装后无返回值，必须自己whenComplete()获取
        CompletableFuture.allOf(*cfs).join()
        println("list=" + list + ",耗时=" + (System.currentTimeMillis() - start))
    }

    /**
     * CompletableFuture 多个 thenAccept()
     */
    @Test
    fun testMultiThen() {
        val f = CompletableFuture.supplyAsync {
            if(randomBoolean())
                throw Exception("随机的异常")
            randomInt(100)
        }

        var i = 0
        f.thenAccept {
            println("第${i++}个监听者: $it")
        }
        f.thenAccept {
            println("第${i++}个监听者: $it")
        }
        f.thenAccept {
            println("第${i++}个监听者: $it")
        }
        f.exceptionally {
            println("监听异常: $it")
            null
        }
    }

    /**
     * CompletableFuture 并发get()
     */
    @Test
    fun testConcurrentGet() {
        val f = CompletableFuture.supplyAsync {
            Thread.sleep(1000)
            randomInt(100)
        }

        var i = AtomicInteger(0)
        val run = {
            println("第${i.getAndIncrement()}个等待者: ${f.get()}")
        }

        makeThreads(3, run)
    }

    @Test
    fun testCompleted(){
        CompletableFuture.completedFuture(1)
                .whenComplete{ r, e->
                    println("结果=" + r + "，异常=" + e)
                }
    }
}