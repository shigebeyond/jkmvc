package com.jkmvc.common

import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期格式化
 *
 * @param pattern 格式
 * @return
 */
public fun Date.format(pattern: String): String
{
    val formatter = SimpleDateFormat(pattern)
    return formatter.format(this)
}