package com.jkmvc.orm

import com.jkmvc.validate.Validation
import java.util.*

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
    public override fun check(): Boolean {
        // 逐个字段校验
        for ((column, exp) in metadata.rules) {
            val value: Any = this[column];
            var last: Any = value;
            // 校验单个字段: 字段值可能被修改
            val (succ, uint) = Validation.execute(exp, value, data)
            if (succ == false) {
                val label: String = metadata.labels.getOrElse(column){ column }; // 字段标签（中文名）
                throw OrmException(label + uint?.message());
            }

            // 更新被修改的字段值
            if (value !== last)
                this[column] = value;
        }

        return true;
    }
}
