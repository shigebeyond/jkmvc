package com.jkmvc.http

import com.jkmvc.common.Config
import java.util.*

/**
 * 路由器
 *	1 加载路由规则
 *	2 解析路由：匹配规则
 *
 * @author shijianhang
 * @date 2016-10-6 上午12:01:17
 *
 */
object Router:IRouter
{
	/**
	 * route配置
	 */
	public val config = Config.instance("routes", "yaml")

	/**
	 * 全部路由规则
	 */
	private val routes:MutableMap<String, Route> = HashMap();

	/**
	 * 静态文件uri的正则
	 */
	public override var staticFileRegex: String = ".*\\.(gif|jpg|jpeg|png|bmp|ico|swf|js|css|eot|ttf|woff)$"

	init {
	    // 加载配置的路由
		val props = config.props as Map<String, Map<String, *>>
		for((name, item) in props){
			// url正则
			val regex = item["regex"] as String
			// 参数正则
			val paramRegex = item["paramRegex"] as Map<String, String>
			// 参数正则
			val defaults = item["defaults"] as Map<String, String>
			// 添加路由规则
			addRoute(name, Route(regex, paramRegex, defaults))
		}
	}

	/**
	 * 添加路由
	 * @param name 路由名
	 * @parma route 路由对象
	 */
	public override fun addRoute(name:String, route:Route): Router {
		routes[name] = route;
		httpLogger.info("添加路由[$name]: $route")
		return this
	}

	/**
	 * 解析路由：匹配规则
	 * @param uri
	 * @return [路由参数, 路由规则]
	 */
	public override fun parse(uri:String):Pair<Map<String, String>, Route>?
	{
		// 逐个匹配路由规则
		for((name, route) in routes){
			//匹配路由规则
			val params = route.match(uri);
			if(params != null)
				return Pair(params, route);
		}

		return null;
	}

}