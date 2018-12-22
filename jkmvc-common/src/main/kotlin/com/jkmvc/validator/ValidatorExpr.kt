package com.jkmvc.validator

import java.util.*
import java.util.concurrent.ConcurrentHashMap

// 校验表达式运算单位： 1 函数名 2 函数参数
private typealias ValidatorExprUnit = Pair<String, Array<String>>

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
class ValidatorExpr protected constructor(override val exp:String /* 原始表达式 */):IValidationExpr {

	companion object{

		/**
		 * 函数参数的正则
		 */
		protected val REGEX_PARAM: Regex = ("([\\w\\d-:]+),?").toRegex();

		/**
		 * 缓存编译后的表达式
		 */
		protected val expsCached: ConcurrentHashMap<String, ValidatorExpr> = ConcurrentHashMap();

		/**
		 * 获得编译后的校验表达式
		 */
		public fun instance(exp: String): ValidatorExpr{
			return expsCached.getOrPut(exp){
				ValidatorExpr(exp);
			}
		}

		/**
		 * 编译表达式
		 *     表达式是由多个(函数调用的)子表达式组成, 子表达式之间以空格分隔, 格式为 a(1) b(1,2) c(3,4)
		 * <code>
		 *     val subexps = ValidationExpr::compile("trim notEmpty email");
		 * </code>
		 *
		 * @param exp
		 * @return
		 */
		public fun compile(exp:String): List<ValidatorExprUnit> {
			// 子表达式之间以空格分隔, 格式为 a(1) b(1,2) c(3,4)
			val subexps = exp.split(" ")
			return subexps.map { subexp ->
				// 子表达式是函数调用, 格式为 a(1,2)
				if(subexp.contains('(')){
					val (func, params) = subexp.split('.');
					ValidatorExprUnit(func, compileParams(params))
				}else{
					ValidatorExprUnit(subexp, emptyArray())
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
			val result:ArrayList<String> = ArrayList();
			for(m in matches)
				result.add(m.groups[1]!!.value)

			return result.toArray() as Array<String>
		}
	}

	/**
	 * 子表达式的数组
	 *   一个子表达式 = listOf(函数名, 参数数组)
	 *   参数数组 = listOf("1", "2", ":name") 参数有值/变量（如:name）
	 */
	protected val subexps:List<ValidatorExprUnit> = compile(exp);

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
	public override fun execute(value:Any?, variables:Map<String, Any?>): Any? {
		if(subexps.isEmpty())
			return value

		// 逐个运算子表达式
		var result:Any? = value
		for (subexp in subexps)
			result = executeSubexp(subexp, result, variables);
		return result
	}

	/**
	 * 运算子表达式
	 *
	 * @param subexp 子表达式
	 * @param value 要校验的值
	 * @param variables 变量
	 * @return
	 */
	protected fun executeSubexp(subexp: ValidatorExprUnit, value: Any?, variables: Map<String, Any?>): Any? {
		// 获得 1 函数名 2 函数参数
		val (func, params) = subexp
		// 调用校验方法
		return ValidatorFunc.get(func).execute(value, params, variables)
	}
}
