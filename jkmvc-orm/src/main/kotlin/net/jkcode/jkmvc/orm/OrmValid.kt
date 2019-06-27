package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.common.LazyAllocatedMap
import net.jkcode.jkmvc.db.MutableRow
import net.jkcode.jkmvc.db.Row

/**
 * ORM之数据校验
 *
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
abstract class OrmValid : IOrm, OrmEntity() {

    /**
     * 最新的字段值：<字段名 to 最新字段值>
     */
    //protected val data: MutableRow = HashMap()
    protected override val data: MutableRow = ormMeta.dataFactory.createMap()

    /**
     * 变化的字段值：<字段名 to 原始字段值>
     *     一般只读，lazy创建，节省内存
     */
    protected val dirty: MutableRow = LazyAllocatedMap<String, Any?>()

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
     * 设置多个字段值
     *     返回值类型由 IOrmEntity 扩大为 IOrm
     * @param values   字段值的数组：<字段名 to 字段值>
     * @param expected 要设置的字段名的数组
     * @return
     */
    public override fun values(values: Row, expected: List<String>?): IOrm{
        return super.values(values, expected) as IOrm
    }

    /**
     * 校验数据
     * @return
     */
    public override fun validate(): Boolean {
        // 逐个属性校验
        for ((field, rule) in ormMeta.rules) {
            // 获得属性值
            val value: Any = this[field];

            // 校验单个属性: 属性值可能被修改
            val newValue = rule.validate(value, data)

            // 更新被修改的属性值
            if (value !== newValue)
                this[field] = newValue;
        }

        return true;
    }
}
