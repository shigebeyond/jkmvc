package com.jkmvc.validate

import java.util.*

/**
 * 校验器
 *   其校验方法是要被ValidationUnit调用的，通过反射来调用，反射时不能识别参数的默认值，因此在定义校验方法时不要设置参数默认值
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
	 * @param array data 变量
	 * @return mixed
	 */
	public fun execute(exp:String, value:Any, binds:Map<String, Any?> = emptyMap()): Pair<Any?, ValidationUint?>
	{
		// 编译
		val expCompiled = expsCached.getOrPut(exp){
			ValidationExpression(exp);
		}
		// 执行
		return expCompiled.execute(value, binds);
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

	/**
	 * 删除两边的空白字符
	 */
	public fun trim(value:String): String {
		return  value.trim()
	}


	/**
	 * 转换为大写
	 */
	public fun toUpperCase(value:String): String {
		return  value.toUpperCase();
	}

	/**
	 * 转换为小写
	 */
	public fun toLowerCase(value:String): String {
		return  value.toLowerCase();
	}


	/**
	 * 截取子字符串
	 * @param int startIndex 开始的位置
	 * @param int endIndex 结束的位置，如果为-1，则到末尾
	 * @return string
	 */
	public fun substring(value:String, startIndex: Int, endIndex: Int): String {
		return value.substring(startIndex, if(endIndex == -1) value.length else endIndex);
	}

	/**
	 * 以..开头
	 */
	public fun startsWith(value:String, prefix: CharSequence, ignoreCase: Boolean = false): Boolean {
		return value.startsWith(prefix, ignoreCase);
	}

	/**
	 * 以..结尾
	 */
	public fun endsWith(value:String, suffix: CharSequence, ignoreCase: Boolean = false): Boolean {
		return value.endsWith(suffix, ignoreCase);
	}
}