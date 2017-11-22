package com.jkmvc.validate

import java.util.*

/**
 * 校验表达式
 *    校验表达式是由多个(函数调用的)子表达式与运算符组成, 格式为 a(1) & b(1,2) && c(3,4) | d(2) . e(1) > f(5)
 *    子表达式是函数调用, 格式为 a(1,2)
 *    子表达式之间用运算符连接, 运算符有 & && | || . >
 *    运算符的含义:
 *        & 与
 *        && 短路与
 *        | 或
 *        || 短路或
 *         .  字符串连接
 *         > 累积结果
 *   无意于实现完整语义的布尔表达式, 暂时先满足于输入校验与orm保存数据时的校验, 因此:
 *   运算符没有优先级, 只能按顺序执行, 不支持带括号的子表达式
 *
 * @author shijianhang
 * @date 2016-10-19 下午3:40:55
 *
 */
interface IValidationExpression
{
	/**
	* 原始表达式
	 */
	val exp:String

	/**
	 * 执行校验表达式
	 *
	 * <code>
	 * 	   // 编译
	 *     val exp = ValidationExpression("trim > notEmpty && email");
	 *     // 执行
	 *     result = exp.execute(value, data, lastsubexp);
	 * </code>
	 *
	 * @param Any? value 要校验的数值，该值可能被修改
	 * @param variables 变量
	 * @return Triple 结果+最后一个校验单元+最后一个值
	 */
	public fun execute(value:Any?, variables:Map<String, Any?> = emptyMap()): ValidationResult
}
