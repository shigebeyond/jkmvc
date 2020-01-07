package net.jkcode.jkmvc.http.router

import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.httpLogger
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 路由器
 *	1 加载路由规则
 *	2 解析路由：匹配规则
 *
 * @author shijianhang
 * @date 2016-10-6 上午12:01:17
 *
 */
object Router: IRouter
{
	/**
	 * route配置
	 */
	public val config = Config.instance("routes", "yaml")

	/**
	 * 全部路由规则
	 *   有序, 但需要倒序插入, 也就是后添加的路由先匹配
	 */
	private val routes = LinkedList<Route>()

	init {
		// 加载配置的路由
		val props = config.props as Map<String, Map<String, *>>
		for ((name, item) in props) {
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
	public override fun addRoute(name:String, route: Route): Router {
		// 倒序插入: 插入到尾部
		routes.addLast(route);
		httpLogger.info("添加路由[{}]: {}", name, route)
		return this
	}

	/**
	 * 解析路由：匹配规则
	 * @param uri
	 * @param method
	 * @return [路由参数, 路由规则]
	 */
	public override fun parse(uri: String, method: HttpMethod): ParamsAndRoute?
	{
		// 1 空uri: 直接取默认路由
		if(uri.isBlank()){
			// 由于路由倒序, 则最后一个路由为默认路由, 配置文件 routes.yaml 中的 default 路由
			val defaultRoute = routes.last
			if(defaultRoute == null || defaultRoute.defaults == null)
				return null
			return Pair(defaultRoute.defaults, defaultRoute)
		}

		// 2 逐个匹配路由规则
		for(route in routes){
			//匹配路由规则
			val params = route.match(uri, method);
			if(params != null)
				return Pair(params, route); // 路由参数 + 路由规则
		}

		// 3 无匹配
		return null;
	}

}