package com.jkmvc.orm

import com.jkmvc.db.IRecord
import java.util.*

/**
 * ORM之实体对象
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
abstract class OrmEntity : IRecord, IOrm {

    protected val data: MutableMap<String, Any?> = LinkedHashMap<String, Any?>()

    /**
     * 变化的字段值：<字段名 => 字段值>
     * @var array
     */
    protected val dirty: MutableSet<String> by lazy {
        HashSet<String>()
    };

    /**
     * 判断是否有某字段
     *
     * @param string column
     * @return
     */
    public override fun hasColumn(column: String): Boolean {
        return true;
    }

    /**
     * 设置对象字段值
     *
     * @param  string column 字段名
     * @param  mixed  value  字段值
     */
    public override operator fun set(column: String, value: Any?) {
        if (!hasColumn(column))
            throw OrmException("类 ${this.javaClass} 没有字段 $column");

        dirty.add(column);
        data[column] = value;
    }

    /**
     * 获得对象字段
     *
     * @param   string column 字段名
     * @return  mixed
     */
    public override operator fun <T> get(column: String, defaultValue: Any?): T {
        if (!hasColumn(column))
            throw OrmException("类 ${this.javaClass} 没有字段 $column");

        return (data[column] ?: defaultValue) as T
    }

    /**
     * 设置多个字段值
     *
     * @param  array values   字段值的数组：<字段名 => 字段值>
     * @param  array expected 要设置的字段名的数组
     * @return ORM
     */
    public override fun values(values: Map<String, Any?>, expected: List<String>?): IOrm {
        val columns = if (expected === null)
            values.keys
        else
            expected;

        for (column in columns)
            this[column] = values[column];

        return this;
    }

    /**
     * 获得字段值
     * @return array
     */
    public override fun asArray(): Map<String, Any?> {
        return data;
    }

}
