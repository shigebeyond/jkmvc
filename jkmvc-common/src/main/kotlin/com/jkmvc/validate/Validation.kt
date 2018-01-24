package com.jkmvc.validate

import com.jkmvc.common.Config
import java.util.concurrent.ConcurrentHashMap

/**
 * 校验器
 *   其校验方法是要被ValidationUnit调用的，通过反射来调用，反射时不能识别参数的默认值，因此在定义校验方法时不要设置参数默认值
 *
 * @author shijianhang
 * @date 2016-10-20 下午2:20:13  
 *
 */
object Validation:IValidation
{
	/**
	 * 校验方法对应的错误消息
	 */
	public val messages: Config = Config.instance("validation-messages")

	/**
	 * 缓存编译后的表达式
	 */
	private val expsCached: ConcurrentHashMap<String, ValidationExpression> = ConcurrentHashMap<String, ValidationExpression>();

	/**
	 * 编译与执行校验表达式
	 *
	 * @param exp 校验表达式
	 * @param value 要校验的数值，该值可能被修改
	 * @param variables 变量
	 * @return
	 */
	public override fun execute(exp:String, value:Any?, variables:Map<String, Any?>): ValidationResult
	{
		// 编译
		val expCompiled = expsCached.getOrPut(exp){
			ValidationExpression(exp);
		}
		// 执行
		return expCompiled.execute(value, variables);
	}

	/**
	 * 获得消息
	 * @param key
	 * @return
	 */
	public override fun getMessage(key:String):String?{
		if(messages.containsKey(key))
			return messages[key];

		return null;
	}

	/**
	 * 检查非空
	 *
	 * @param value
	 * @return
	 */
	public fun notEmpty(value:Any?): Boolean {
		return value != null && !(value is String && value.isEmpty());
	}

	/**
	 * 检查长度
	 *
	 * @param value
	 * @param min 最小长度
	 * @param max 最大长度，如果为-1，则不检查最大长度
	 * @return
	 */
	public fun length(value:String, min:Int, max:Int): Boolean {
		val len = value.length
		return len >= min && (max > -1 || len <= max);
	}

	/**
	 * 检查最小值
	 *
	 * @param value
	 * @param min 最小值
	 * @param max 最大值
	 * @return
	 */
	public fun min(value:Int, min:Int): Boolean {
		return value >= min
	}

	/**
	 * 检查最大值
	 *
	 * @param value
	 * @param min 最小值
	 * @param max 最大值
	 * @return
	 */
	public fun max(value:Int, max:Int): Boolean {
		return value <= max
	}

	/**
	 * 检查是否在某个范围内
	 *
	 * @param value
	 * @param min 最小值
	 * @param max 最大值
	 * @return
	 */
	public fun between(value:Int, min:Int, max:Int): Boolean {
		return value >= min && value <= max
	}

	/**
	 * 检查是否在某个范围内
	 *
	* @param value
	* @param min 最小值
	* @param max 最大值
	* @param step 步长
	* @return
	*/
	public fun range(value:Int, min:Int, max:Int, step:Int): Boolean {
		return (value >= min && value <= max) // 是否在范围内
				&& ((value - min) % step === 0); // 是否间隔指定步长
	}

	/**
	 * 检查是否邮件格式
	 * @param value
	 * @return
	 */
	public fun email(value:String): Boolean {
		return "^[\\w\\-\\.]+@[\\w\\-]+(\\.\\w+)+".toRegex().matches(value);
	}

	/**
	 * 检查是否数字，不包含.-
	 */
	public fun digit(value:String): Boolean{
		return "^\\d+$".toRegex().matches(value);
	}

	/**
	 * 检查是否数值，包含.-
	 */
	public fun numeric(value:String): Boolean{
		return "^-?\\d+(\\.\\d+)?$".toRegex().matches(value);
	}

	/**
	 * 检查字符串是否相等
	 */
	public fun strEquals(value:String, other: String, ignoreCase: Boolean = false): Boolean {
		return value.equals(other, ignoreCase)
	}

	/**
	 * 检查字符串是否有前缀
	 */
	public fun startsWith(value:String, prefix: String, ignoreCase: Boolean = false): Boolean {
		return value.startsWith(prefix, ignoreCase);
	}

	/**
	 * 检查字符串是否有后缀
	 */
	public fun endsWith(value:String, suffix: String, ignoreCase: Boolean = false): Boolean {
		return value.endsWith(suffix, ignoreCase);
	}

	/**
	 * 删除字符串两边的空白字符
	 */
	public fun trim(value:String): String {
		return  value.trim()
	}


	/**
	 * 字符串转换为大写
	 */
	public fun toUpperCase(value:String): String {
		return  value.toUpperCase();
	}

	/**
	 * 字符串转换为小写
	 */
	public fun toLowerCase(value:String): String {
		return  value.toLowerCase();
	}


	/**
	 * 字符串截取子字符串
	 * @param startIndex 开始的位置
	 * @param endIndex 结束的位置，如果为-1，则到末尾
	 * @return
	 */
	public fun substring(value:String, startIndex: Int, endIndex: Int): String {
		return value.substring(startIndex, if(endIndex == -1) value.length else endIndex);
	}
}