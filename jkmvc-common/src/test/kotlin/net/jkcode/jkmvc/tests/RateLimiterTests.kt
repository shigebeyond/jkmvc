package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.ratelimit.CounterRateLimiter
import net.jkcode.jkmvc.ratelimit.IRateLimiter
import net.jkcode.jkmvc.ratelimit.TokenBucketRateLimiter
import net.jkcode.jkmvc.validator.RuleValidator
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool

class RateLimiterTests{

    val intervalMillis = 200

    @Test
    fun testCounterRateLimiter(){
        val l = CounterRateLimiter(1000 / intervalMillis, intervalMillis)
        testRateLimiter(l)
    }

    @Test
    fun testTokenBucketRateLimiter(){
        val l = TokenBucketRateLimiter(1000 / intervalMillis, intervalMillis)
        testRateLimiter(l)
    }

    @Test
    fun testConcurrent(){
        val l = TokenBucketRateLimiter(1000 / intervalMillis, intervalMillis)
        val threadNum = 5
        //val threadPool = ForkJoinPool.commonPool()
        val threadPool = Executors.newFixedThreadPool(threadNum)
        for(i in 0 until threadNum) {
            threadPool.execute {
                testRateLimiter(l)
            }
        }
        Thread.sleep(2000)
    }

    fun testRateLimiter(l: IRateLimiter) {
        val n: Int = 1000 / intervalMillis
        for(i in 0 until n) {
            println(" -------- Thread: ${Thread.currentThread().name}, Times: $i -------- ")
            println("time: " + getTime() + ", acquire: " + l.acquire())
            println("time: " + getTime() + ", acquire: " + l.acquire())
            println("time: " + getTime() + ", acquire: " + l.acquire())
            Thread.sleep(intervalMillis.toLong())
        }
    }

    fun getTime(): Long {
        return System.currentTimeMillis() / intervalMillis
    }

}