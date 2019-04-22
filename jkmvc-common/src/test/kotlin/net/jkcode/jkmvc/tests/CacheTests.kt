package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.cache.ICache
import net.jkcode.jkmvc.common.randomInt
import org.junit.Test

class CacheTests{

    @Test
    fun testGetOrPut(){
        val data = ICache.instance("jedis").getOrPut("test", 15){
            val v = randomInt(100)
            println("set cache: $v")
            v
        }
        println("get cache: " + data.get())
    }


}