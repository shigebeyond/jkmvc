package net.jkcode.jkmvc.orm

import net.jkcode.jkutil.validator.ModelValidateResult
import net.jkcode.jkutil.validator.ValidateResult

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
    fun validate(): ModelValidateResult?;

    /**
     * 校验数据
     */
    fun validateOrThrow() {
        validate()?.getOrThrow()
    }

    /**
     * 标记字段为脏
     * @param column 字段名
     * @param flag 是否脏
     */
    fun setDirty(column: String, flag: Boolean = true)

    /**
     * 检查字段是否为脏
     * @param column 字段名
     */
    fun isDirty(column: String): Boolean

    /**
     * 检查字段是否为脏
     * @param key 字段名
     */
    public fun isDirty(key: DbKeyNames): Boolean {
        return key.columns.any { column ->
            isDirty(column)
        }
    }
}
