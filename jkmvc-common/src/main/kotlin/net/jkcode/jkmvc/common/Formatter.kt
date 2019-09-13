package net.jkcode.jkmvc.common

import java.text.DecimalFormat
import java.util.*

/**
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-13 4:35 PM
 */
object Formatter {

    /**
     * 格式化日期
     * @param value
     * @return
     */
    @JvmStatic
    public fun formateDate(value: Date): String {
        return value.format()
    }

    /**
     * 格式化时间戳
     * @param value
     * @param isSecond 是否秒数, 否则毫秒数
     * @return
     */
    @JvmStatic
    @JvmOverloads
    public fun formateTimestamp(value: Long, isSecond: Boolean = true): String {
        val ts = if (isSecond) value * 1000 else value
        return Date(ts).format()
    }

    /**
     * 格式化分
     * @param value
     * @return
     */
    @JvmStatic
    @JvmOverloads
    public fun formateCents(value: Int, pattern: String = "##.##元"): String {
        val df = DecimalFormat(pattern)
        return df.format(value / 100.0)
    }

}