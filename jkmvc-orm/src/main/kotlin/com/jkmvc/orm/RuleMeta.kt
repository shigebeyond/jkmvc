package com.jkmvc.orm

/**
 * 字段校验规则的元数据
 *    标签 + 校验规则
 * @author shijianhang
 * @date 2016-10-10
 */
data class RuleMeta(
        override val label:String /* 字段标签（中文名） */,
        override val rule: String? /* 字段的校验规则 */
): IRuleMeta {
}
