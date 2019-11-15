package net.jkcode.jkmvc.common

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

// 是否缓存当前毫秒
public var currMillisCached: Boolean = true
// 缓存当前毫秒
@Volatile
private var currMs:Long = 0
// 是否启动定时更新当前毫秒
private val started: AtomicBoolean = AtomicBoolean(false)

/**
 * 获得以毫秒为单位的当前时间
 *   优化原生api `System.currentTimeMillis()`, 频繁调用消耗大, 因此使用 currMs 来缓存当前毫秒, 并由 CommonMilliTimer 来定时刷新
 *   但是不精准, 不能用于精确毫秒的场景, 如SnowflakeIdWorker, 他使用currMillis()会导致生成id为负数
 * @return
 */
public fun currMillis(): Long {
    // 不缓存
    if(!currMillisCached)
        return System.currentTimeMillis()

    // 缓存
    if(!started.get()){ // 未启动定时
        if(started.compareAndSet(false, true))
            CommonMilliTimer.newPeriodic({ currMs = System.currentTimeMillis()}, 1, TimeUnit.MILLISECONDS)
        else
            currMs = System.currentTimeMillis()
    }
    return currMs
}

/**
 * 缓存日期格式
 *   使用 ThreadLocal 解决 SimpleDateFormat 线程安全问题
 */
private val dateFormats: ThreadLocal<ConcurrentHashMap<String, SimpleDateFormat>> = ThreadLocal.withInitial {
    ConcurrentHashMap<String, SimpleDateFormat>()
}

/**
 * 日期格式化
 *
 * @param pattern 格式
 * @return
 */
public fun Date.format(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    return dateFormats.get().getOrPut(pattern){
                SimpleDateFormat(pattern)
            }
            .format(this)
}

/**
 * 输出时间
 *
 * @return
 */
public fun Date.print():Unit{
    println(this.format("yyyy-MM-dd HH:mm:ss"))
}

/**
 * 线程安全的日历对象
 */
private val calendars:ThreadLocal<GregorianCalendar> = ThreadLocal.withInitial {
    GregorianCalendar()
}

/**
 * 清零时间
 * @return
 */
public fun Calendar.zeroTime(): Calendar {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    return this
}

/**
 * 充满时间
 * @return
 */
public fun Calendar.fullTime(): Calendar {
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
    return this
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
    val calendar = calendars.get()
    calendar.time = this
    calendar.add(field, amount)
    return calendar.time
}

/**
 *  对时间应用Calendar的操作，并返回新的时间
 *
 *  @param block Calendar对象的操作
 *  @return
 */
public fun Date.applyCalendar(block: Calendar.() -> Unit): Date? {
    val calendar = calendars.get()
    calendar.time = this
    calendar.block()
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
    val calendar = calendars.get()
    calendar.setTime(this)
    return calendar.get(field)
}

/**
 * 获得一天的开始时间: 0:00:00
 * @return
 */
public val Calendar.dayStartTime:Date
    get(){
        zeroTime()
        return time
    }

/**
 * 获得一天的结束时间: 23:59:59
 * @return
 */
public val Calendar.dayEndTime:Date
    get(){
        fullTime()
        return time
    }

/**
 * 获得一周的开始时间: 周日0:00:00
 * @return
 */
public val Calendar.weekStartTime:Date
    get(){
        set(Calendar.DAY_OF_WEEK, 1)
        zeroTime()
        return time
    }

/**
 * 获得一周的结束时间: 周六23:59:59
 * @return
 */
public val Calendar.weekEndTime:Date
    get(){
        set(Calendar.DAY_OF_WEEK, 7)
        fullTime()
        return time
    }

/**
 * 获得一周的开始时间: 周一0:00:00
 * @return
 */
public val Calendar.weekStartTime2:Date
    get(){
//        set(Calendar.DAY_OF_WEEK, 0)
//        add(Calendar.DATE, 1)
        val weekday = get(Calendar.DAY_OF_WEEK) - 2
        add(Calendar.DATE, -weekday)
        zeroTime()
        return time
    }

/**
 * 获得一周的结束时间: 周日23:59:59
 * @return
 */
public val Calendar.weekEndTime2:Date
    get(){
//        set(Calendar.DAY_OF_WEEK, 7)
//        add(Calendar.DATE, 1)
        val weekday = get(Calendar.DAY_OF_WEEK)
        add(Calendar.DATE, 8 - weekday)
        fullTime()
        return time
    }

/**
 * 获得一月的开始时间: 1号0:00:00
 * @return
 */
public val Calendar.monthStartTime:Date
    get(){
        set(Calendar.DATE, 1);
        zeroTime()
        return time
    }

/**
 * 获得一月的结束时间: 30/31号23:59:59
 * @return
 */
public val Calendar.monthEndTime:Date
    get(){
        set(Calendar.DATE, 1);
        add(Calendar.MONTH, 1); // 下月1号
        add(Calendar.DATE, -1);
        fullTime()
        return time
    }

/**
 * 获得一年的开始时间: 1月1号0:00:00
 * @return
 */
public val Calendar.yearStartTime:Date
    get(){
        set(Calendar.MONTH, 0);
        set(Calendar.DATE, 1);
        zeroTime()
        return time
    }

/**
 * 获得一年的结束时间: 12月30/31号23:59:59
 * @return
 */
public val Calendar.yearEndTime:Date
    get(){
        set(Calendar.MONTH, 11);
        set(Calendar.DATE, 31);
        fullTime()
        return time
    }

/**
 * 获得一季度的开始时间: 季度1号0:00:00
 * @return
 */
public val Calendar.quarterStartTime:Date
    get(){
        val currentMonth = get(Calendar.MONTH) + 1
        set(Calendar.MONTH, currentMonth / 3 * 3);
        set(Calendar.DATE, 1);
        zeroTime()
        return time
    }

/**
 * 获得一季度的结束时间: 季度30/31号23:59:59
 * @return
 */
public val Calendar.quarterEndTime:Date
    get(){
        val currentMonth = get(Calendar.MONTH) + 1
        set(Calendar.MONTH, currentMonth / 3 * 3 + 3);
        set(Calendar.DATE, 1);
        add(Calendar.DATE, -1);
        fullTime()
        return time
    }