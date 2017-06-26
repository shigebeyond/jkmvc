package com.jkmvc.http

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
	 * 全部路由规则
	 */
	val routes:MutableMap<String, Route> = HashMap<String, Route>();

	/**
	 * 要跳过的目录
	 * 　　对指定目录下的uri不进行路由解析，主要用于处理静态文件或上传文件
	 */
	public override var skipedDirectories: Array<String>? = null

	/**
	 * 添加路由
	 * @param name 路由名
	 * @parma route 路由对象
	 */
	public override fun addRoute(name:String, route:Route): Router {
		routes[name] = route;
		return this
	}

	/**
	 * 是否跳过对该url的路由解析
	 * 　　对属性 skipedDirectories 指定目录下的uri不进行路由解析，主要用于处理静态文件或上传文件
	 * @return
	 */
	public override fun isSkip(uri:String): Boolean {
		if(skipedDirectories != null)
			for(dir in skipedDirectories!!)
				if(uri.startsWith(dir))
					return true

		return false
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