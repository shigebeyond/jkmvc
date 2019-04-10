package net.jkcode.jkmvc.common

import net.jkcode.jkmvc.idworker.IIdWorker
import net.jkcode.jkmvc.idworker.SnowflakeIdWorker
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * <模块 to id生成器>
 */
private val idWorkers: ConcurrentHashMap<String, IIdWorker> = ConcurrentHashMap()

/**
 * 生成唯一id
 * @param module 模块
 * @return
 */
public fun generateId(module: String): Long {
    val idWorker = idWorkers.getOrPut(module) {
        SnowflakeIdWorker()
    }
    return idWorker.nextId()
}

/**
 * 生成uuid
 * @return
 */
public fun generateUUID(): String {
    return UUID.randomUUID().toString()
}