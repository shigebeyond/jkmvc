package net.jkcode.jkmvc.db

import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass

/**
 * 结果集的一行
 *   生命周期只在转换结果集阶段, 不能被外部引用
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-11-21 4:27 PM
 */
class DbResultRow(public val rs: DbResultSet): Iterable<Pair<String, Any?>> {

    /**
     * 迭代器
     */
    override fun iterator(): Iterator<Pair<String, Any?>> {
        return DbResultRowIterator(this)
    }

    /**
     * 通过inline指定的值类型, 来获得结果集的单个值
     * @param column
     * @return
     */
    public inline operator fun <reified T: Any> get(column: String): T?{
        return get(rs.findColumn(column)) // mysql jdbc就是这么实现的, 参考 com.mysql.jdbc.ResultSetImpl.getString(java.lang.String)
    }

    /**
     * 通过inline指定的值类型, 来获得结果集的单个值, 超出列报错
     * @param i
     * @return
     */
    public inline operator fun <reified T: Any> get(i: Int): T?{
        return get(i, T::class) as T?
    }

    /**
     * 通过inline指定的值类型, 来获得结果集的单个值, 超出列返回null
     * @param i
     * @return
     */
    public inline fun <reified T: Any> getOrNull(i: Int): T?{
        if(i <= 0 && i > rs.columnCount)
            return null

        return get(i)
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
     * 转换为map
     * @param convertingColumn 是否转换字段名
     * @param to
     * @return
     */
    public fun toMap(convertingColumn: Boolean = false, to: MutableMap<String, Any?> = HashMap()): Map<String, Any?>{
        forEach { (col, value) ->
            val key = if(convertingColumn) rs.db.column2Prop(col) else col
            val nullInvalid = value == null && to is Hashtable // 不允许null的情况
            if(!nullInvalid)
                to[key] = value;
        }
        return to
    }

}