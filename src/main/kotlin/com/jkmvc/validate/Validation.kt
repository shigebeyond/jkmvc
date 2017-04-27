package com.jkmvc.validate

import java.util.*

/**
 * 校验器
 *
 * @Package packagename 
 * @category 
 * @author shijianhang
 * @date 2016-10-20 下午2:20:13  
 *
 */
object Validation
{
	/**
	 * 校验方法对应的错误消息
	 * @var array
	 */
	val messages:Map<String, String> = mapOf(
			"notempty" to "不能为空",
			"length" to "的长度必须在:0到:1之间",
			"range" to "的数值必须是:0到:1之间的整数"
	);

	/**
	 * 缓存编译后的表达式
	 * @var array
	 */
	val expsCached:MutableMap<String, ValidationExpression> = HashMap<String, ValidationExpression>();

	/**
	 * 编译与执行校验表达式
	 *
	 * @param string exp 校验表达式
	 * @param unknown value 要校验的数值，该值可能被修改
	 * @param array data 其他参数
	 * @return mixed
	 */
	public fun execute(exp:String, value:Any, data:Map<String, Any> = emptyMap()):Any?
	{
		if(exp.isEmpty())
			return value;
		
		// 编译
		val expCompiled = expsCached.getOrPut(exp){
			ValidationExpression(exp);
		}
		// 执行
		val result = expCompiled.execute(value, data);
		
		// 构建结果消息

		return result;
	}
	
	/**
	 * 检查非空
	 * 
	 * @param unknown value
	 * @return bool
	 */
	public fun notEmpty(value:String): Boolean {
		return value.isEmpty();
	}
	
	/**
	 * 检查长度
	 * 
	 * @param unknown value
	 * @param int min
	 * @param int max
	 * @return bool
	 */
	public fun length(value:String, min:Int, max:Int = -1): Boolean {
		val len = value.length
		return len >= min && (max > -1 || len <= max);
	}
	
	/**
	 * 检查是否在某个范围内
	 *
	* @param string value
	* @param int min 最小值
	* @param int max 最大值
	* @param int step 步长
	* @return  bool
	*/
	public fun range(value:Int, min:Int, max:Int, step:Int = 1): Boolean {
		return (value >= min && value <= max) // 是否在范围内
						&& ((value - min) % step === 0); // 是否间隔指定步长
	}
	
	/**
	 * 检查是否邮件格式
	 * @param String value
	 * @return bool
	 */
	public fun email(value:String): Boolean {
		return "^[\\w\\-\\.]+@[\\w\\-]+(\\.\\w+)+".toRegex().matches(value);
	}
	
}