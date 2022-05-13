package net.jkcode.jkmvc.http.router

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
 * 	   // 将 <controller>(\/<action>(\/<id:\d+>)?)? 编译为 /([^\/]+)(\/([^\/]+)\/(\d+)?)?/
 *     // 其中参数对子正则的映射关系保存在 paramGroupMapping 中
 *     val route = Route(
 *        "<controller>(\/<action>(\/<id>)?)?", // uri正则
 *         mapOf( // 参数的子正则
 *           "controller" to "[a-z]+",
 *           "action" to "[a-z]+",
 *           "id" to "\d+",
 *         )
 *     );
 *     // 或
 *     val route = Route(
 *        "<controller:[a-z]+>(\/<action:[a-z]+>(\/<id:\d+>)?)?" // uri正则，也可以带参数的子正则
 *     );
 * </code>
 *
 *  2 匹配路由正则
 * <code>
 *     route.mathes('welcome/index');
 * </code>
 */
class Route(override val regex:String, // 原始正则: <controller>(\/<action>(\/<id:\d+>)?)?
			override val paramRegex:MutableMap<String, String> = HashMap(), // 参数的子正则
			override val defaults:Map<String, String>? = null, // 参数的默认值
			override val method: HttpMethod = HttpMethod.ALL, // http方法
			override val controller: String? = null, // controller, 仅当方法级注解路由时有效
			override val action: String? = null // action, 仅当方法级注解路由时有效
): IRoute {

	// 仅在方法级注解路由时调用
	constructor(regex:String, // 原始正则: <controller>(\/<action>(\/<id:\d+>)?)?
				method: HttpMethod, // http方法
				controller: String?, // controller, 仅当方法级注解路由时有效
				action: String? // action, 仅当方法级注解路由时有效
	):this(regex, HashMap(), emptyMap(), method, controller, action)

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
	 * 编译后正则: 将 <controller>(\/<action>(\/<id:\d+>)?)? 编译为 /([^\/]+)(\/([^\/]+)\/(\d+)?)?/
	 *   其中参数对子正则的映射关系保存在 paramGroupMapping 中
	 */
	protected var compiledRegex:Regex;

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

	/**
	 * 快速匹配的子串
	 */
	protected var fastSubString: String

	/**
	 * 是否方法级注解的路由
	 */
	public val isMethodLevel: Boolean by lazy{
		controller != null && action != null
	}

	/**
	 * 是否全局配置的路由
	 */
	public val isGlobal: Boolean
		get() = !isMethodLevel

	init {
		if(controller == null && action != null || controller != null && action == null)
			throw IllegalArgumentException("controller/action must all-null or all-not-null")

		// 对参数名加括号，匹配<controller>或<id:\d+>，参数名也变为子正则, 用括号包住
		groupRegex = "<([\\w\\d]+)(:([^>]+))?>".toRegex().replace(regex){ result: MatchResult ->
			val paramName = result.groups[1]!!.value; // 参数名
			val paramReg = result.groups[3]?.value; // 参数子正则
			if(paramReg != null)
				paramRegex[paramName] = paramReg // 记录参数的子正则
			"(<$paramName>)" // 用括号包住, 变为子正则： <controller> 变为 (<controller>)， <id:\d+> 变为 (<id>) 并记录id参数的子正则为\d+
		}
		// 计算子正则的范围
		groupRangs = buildGroupRangs()
		// 构建参数对子正则的映射
		paramGroupMapping = buildParamGroupMapping()
		// 编译参数正则: 将<参数名>替换为对应的带参数的子正则
		var compileRegex = "<([\\w\\d]+)>".toRegex().replace(groupRegex){ result: MatchResult ->
			val paramName = result.groups[1]!!.value; // 参数名
			paramRegex.getOrDefault(paramName, REGEX_PARAM)!! // 替换参数正则
		}
		compileRegex = "^$compileRegex$" // 匹配开头与结尾
		this.compiledRegex = compileRegex.toRegex()

		// 构建快速匹配的子串
		this.fastSubString = buildFastSubString().trim()
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
				val level = levels.removeAt(levels.lastIndex) // 层级 - 1
				rangs[level].end = i // 记录)结束
			}
		}
		return rangs;
	}

	/**
	 * 构建参数对子正则的映射
	 */
	protected fun buildParamGroupMapping(): MutableMap<String, Int> {
		val matches: Sequence<MatchResult> = "<([\\w\\d]+)>".toRegex().findAll(groupRegex)
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
	 * 构建快速匹配的子串
	 *   就是最长的非正则的子串
	 * @return
	 */
	protected fun buildFastSubString(): String {
		// 转为非正则
		val delimiter = "\n" // 换行符作为分隔符, 肯定没有人用换行符来写路由正则
		var noregx = regex.replace("\\(.+\\)\\??".toRegex(), delimiter) // 去掉()子表达式
		noregx = noregx.replace("<[\\w\\d]+>".toRegex(), delimiter) // 去掉<参数>
		noregx = noregx.replace("[\\\\*+\\[\\](){}\\\$.?\\^|]".toRegex(), delimiter) // 去掉正则符号

		// 拆分为非正则的子串
		val substrs = noregx.split(delimiter)
		// 取最长的子串
		return substrs.maxBy {
			it.length
		}!!
	}

	/**
	 * 快速匹配
	 *    尝试用子串先快速匹配一下, 减少正则匹配
	 *    如果快速匹配不通过, 则不用进行正则匹配了
	 *
	 * @param uri
	 * @return
	 */
	protected fun fastMatch(uri: String): Boolean {
		return fastSubString.isEmpty() // 无子串, 直接通过
				|| uri.contains(fastSubString) // 匹配子串
	}

	/**
	 * 检查uri是否匹配路由正则
	 *
	 * @param uri
	 * @param method
	 * @return 匹配的路由参数，如果为null，则没有匹配
	 */
	public override fun match(uri: String, method: HttpMethod):Map<String, String>?{
		// 1 匹配方法
		if(!this.method.match(method))
			return null

		// 2 先快速匹配子串
		if(!fastMatch(uri))
			return null

		// 3 匹配正则
		val matches:MatchResult? = compiledRegex.find(uri)
		if(matches == null)
			return null

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