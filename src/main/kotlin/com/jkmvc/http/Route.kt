package com.jkmvc.http

import com.jkmvc.common.getOrDefault
import com.jkmvc.common.trim
import java.util.*

/**
 * 子正则的范围
 */
data class GroupRange(var start:Int, var end:Int){

	/**
	 * 是否包含某个范围
	 */
	public fun contains(paramRange: IntRange): Boolean {
		return start <= paramRange.first &&  end >= paramRange.last
	}
}

/**
 * 路由正则处理
 *
 *  1 编译正则
 *  将简化的路由正则，转换为完整的正则：主要是将<参数>替换为子正则
 *
 * <code>
 * 	   // 将 <controller>(\/<action>(\/<id>)?)? 编译为 /([^\/]+)(\/([^\/]+)\/(\d+)?)?/
 *     // 其中参数对子正则的映射关系保存在 paramGroupMapping 中
 *     val routeRegex = RouteRegex(
 *        "<controller>(\/<action>(\/<id>)?)?",
 *         mapOf(
 *           "controller" to "[a-z]+",
 *           "action" to "[a-z]+",
 *           "id" to "\d+",
 *         )
 *     );
 *
 *  2 匹配路由正则
 *
 * </code>
 *
 * @param array regex 整个uri的正则　
 * @param array params 参数的正则
 * @return  string
 */
class Route(protected val regex:String /* 原始正则: <controller>(\/<action>(\/<id>)?)? */, protected val paramRegex:Map<String, String>? /* 参数的子正则 */, protected val defaults:Map<String, String>? /* 参数的默认值 */){

	companion object{
		/**
		 * 参数的默认正则
		 * @var string
		 */
		protected val REGEX_PARAM = "[^\\/]+";
	}

	/**
	 *  对参数加括号，参数也变为子正则
	 */
	protected lateinit var groupRegex:String;

	/**
	 * 编译后正则: 将 <controller>(\/<action>(\/<id>)?)? 编译为 /([^\/]+)(\/([^\/]+)\/(\d+)?)?/
	 *   其中参数对子正则的映射关系保存在 paramGroupMapping 中
	 * @var string
	 */
	protected lateinit var compileRegex:String;

	/**
	 * 子正则的范围
	 */
	protected lateinit var groupRangs:List<GroupRange>

	/**
	 * 参数对子正则的映射
	 *   key是参数名
	 *   value是子正则的序号
	 */
	protected lateinit var paramGroupMapping: MutableMap<String, Int>


	init {
		// 对参数加括号，参数也变为子正则
		groupRegex = "<\\w+>".toRegex().replace(regex){ result: MatchResult ->
			"(${result.value})"
		}
		// 计算子正则的范围
		groupRangs = buildGroupRangs()
		// 构建参数对子正则的映射
		paramGroupMapping = buildParamGroupMapping()
		// 编译参数正则: 将<参数>替换为对应的带参数的子正则
		compileRegex = "<(\\w+)>".toRegex().replace(groupRegex){ result: MatchResult ->
			val paramName = result.groups[1]!!.value; // 参数名
			paramRegex.getOrDefault(paramName, REGEX_PARAM)!! // 替换参数正则
		}
		compileRegex = "^$compileRegex$" // 匹配开头与结尾
	}

	/**
	 * 计算子正则的范围
	 *  即()的开始与结束位置
	 */
	protected fun buildGroupRangs():List<GroupRange>{
		// 计算所有子正则的范围
		val rangs:MutableList<GroupRange> = ArrayList<GroupRange>();
		var level:Int = -1;
		for(i in 0..(groupRegex.length - 1)){
			if(groupRegex[i] == '('){ // 子正则开始
				rangs.add(GroupRange(i, -1))
				level++;
			}else if(groupRegex[i] == ')'){ // 子正则结束
				rangs[level].end = i
				level--;
			}
		}
		return rangs;
	}

	/**
	 * 构建参数对子正则的映射
	 */
	public fun buildParamGroupMapping(): MutableMap<String, Int> {
		val matches: Sequence<MatchResult> = "<(\\w+)>".toRegex().findAll(groupRegex)
		val mapping:MutableMap<String, Int> = HashMap<String, Int>();
		for(m in matches){
			val param = m.groups[1]!!
			val paramRange = param.range // 参数的范围
			val paramName = param.value // 参数名
			val i = findGroupRang(paramRange) // 找到参数在哪个子正则中
			mapping[paramName] = i;
		}

		return mapping;
	}

	/**
	 * 找到参数在哪个子正则中
	 */
	protected fun findGroupRang(paramRange: IntRange): Int {
		var result:Int = -1
		for (i in groupRangs.indices){
			if (groupRangs[i].contains(paramRange))
				result = i
		}
		return result;
	}

	/**
	 * 检查uri是否匹配路由正则
	 *
	 * @param string uri
	 * @return boolean|array
	 */
	public fun match(uri:String):Map<String, String>?{
		// 匹配uri
		val matches:MatchResult? = compileRegex.toRegex().find(uri.trim("/")) // 去掉两头的/
		if(matches == null)
			return null;

		//返回 默认参数值 + 匹配的参数值
		val params: MutableMap<String, String> = HashMap<String, String>();
		if(defaults != null){ // 默认参数值
			params.putAll(defaults);
		}
		for((name, group) in paramGroupMapping){ // 匹配的参数值
			params[name] = matches.groupValues[group];
		}
		return params;
	}
}