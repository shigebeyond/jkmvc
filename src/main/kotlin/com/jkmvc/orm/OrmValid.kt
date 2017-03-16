package com.jkmvc.orm

import java.util.*

/**
 * ORM之数据校验
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
abstract class OrmValid: OrmEntity() {

    companion object {
        /**
         * 每个字段的校验规则
         * @var array
         */
        protected val rules: MutableMap<String, Array<String>> by lazy {
            LinkedHashMap<String, Array<String>>()
        };

        /**
         * 每个字段的标签（中文名）
         * @var array
         */
        protected val labels: MutableMap<String, String> by lazy {
            LinkedHashMap<String, String>()
        };
    }

    /**
     * 校验数据
     * @return boolean
     */
    public override fun check(): Boolean {
        /*// 逐个字段校验
        for ((column, exp) in rules) {
            val value: Any = this[column];
            var last: Any = value;
            // 校验单个字段: 字段值可能被修改
            val (succ, message) = Validation.execute(exp, value, this, message)
            if (!succ) {
                val label: String = labels.getOrElse(column){ column }; // 字段标签（中文名）
                throw OrmException(label + message);
            }

            // 更新被修改的字段值
            if (value !== last)
                this[column] = value;
        }*/

        return true;
    }
}
