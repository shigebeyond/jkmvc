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

}
