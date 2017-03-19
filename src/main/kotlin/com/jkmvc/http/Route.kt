package com.jkmvc.http

import java.util.*

/**
 * 路由规则
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-6 上午12:01:17
 *
 */
class Route(protected val regex:String /* 原始正则: <controller>(\/<action>(\/<id>)?)? */, protected val params:Map<String, String>? /* 参数的子正则 */, protected val defaults:Map<String, String>? /* 参数的默认值 */)
{
	companion object{

		/**
		 * 参数的默认正则
		 * @var string
		 */
		protected val REGEX_PARAM = "[^\/]+";

		/**
		 * 将简化的路由正则，转换为完整的正则：主要是将<参数>替换为子正则
		 *
		 * <code>
		 * 	   // 将 <controller>(\/<action>(\/<id>)?)? 编译为 "/(?P<controller>[a-z]+)(\/(?P<action>[a-z]+)\/(?P<id>\d+)?)?/"
		 *     compiled = Route::compile(
		 *        "<controller>(\/<action>(\/<id>)?)?",
		 *         array(
		 *           "controller" => "[a-z]+",
		 *           "action" => "[a-z]+",
		 *           "id" => "\d+",
		 *         )
		 *     );
		 * </code>
		 *
		 * @param array regex 整个uri的正则　
		 * @param array params 参数的正则
		 * @return  string
		 */
		public fun compile(regex:String, params:Map<String, String>? = null):Pair<String, Map<String, Int>>
		{
			// 将<参数>替换为对应的带参数的子正则，如将<controller>替换为(?P<controller>[^\/]+)
			// regex = pregreplace("/\<(\w+)\>/", "(?P<1>.+?)", regex); // wrong: 不能采用 params 中自定义的子正则
			regex = pregreplacecallback("/\<(\w+)\>/", fun(matches) use(params){
				return static::compileParam(matches[1], params);
			}, regex);

			// 匹配开头与结尾
			return "/^regex/";
		}

		/**
		 *　编译单个参数的正则
		 *
		 * @param string name 参数名
		 * @param array params 参数的正则
		 * @return string 带参数的子正则
		 */
		public fun compileParam(name, array params = null):String{
			regex = Arr::get(params, name, static::REGEX_PARAM); //　参数的正则
			return "(?P<name>regex)"; // 带参数的子正则
		}
	}



	/**
	 * 编译后正则: /(?P<controller>[^\/]+)(\/(?P<action>[^\/]+)\/(?P<id>\d+)?)?/
	 * @var string
	 */
	protected lateinit var compileRegex:String;

	/**
	 *
	 */
	protected lateinit var paramGroups:Map<String, Int>;


	init{
		val (compileRegex, paramGroups) = compile(regex, params)// 编译简化的路由正则
		this.compileRegex = compileRegex;
		this.paramGroups = paramGroups;
	}

	/**
	 * 检查uri是否匹配路由正则
	 *
	 * @param string uri
	 * @return boolean|array
	 */
	public fun match(uri:String):Map<String, String>?{
		// 去掉两头的/
		uri.detele('/')

		// 匹配uri
		val matches:MatchResult? = compileRegex.toRegex().find(uri)
		if(matches == null)
			return null;

		//返回 默认参数 + 匹配的参数
		val params: MutableMap<String, String> = HashMap<String, String>();
		if(defaults != null)
			params.putAll(defaults);
		for((name, group) in paramGroups){
			params[name] = matches.groupValues[group];
		}
		return params;
	}
}
