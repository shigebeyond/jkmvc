package com.jkmvc.validate

/**
 * 校验结果
 *
 * @author shijianhang
 * @date 2016-10-10
 */
data class ValidationResult(
        override val result: Any?, /* 结果 */
        override val unit: ValidationUint?, /* 最后一个校验单元 */
        override val lastValue: Any? /* 最后一个值 */
) : IValidationResult {
}