package net.jkcode.jkmvc.db

import kotlin.reflect.KClass

/**
 * 结果集的一行
 *   生命周期只在转换结果集阶段, 不能被外部引用
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-11-21 4:27 PM
 */
class DbResultRow(protected val rs: DbResultSet) {

    /**
     * 通过inline指定的值类型, 来获得结果集的单个值
     * @param column
     * @return
     */
    public inline operator fun <reified T: Any> get(column: String): T?{
        return get(rs.findColumn(column))
    }

    /**
     * 通过inline指定的值类型, 来获得结果集的单个值
     * @param i
     * @return
     */
    public inline operator fun <reified T: Any> get(i: Int): T?{
        return get(i, T::class) as T?
    }

    /**
     * 获得当前行的单个值
     * @param i
     * @param clazz
     * @return
     */
    public fun get(i: Int, clazz: KClass<*>? = null): Any?{
        return rs.get(i, clazz)
    }

    /**
     * 遍历键值
     * @param action
     */
    public inline fun forEach(action: (String, Any?) -> Unit): Unit {
        for (i in 1..rs.metaData.columnCount) { // 多列
            val label: String = rs.metaData.getColumnLabel(i); // 字段名
            val value: Any? = rs.getValue(i) // 字段值
            action(label, value)
        }
    }

    /**
     * 转换为map
     * @param convertingColumn 是否转换字段名
     * @param to
     * @return
     */
    public fun toMap(convertingColumn: Boolean = false, to: MutableMap<String, Any?> = HashMap()): Map<String, Any?>{
        forEach { col, value ->
            val key = if(convertingColumn) rs.db.column2Prop(col) else col
            to[key] = value;
        }
        return to
    }

}