package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.common.format
import net.jkcode.jkmvc.db.MutableRow
import java.util.*
import kotlin.collections.HashMap

/**
 * ORM之数据校验+格式化
 *
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
abstract class OrmValid : IOrm, OrmEntity() {

    /**
     * 改写 OrmEntity 中的 data属性
     * 最新的字段值：<字段名 to 最新字段值>
     */
    protected override val data: MutableRow by lazy{
        try {
            ormMeta.dataFactory.createMap()
        }catch (e: Exception){
            e.printStackTrace()
            throw e
        }
    }

    /**
     * 变化的字段值：<字段名 to 原始字段值>
     *     一般只读，lazy创建，节省内存
     */
    protected val dirty: MutableRow = HashMap<String, Any?>()

    /**
     * 设置对象字段值
     *    支持记录变化的字段名 + 原始值
     *
     * @param column 字段名
     * @param  value  字段值
     */
    public override operator fun set(column: String, value: Any?) {
        if (!hasColumn(column))
            throw OrmException("类 ${this.javaClass} 没有字段 $column");

        // 记录变化的字段名 + 原始值
        if(!dirty.containsKey(column)
                //&& value != data[column])
                && !equalsValue(data[column], value))
            dirty[column] = data[column];

        super.set(column, value)
    }

    /**
     * 标记字段为脏
     * @param column 字段名
     */
    public override fun setDirty(column: String){
        if(!dirty.containsKey(column))
            dirty[column] = data[column];
    }

    /**
     * 校验数据
     * @return
     */
    public override fun validate(): Boolean {
        return ormMeta.validate(this)
    }

    /**
     * 格式化日期字段值
     * @param column
     * @return
     */
    public fun formateDate(column: String): String {
        val value = data[column]
        if(value is Date)
            return value.format()

        return ""
    }

    /**
     * 格式化时间戳字段值
     * @param column
     * @param isSecond 是否秒数, 否则毫秒数
     * @return
     */
    @JvmOverloads
    public fun formateTimestamp(column: String, isSecond: Boolean = true): String {
        val value = data[column]
        if(value is Long)
            return Date(value).format()

        return ""
    }

}
