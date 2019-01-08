package com.jkmvc.validator

import java.util.*
import java.util.concurrent.ConcurrentHashMap

// 规则表达式的运算单位： 1 函数名 2 函数参数
private typealias SubRule = Pair<String, Array<String>>

/**
 * 规则校验器, 由一个规则表达式, 来表达校验逻辑
 *
 * 1 格式
 *    规则表达式是由多个(函数调用的)规则子表达式组成, 规则子表达式之间以空格分隔, 格式为 a(1) b(1,"2") c(3,4)
 *    规则子表达式是函数调用, 格式为 a(1,"2")
 *
 * 2 限制
 *   无意于实现完整语义的布尔表达式, 暂时先满足于输入校验与orm保存数据时的校验, 因此:
 *   运算符没有优先级, 只能按顺序执行, 不支持带括号的规则子表达式
 *
 * @author shijianhang
 * @date 2016-10-19 下午3:40:55
 */
class RuleValidator(public val label: String /* 值的标识, 如orm中的字段名, 如请求中的表单域名 */,
					public val rule: String /* 规则表达式 */

) : IValidator {

	companion object{

		/**
		 * 函数参数的正则
		 */
		protected val REGEX_PARAM: Regex = ("([\\w\\d-:\"]+),?").toRegex();

		/**
		 * 缓存编译后的规则子表达式
		 */
		protected val compiledSubRules: ConcurrentHashMap<String, List<SubRule>> = ConcurrentHashMap();

		/**
		 * 编译规则表达式
		 *     规则表达式是由多个(函数调用的)规则子表达式组成, 规则子表达式之间以空格分隔, 格式为 a(1) b(1,2) c(3,4)
		 * <code>
		 *     val subRules = ValidationExpr::compileSubRules("trim notEmpty email");
		 * </code>
		 *
		 * @param rule
		 * @return
		 */
		public fun compileSubRules(rule:String): List<SubRule> {
			if(rule.isEmpty())
				return emptyList()

			// 规则子表达式之间以空格分隔, 格式为 a(1) b(1,2) c(3,4)
			val subRules = rule.split(" ")
			return subRules.map { subRule ->
				// 规则子表达式是函数调用, 格式为 a(1,2)
				if(subRule.contains('(')){
					val (func, params) = subRule.split('.');
					SubRule(func, compileParams(params))
				}else{
					SubRule(subRule, emptyArray())
				}
			}
		}

		/**
		 * 编译函数参数
		 *
		 * @param params
		 * @return
		 */
		public fun compileParams(exp: String): Array<String> {
			val matches: Sequence<MatchResult> = REGEX_PARAM.findAll(exp);
			val result: ArrayList<String> = ArrayList();
			for(m in matches)
				result.add(m.groups[1]!!.value)

			return result.toArray() as Array<String>
		}
	}

	/**
	 * 规则子表达式的数组
	 *   一个规则子表达式 = listOf(函数名, 参数数组)
	 *   参数数组 = listOf("1", "2", ":name") 参数有值/变量（如:name）
	 */
	protected val subRules:List<SubRule> = compiledSubRules.getOrPut(rule){
		compileSubRules(rule)
	}

	/**
	 * 执行规则表达式
	 * <code>
	 * 	   // 编译
	 *     val rule = ValidationExpr("trim notEmpty email");
	 *     // 执行
	 *     val result = rule.validate(value);
	 * </code>
	 *
	 * @param value 要校验的数值，该值可能被修改
	 * @param variables 变量
	 * @return 最终的数值
	 */
	public override fun validate(value:Any?, variables:Map<String, Any?>): Any? {
		if(subRules.isEmpty())
			return value

		// 逐个运算规则子表达式
		var result:Any? = value
		for (subRule in subRules)
			result = executeSubRule(subRule, result, variables, label);
		return result
	}

	/**
	 * 运算规则子表达式
	 *
	 * @param subRule 规则子表达式
	 * @param value 要校验的值
	 * @param variables 变量
	 * @param label 值的标识, 如orm中的字段名, 如请求中的表单域名
	 * @return
	 */
	protected fun executeSubRule(subRule: SubRule, value: Any?, variables: Map<String, Any?>, label: String): Any? {
		// 获得 1 函数名 2 函数参数
		val (func, params) = subRule
		// 调用校验方法
		return ValidateFunc.get(func).execute(value, params, variables, label)
	}
}
