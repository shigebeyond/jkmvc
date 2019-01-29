package com.jkmvc.common

import com.jkmvc.idworker.IIdWorker
import java.util.*

/**
 * id生成器
 */
private val idWorker: IIdWorker = IIdWorker.instance("snowflakeId")

/**
 * 生成唯一id
 * @return
 */
public fun generateId(): Long {
    return idWorker.nextId()
}

/**
 * 生成uuid
 */
public fun generateUUID(): String {
    return UUID.randomUUID().toString()
}