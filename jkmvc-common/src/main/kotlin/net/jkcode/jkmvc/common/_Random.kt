package net.jkcode.jkmvc.common

import java.util.concurrent.ThreadLocalRandom


/**
 * 随机的int
 * @return
 */
public inline fun randomInt(bound: Int): Int {
    return ThreadLocalRandom.current().nextInt(bound)
}

/**
 * 随机的long
 * @return
 */
public inline fun randomLong(bound: Long): Long {
    return ThreadLocalRandom.current().nextLong(bound)
}

/**
 * 随机的bool
 * @return
 */
public inline fun randomBoolean(): Boolean {
    return randomInt(2) == 1
}

/**
 * 随机字符串
 *
 * @param length
 * @param base
 * @return
 */
public fun randomString(length: Int, base: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"): String {
    val random = ThreadLocalRandom.current()
    val sb = StringBuffer()
    for (i in 0 until length) {
        // 随机选个字符
        val j = random.nextInt(base.length)
        sb.append(base[j])
    }
    //将承载的字符转换成字符串
    return sb.toString()
}

/**
 * 随机数字字符串
 *
 * @param length
 * @param base
 * @return
 */
public fun randomNumberString(length: Int): String {
    return randomString(length, "0123456789")
}