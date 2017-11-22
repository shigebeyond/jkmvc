package com.jkmvc.orm

import com.jkmvc.validate.Validation
import com.jkmvc.validate.ValidationException

/**
 * ORM之数据校验
 *
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
abstract class OrmValid: OrmEntity() {
    /**
     * 校验数据
     * @return
     */
    public override fun validate(): Boolean {
        // 逐个字段校验
        for ((column, rule) in ormMeta.rules) {
            if(rule.rule == null)
                break;

            // 获得字段值
            val value: Any = this[column];

            // 校验单个字段: 字段值可能被修改
            val (succ, uint, lastValue) = Validation.execute(rule.rule!!, value, data)
            if (succ == false)
                throw ValidationException(rule.label + uint?.message());

            // 更新被修改的字段值
            if (value !== lastValue)
                this[column] = value;
        }

        return true;
    }
}
