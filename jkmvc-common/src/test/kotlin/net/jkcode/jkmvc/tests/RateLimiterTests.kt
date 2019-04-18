package net.jkcode.jkmvc.tests

import com.google.common.util.concurrent.RateLimiter
import net.jkcode.jkmvc.common.currMillis
import org.junit.Test

class RateLimiterTests{

    val intervalMillis = 200

    fun getTime(): Long {
        return currMillis() / intervalMillis
    }

    @Test
    fun testRateLimiter(){
        val l = RateLimiter.create(1.0);
        System.out.println("time: " + getTime() + ", acquire: " + l.acquire());

        //Thread.sleep(1000L);
        // guava的 RateLimiter.acquire() 返回的是等待时间,其内部也调用 Uninterruptibles.sleepUninterruptibly() 来休眠线程
        //Uninterruptibles.sleepUninterruptibly(100, TimeUnit.SECONDS)

        println("time: " + getTime() + ", acquire: " + l.acquire())
        println("time: " + getTime() + ", acquire: " + l.acquire())
        println("time: " + getTime() + ", acquire: " + l.acquire())
        println("time: " + getTime() + ", acquire: " + l.acquire())
    }


}