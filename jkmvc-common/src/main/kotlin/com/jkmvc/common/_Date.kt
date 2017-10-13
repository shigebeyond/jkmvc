package com.jkmvc.common

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 缓存日期格式
 */
private val dateFormats: ConcurrentHashMap<String, SimpleDateFormat> = ConcurrentHashMap()

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

/**
 * 获得一天的开始时间: 0:00:00
 * @return
 */
public fun Date.startTime(): Date{
    val calendar = GregorianCalendar();
    calendar.setTime(this);
    calendar.set(Calendar.HOUR,0)
    calendar.set(Calendar.MINUTE,0)
    calendar.set(Calendar.SECOND,0)
    calendar.set(Calendar.MILLISECOND,0)
    System.out.println("开始时间：" + calendar.time)
    return calendar.time
}

/**
 * 获得一天的结束时间: 23:59:59
 * @return
 */
public fun Date.endTime(): Date {
    val calendar = GregorianCalendar();
    calendar.setTime(this);
    calendar.set(Calendar.HOUR,23)
    calendar.set(Calendar.MINUTE,59)
    calendar.set(Calendar.SECOND,59)
    calendar.set(Calendar.MILLISECOND,999)
    System.out.println("结束时间：" + calendar.time)
    return calendar.time
}