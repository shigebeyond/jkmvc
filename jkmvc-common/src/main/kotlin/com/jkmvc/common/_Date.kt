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
 * 时间运算
 *    参考 java.util.GregorianCalendar.add
 *
 * @param field the calendar field.
 * @param amount the amount of date or time to be added to the field.
 * @return
 */
public fun Date.add(field:Int, amount:Int): Date {
    val calendar = GregorianCalendar()
    calendar.time = this
    calendar.add(field, amount)
    return calendar.time
}

/**
 * 获得日历字段
 *   参考 java.util.Calendar.get
 *
 * @param field the calendar field.
 * @return
 */
public fun Date.get(field:Int): Int {
    val calendar = GregorianCalendar()
    calendar.setTime(this)
    return calendar.get(field)
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