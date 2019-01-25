package com.jkmvc.common

/**
 * 标记是否脏了(改动过数据)的map
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-01-21 6:01 PM
 */
class DirtyFlagMap<K, V> /* 非公开构造函数 */protected constructor(protected val map: MutableMap<K, V> /* 被代理的map */) : MutableMap<K, V> by map {

    /**
     * 公开构造函数
     */
    public constructor(): this(HashMap())

    /**
     * 是否脏了(改动过数据)
     */
    public var dirty: Boolean = false
            protected set

    public override fun put(key: K, value: V): V?{
        // 检查插入的值是否已存在
        if(map[key] == value)
            return value

        dirty = true
        return map.put(key, value)
    }

    public override fun remove(key: K): V?{
        // 检查删除的值是否已存在
        if(!map.containsKey(key))
            return null

        dirty = true
        return map.remove(key)
    }

    public fun remove(key: K, value: V): Boolean {
        // 检查删除的值是否已存在
        if(map[key] != value)
            return false

        dirty = true
        return map.remove(key, value)
    }

    public override fun putAll(from: Map<out K, V>): Unit{
        // 检查插入的值是否已存在JedisPoolConfig
        var same = true
        for((k, v) in from){
            if(map[k] != v){
                same = false
                break
            }
        }
        if(same)
            return

        dirty = true
        map.putAll(from)
    }

    public override fun clear(): Unit{
        // 检查是否早就是空的
        if(dirty == false && map.isEmpty())
            return

        dirty = true
        map.clear()
    }
}