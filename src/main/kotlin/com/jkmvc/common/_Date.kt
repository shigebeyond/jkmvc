package com.jkmvc.common

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

/**
 * 缓存日期格式
 */
val dateFormats: ConcurrentHashMap<String, SimpleDateFormat> = ConcurrentHashMap()

/**
 * 日期格式化
 *
 * @param pattern 格式
 * @return
 */
public fun Date.format(pattern: String): String
{
    return dateFormats.getOrPut(pattern){
        SimpleDateFormat(pattern)
    }
    .format(this)
}