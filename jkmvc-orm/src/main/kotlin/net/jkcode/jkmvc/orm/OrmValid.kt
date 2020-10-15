package net.jkcode.jkmvc.orm

import net.jkcode.jkutil.validator.ModelValidateResult


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
    protected override val _data: MutableMap<String, Any?> by lazy{
        ormMeta.dataFactory.createMap()
    }

    /**
     * 变化的字段值：<字段名 to 原始字段值>
     *     一般只读，lazy创建，节省内存
     */
    protected val _dirty: MutableMap<String, Any?> by lazy {
        HashMap<String, Any?>()
    }

    /**
     * 设置对象字段值
     *    支持记录变化的字段名 + 原始值
     *
     * @param column 字段名
     * @param  value  字段值
     */
    public override operator fun set(column: String, value: Any?) {
        if (!hasColumn(column))
            throw OrmException("Model class ${this.javaClass} has no property: $column");

        // 记录变化的字段名 + 原始值
        if(!_dirty.containsKey(column)
                //&& value != _data[column])
                && !equalsValue(_data[column], value))
            _dirty[column] = _data[column];

        super.set(column, value)
    }

    /**
     * 标记字段为脏
     * @param column 字段名
     * @param flag 是否脏
     */
    public override fun setDirty(column: String, flag: Boolean){
        // 标记为脏
        if(flag) {
            if (_dirty.containsKey(column))
                _dirty[column] = _data[column];
            return
        }

        // 标记不脏
        _dirty.remove(column)
    }

    /**
     * 检查字段是否为脏
     * @param column 字段名
     */
    public override fun isDirty(column: String): Boolean {
        return _dirty.containsKey(column)
    }

    /**
     * 校验数据
     * @return
     */
    public override fun validate(): ModelValidateResult {
        return ormMeta.validate(this)
    }

}
