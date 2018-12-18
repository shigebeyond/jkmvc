package com.jkmvc.orm

/**
 * (联合)主键, 支持多个字段
 * @author shijianhang<772910474@qq.com>
 * @date 2018-12-17 7:13 PM
 */
internal class DbKey<T>(val columns: Array<T>) : CharSequence by "" {

    /**
     * 第一个字段
     */
    public val first = columns.first()

    /**
     * 字段个数
     */
    public val size = columns.size

    /**
     * 生成新的主键
     *
     * @param transform 字段转换函数
     * @return
     */
    public inline fun <R> map(transform: (T) -> R): DbKey<R> {
        val newKeys = columns.clone() as Array<R>
        forEachColumn { i, v ->
            newKeys[i] = transform(columns[i])
        }
        return DbKey<R>(newKeys)
    }

    /**
     * 遍历每个字段
     * @param action 操作函数
     */
    public inline fun forEachColumn(action: (i: Int, T) -> Unit): Unit {
        var i = 0
        for (item in columns)
            action(i++, item)
    }

    /**
     * 访问字段
     * @param i
     * @return
     */
    public fun getColumn(i: Int): T{
        return columns[i]
    }

}

// 主键的字段名
internal typealias DbKeyName = DbKey<String>

// 主键的字段值
internal typealias DbKeyValue = DbKey<Any?>

/**
 * 添加前缀/后缀
 *
 * @param prefix 前缀
 * @param postfix 后缀
 * @return
 */
internal fun DbKeyName.wrap(prefix: CharSequence = "", postfix: CharSequence = ""): DbKeyName {
    return this.map {
        "$prefix$it$postfix"
    }
}
