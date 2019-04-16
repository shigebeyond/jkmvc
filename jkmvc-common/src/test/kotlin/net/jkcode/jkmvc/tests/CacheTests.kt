package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.cache.ICache
import net.jkcode.jkmvc.common.randomInt
import org.junit.Test

class CacheTests{

    @Test
    fun testGetOrPut(){
        ICache.instance("jedis").getOrPut("test", 60){
            Thread.sleep(1000)
            randomInt(100)
        }
    }


}