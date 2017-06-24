package com.jkmvc.http

import com.jkmvc.common.trim
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
	* 根url，不作为路由解析
	 */
	public override var baseUri:String = "/";

	/**
	 * 全部路由规则
	 */
	val routes:MutableMap<String, Route> = HashMap<String, Route>();

	/**
	 * 添加路由
	 * @param name 路由名
	 * @parma route 路由对象
	 */
	public override fun addRoute(name:String, route:Route):Unit{
		routes[name] = route;
	}

	/**
	 * 解析路由：匹配规则
	 * @param uri
	 * @return [路由参数, 路由规则]
	 */
	public override fun parse(uri:String):Pair<Map<String, String>, Route>?
	{
		val cleanUri = uri.trim(baseUri, "/");
		// 逐个匹配路由规则
		for((name, route) in routes){
			//匹配路由规则
			val params = route.match(cleanUri);
			if(params != null)
				return Pair(params, route);
		}

		return null;
	}

}