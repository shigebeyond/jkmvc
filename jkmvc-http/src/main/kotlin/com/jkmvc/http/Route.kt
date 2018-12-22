package com.jkmvc.http

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
 *     val route = Route(
 *        "<controller>(\/<action>(\/<id>)?)?",
 *         mapOf(
 *           "controller" to "[a-z]+",
 *           "action" to "[a-z]+",
 *           "id" to "\d+",
 *         )
 *     );
 * </code>
 *
 *  2 匹配路由正则
 * <code>
 *     route.mathes('welcome/index');
 * </code>
 */
class Route(override val regex:String /* 原始正则: <controller>(\/<action>(\/<id>)?)? */,
			override val paramRegex:Map<String, String> = emptyMap() /* 参数的子正则 */,
			override val defaults:Map<String, String>? /* 参数的默认值 */ = null
): IRoute{

	companion object{
		/**
		 * 参数的默认正则
		 */
		protected val REGEX_PARAM = "[^/]+";
	}

	/**
	 *  对参数加括号，参数也变为子正则
	 */
	protected var groupRegex:String;

	/**
	 * 编译后正则: 将 <controller>(\/<action>(\/<id>)?)? 编译为 /([^\/]+)(\/([^\/]+)\/(\d+)?)?/
	 *   其中参数对子正则的映射关系保存在 paramGroupMapping 中
	 */
	protected var compileRegex:String;

	/**
	 * 子正则的范围
	 */
	protected var groupRangs:List<GroupRange>

	/**
	 * 参数对子正则的映射
	 *   key是参数名
	 *   value是子正则的序号
	 */
	protected var paramGroupMapping: MutableMap<String, Int>


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
		val levels:MutableList<Int> = ArrayList<Int>();
		for(i in 0..(groupRegex.length - 1)){
			if(groupRegex[i] == '('){ // 子正则开始
				levels.add(rangs.size); // 层级 + 1
				rangs.add(GroupRange(i + 1, -1)) // 记录(开始
			}else if(groupRegex[i] == ')'){ // 子正则结束
				val level = levels.removeAt(levels.lastIndex) // // 层级 - 1
				rangs[level].end = i // 记录)结束
			}
		}
		return rangs;
	}

	/**
	 * 构建参数对子正则的映射
	 */
	protected fun buildParamGroupMapping(): MutableMap<String, Int> {
		val matches: Sequence<MatchResult> = "<(\\w+)>".toRegex().findAll(groupRegex)
		val mapping:MutableMap<String, Int> = HashMap();
		for(m in matches){
			val param = m.groups[1]!!
			val paramRange = param.range // 参数的范围
			val paramName = param.value // 参数名
			val i = findGroupRang(paramRange) // 找到参数在哪个子正则中
			mapping[paramName] = i + 1; // 匹配结果从1开始
		}

		return mapping;
	}

	/**
	 * 找到参数在哪个子正则中
	 * @param paramRange 参数的范围
	 * @return
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
	 * @param uri
	 * @return 匹配的路由参数，如果为null，则没有匹配
	 */
	public override fun match(uri:String):Map<String, String>?{
		// 匹配uri
		val matches:MatchResult? = compileRegex.toRegex().find(uri)
		if(matches == null)
			return defaults;

		//返回 默认参数值 + 匹配的参数值
		val params: MutableMap<String, String> = HashMap();
		if(defaults != null)// 默认参数值
			params.putAll(defaults);
		for((name, group) in paramGroupMapping){ // 匹配的参数值
			val value = matches.groupValues[group]
			if(value != "")
				params[name] = value;
		}
		return params;
	}

	public override fun toString():String{
		return "regex=$regex, paramRegex=$paramRegex, defaults=$defaults"
	}
}