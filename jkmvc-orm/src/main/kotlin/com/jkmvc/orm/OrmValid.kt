package com.jkmvc.orm

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
