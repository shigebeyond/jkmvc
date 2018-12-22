package com.jkmvc.validate

/**
 * 校验器
 *   其校验方法是要被ValidationUnit调用的，通过反射来调用，反射时不能识别参数的默认值，因此在定义校验方法时不要设置参数默认值
 *
 * @author shijianhang
 * @date 2016-10-20 下午2:20:13  
 *
 */
interface IValidation
{
	/**
	 * 编译与执行校验表达式
	 *
	 * @param exp 校验表达式
	 * @param value 要校验的数值，该值可能被修改
	 * @param variables 变量
	 * @return
	 */
	fun execute(exp:String, value:Any?, variables:Map<String, Any?> = emptyMap()): Any?
}