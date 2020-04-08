package net.jkcode.jkmvc.http.router

/**
 * 路由参数 + 路由规则
 */
typealias ParamsAndRoute = Pair<Map<String, String>, Route>

/**
 * 路由器
 *	1 加载路由规则
 *	2 解析路由：匹配规则
 *
 * @author shijianhang
 * @date 2016-10-6 上午12:01:17
 *
 */
interface IRouter
{
	/**
	 * 添加路由
	 * @param name 路由名
	 * @parma route 路由对象
	 */
	fun addRoute(name:String, route: Route): Router

	/**
	 * 解析路由：匹配规则
	 * @param uri
	 * @param method
	 * @return [路由参数, 路由规则]
	 */
	fun parse(uri: String, method: HttpMethod): RouteResult?;
}