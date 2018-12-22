package com.jkmvc.validator

/**
 * 校验表达式
 *
 * 1 格式
 *    校验表达式是由多个(函数调用的)子表达式组成, 子表达式之间以空格分隔, 格式为 a(1) b(1,2) c(3,4)
 *    子表达式是函数调用, 格式为 a(1,2)
 *
 * 2 限制
 *   无意于实现完整语义的布尔表达式, 暂时先满足于输入校验与orm保存数据时的校验, 因此:
 *   运算符没有优先级, 只能按顺序执行, 不支持带括号的子表达式
 *
 * @author shijianhang
 * @date 2016-10-19 下午3:40:55
 */
interface IValidationExpr
{
	/**
	* 原始表达式
	 */
	val exp:String

	/**
	 * 执行校验表达式
	 * <code>
	 * 	   // 编译
	 *     val exp = ValidationExpr("trim notEmpty email");
	 *     // 执行
	 *     val result = exp.execute(value);
	 * </code>
	 *
	 * @param value 要校验的数值，该值可能被修改
	 * @param variables 变量
	 * @return 最终的数值
	 */
	public fun execute(value:Any?, variables:Map<String, Any?> = emptyMap()): Any?
}
