package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.db.MutableRow

/**
 * ORM之数据校验
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
     * 从其他实体对象中设置字段值
     *   子类会改写
     * @param from
     */
    public override fun fromEntity(from: IOrmEntity){
        from.toMap(data)
    }

    /**
     * 转为实体对象
     * @return
     */
    public override fun toEntity(): OrmEntity {
        // 查找实体类
        val entityClass = this::class.entityClass
        if(entityClass == null)
            throw OrmException("模型类[${this.javaClass}]没有抽取实体类");

        // 创建实体
        val entity = entityClass.newInstance() as OrmEntity
        toEntity(entity)
        return entity
    }

    /**
     * 转为实体对象
     *   子类会改写
     *
     * @param entity
     */
    protected open fun toEntity(entity: OrmEntity) {
        this.toMap(entity.getData())
    }
}
