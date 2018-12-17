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
abstract class OrmValid : OrmEntity() {
    /**
     * 校验数据
     * @return
     */
    public override fun validate(): Boolean {
        // 逐个属性校验
        for ((field, rule) in ormMeta.rules) {
            if(rule.rule == null)
                break;

            // 获得属性值
            val value: Any = this[field];

            // 校验单个属性: 属性值可能被修改
            val (succ, uint, lastValue) = Validation.execute(rule.rule!!, value, data)
            if (succ == false)
                throw ValidationException(rule.label + uint?.message());

            // 更新被修改的属性值
            if (value !== lastValue)
                this[field] = value;
        }

        return true;
    }
}
