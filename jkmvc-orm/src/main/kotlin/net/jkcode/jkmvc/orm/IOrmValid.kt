package net.jkcode.jkmvc.orm

/**
 * ORM之数据校验
 *
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
interface IOrmValid : IOrmEntity {

    /**
     * 校验数据
     * @return
     */
    fun validate(): Boolean;

    /**
     * 标记字段为脏
     * @param column 字段名
     */
    fun setDirty(column: String)

    /**
     * 从其他实体对象中设置字段值
     *
     * @param from
     */
    fun fromEntity(from: IOrmEntity)

    /**
     * 转为实体对象
     * @return
     */
    fun toEntity(): OrmEntity
}
