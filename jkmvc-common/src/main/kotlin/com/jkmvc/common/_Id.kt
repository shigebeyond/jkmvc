package com.jkmvc.common

import com.jkmvc.idworker.IIdWorker
import com.jkmvc.idworker.SnowflakeIdWorker
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
    val idWorker = idWorkers.getOrPutOnce(module) {
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