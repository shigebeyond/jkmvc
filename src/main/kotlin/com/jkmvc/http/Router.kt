package com.jkmvc.http

import java.util.*

/**
 * 路由器
 *	1 加载路由规则
 *	2 解析路由：匹配规则
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-6 上午12:01:17
 *
 */
class Router
{
	companion object{
		/**
		 * 全部路由规则
		 * @var array
		 */
		protected val routes:MutableMap<String, Route> = HashMap<String, Route>();

		public fun addRoute(name:String, route:Route){
			routes[name] = route;
		}

		/**
		 * 解析路由：匹配规则
		 * @param string uri
		 * @return array|boolean [路由参数, 路由规则]
		 */
		public fun parse(uri:String):Pair<Map<String, String>, Route>?
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

	

}