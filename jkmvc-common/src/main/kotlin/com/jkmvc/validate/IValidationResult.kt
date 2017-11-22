package com.jkmvc.validate

/**
 * 校验结果
 *
 * @author shijianhang
 * @date 2016-10-10
 */
interface IValidationResult {

    /**
     * 结果
     */
    val result: Any?

    /**
     * 最后一个校验单元
     */
    val unit: ValidationUint?

    /**
     * 最后一个值
     */
    val lastValue: Any?
}
