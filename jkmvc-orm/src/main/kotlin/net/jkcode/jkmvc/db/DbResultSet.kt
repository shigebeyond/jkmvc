package net.jkcode.jkmvc.db

import java.math.BigDecimal
import java.sql.Blob
import java.sql.Clob
import java.sql.ResultSet
import kotlin.reflect.KClass

/**
 * 结果集
 *    生命周期只在转换结果集阶段, 不能被外部引用
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-11-21 4:27 PM
 */
class DbResultSet(
        public val db: IDbMeta,
        protected val rs: ResultSet
) : ResultSet by rs, Iterable<DbResultRow> {

    /**
     * 迭代器
     */
    override fun iterator(): Iterator<DbResultRow> {
        return DbResultSetIterator(this)
    }

    /**
     * 列数
     */
    public val columnCount: Int
        get() = rs.metaData.columnCount

    /**
     * 列
     */
    public val columns: List<String>
        get() {
            val md = rs.metaData
            return (0 until md.columnCount).map { i ->
                md.getColumnLabel(i)
            }
        }

    /**
     * 转换为map
     *    TODO: 优化为固定key的map
     *
     * @param convertingColumn 是否转换字段名
     * @return
     */
    public fun toMaps(convertingColumn: Boolean = false): List<Map<String, Any?>> {
        return map { row ->
            row.toMap(convertingColumn)
        }
    }

    /**
     * 根据返回值的类型 XXX 来获得 ResultSet 的 getXXX() 方法, 即某列的取值方法
     *   参考 org.springframework.jdbc.support.JdbcUtils#getResultSetValue(java.sql.ResultSet, int, java.lang.Class<?>)
     *
     * @param i
     * @param clazz
     * @return
     */
    public inline fun get(i: Int, clazz: KClass<*>? = null): Any? {
        return when (clazz) {
            null -> getValue(i)

            String::class -> rs.getString(i)

            Int::class -> rs.getInt(i)
            Long::class -> rs.getLong(i)
            Float::class -> rs.getFloat(i)
            Double::class -> rs.getDouble(i)
            Boolean::class -> rs.getBoolean(i)
            Short::class -> rs.getShort(i)
            Byte::class -> rs.getByte(i)
            BigDecimal::class -> rs.getBigDecimal(i)

            java.util.Date::class -> rs.getDate(i)
            java.sql.Date::class -> rs.getDate(i)
            java.sql.Time::class -> rs.getTime(i)
            java.sql.Timestamp::class -> rs.getTimestamp(i)

            ByteArray::class -> rs.getBytes(i)
            Blob::class -> rs.getBlob(i)
            Clob::class -> rs.getClob(i)

            Any::class -> getObject(i)
            else -> getObject(i, clazz.java)
        }
    }

    /**
     * 获得结果集的单个值
     *   参考 org.springframework.jdbc.support.JdbcUtils#getResultSetValue(java.sql.ResultSet, int)
     * @param i
     * @return
     */
    public fun getValue(i: Int): Any? {
        val obj: Any? = rs.getObject(i)
        if (obj == null)
            return null

        // 常用的直接返回
        if (obj is Boolean || obj is Int || obj is Long || obj is String || obj is ByteArray)
            return obj

        // numeric
        /*if (obj is Number) {
            if (obj is BigDecimal) {
                if (obj.scale() == 0)
                    return obj.toBigInteger()

                // we might loose precision here
                return obj.toDouble()
            }

            return obj
        }*/

        // 二进制大对象
        if (obj is Blob)
            return obj.getBytes(1, obj.length().toInt())

        // 字符型大对象
        if (obj is Clob)
            return obj.getSubString(1, obj.length().toInt())

        // oracle时间对象
        val className = obj.javaClass.name
        if ("oracle.sql.TIMESTAMP" == className || "oracle.sql.TIMESTAMPTZ" == className)
            return rs.getTimestamp(i)

        if (className.startsWith("oracle.sql.DATE")) {
            val metaDataClassName = metaData.getColumnClassName(i)
            if ("java.sql.Timestamp" == metaDataClassName || "oracle.sql.TIMESTAMP" == metaDataClassName) {
                return rs.getTimestamp(i)
            }

            return rs.getDate(i)
        }

        return obj
    }


}