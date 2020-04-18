package net.jkcode.jkmvc.http.router

import net.jkcode.jkmvc.http.controller.ControllerClass
import net.jkcode.jkmvc.http.controller.ControllerClassLoader
import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.httpLogger
import java.lang.reflect.Method
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
object Router: IRouter
{
	/**
	 * route配置
	 */
	public val config = Config.instance("routes", "yaml")

	/**
	 * 全部路由规则
	 *   有序, 但需要遍历时需倒序, 也就是后添加的路由先匹配
	 *   为什么要倒序? 是因为扫描加载controller类时, 会针对action方法解析路由注解, 并添加对应路由
	 */
	public val routes = LinkedList<Route>()

	/**
	 * 默认路由
	 *    配置文件 routes.yaml 中的 default 路由
	 */
	public lateinit var defaultRoute: Route

	/**
	 * 加载路由配置
	 */
	public fun load() {
		// 1. 加载配置的路由, 包含默认路由
		val props = config.props as Map<String, Map<String, *>>
		for ((name, item) in props) {
			// url正则
			val regex = item["regex"] as String
			// 参数正则
			val paramRegex = item["paramRegex"] as Map<String, String>
			// 参数正则
			val defaults = item["defaults"] as Map<String, String>
			// 添加路由规则
			val route = Route(regex, paramRegex, defaults)
			addRoute(name, route)
			// 识别默认路由
			if(name == "default")
				this.defaultRoute = route
		}

		// 2 加载controller类中注解定义的路由
		// ControllerClassLoader 必须随着 Router 一起加载, 否则会在路由解析时都未加载controller类及其action方法注解
		ControllerClassLoader.load()
	}

	/**
	 * 添加路由
	 * @param name 路由名
	 * @parma route 路由对象
	 */
	public override fun addRoute(name:String, route: Route): Router {
		// 倒序插入: 插入到头部
		routes.addFirst(route);
		httpLogger.info("添加路由[{}]: {}", name, route)
		return this
	}

	/**
	 * 解析路由：匹配规则
	 * @param uri
	 * @param method
	 * @return [路由参数, 路由规则]
	 */
	public override fun parse(uri: String, method: HttpMethod): RouteResult?
	{
		// 1 空uri: 直接取默认路由
		if(uri.isBlank()){
			if(defaultRoute.defaults == null)
				return null
			return RouteResult(defaultRoute.defaults!!, defaultRoute)
		}

		// 2 逐个匹配路由规则
		for(route in routes){
			//匹配路由规则
			val params = route.match(uri, method);
			if(params != null) {
				// 如果是默认路由(方法的路由注解的正则为空), 最后需要匹配方法, 如不匹配则返回null
				if(route == defaultRoute && !matchActionAnnotationMethod(params, method))
					return null

				return RouteResult(params, route); // 路由参数 + 路由规则
			}
		}

		// 3 无匹配
		return null;
	}

	/**
	 * 匹配action方法注解的http方法
	 *   仅在默认路由匹配时调用
	 * @param params 路由参数
	 * @param method
	 * @return
	 */
	private fun matchActionAnnotationMethod(params: Map<String, String>, method: HttpMethod): Boolean {
		// 1 获得controller类
		val clazz: ControllerClass? = ControllerClassLoader.get(params["controller"]!!);
		// 2 获得action方法
		val action: Method? = clazz?.getActionMethod(params["action"]!!);
		// 3 匹配路由注解的方法
		if (action != null) {
			val routeMethod = action.route?.method
			if (routeMethod != null && !routeMethod.match(method)) // 匹配方法
				return false
		}

		return true
	}


}