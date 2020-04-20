package net.jkcode.jkmvc.http

import net.jkcode.jkmvc.http.controller.Controller
import net.jkcode.jkmvc.http.controller.ControllerClass
import net.jkcode.jkmvc.http.controller.ControllerClassLoader
import net.jkcode.jkmvc.http.router.HttpMethod
import net.jkcode.jkmvc.http.router.RouteException
import net.jkcode.jkmvc.http.router.RouteResult
import net.jkcode.jkmvc.http.router.Router
import net.jkcode.jkutil.common.enumeration
import net.jkcode.jkutil.common.toNullable
import net.jkcode.jkutil.common.trim
import net.jkcode.jkutil.validator.RuleValidator
import java.net.URI
import java.util.*
import javax.servlet.RequestDispatcher
import javax.servlet.ServletContext
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import kotlin.collections.HashMap
import kotlin.collections.Map
import kotlin.collections.MutableMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.firstOrNull
import kotlin.collections.iterator
import kotlin.collections.set

/**
 * 请求对象
 *    1 上传请求
 *    对上传请求中文本字段, 在 servlet3 中跟在普通请求中一样, 直接使用 parameterMap / getParameter() 来获取, 因此不用改写 parameterMap / getParameter()
 *    对上传请求中文件字段, 设计单独的api来获取: partFileMap/partFileNames/getPartFile()/getPartFileValues()
 *    2 参数获取
 *    先取get/post参数, 再取路由参数, 防止重名的情况
 *
 * @author shijianhang
 * @date 2016-10-6 上午9:27:56
 *
 */
class HttpRequest(req:HttpServletRequest): MultipartRequest(req)
{
	companion object{

		/**
		 * 可信任的代理服务器ip
		 */
		public val proxyips = arrayOf("127.0.0.1", "localhost", "localhost.localdomain");

		/**
		 * 获得当前请求
		 */
		@JvmStatic
		public fun current(): HttpRequest {
			return currentOrNull() ?: throw IllegalStateException("当前非http环境")
		}

		/**
		 * 获得当前请求
		 */
		@JvmStatic
		public fun currentOrNull(): HttpRequest? {
			return Controller.currentOrNull()?.req
		}
	}

	init{
		// 中文编码
		req.characterEncoding = "UTF-8";
	}

	/*************************** ServletContext改写 *****************************/
	/**
	 * 全局的ServletContext
	 *   fix bug: jetty异步请求后 req.contextPath/req.servletContext 居然为null, 因此直接在 JkFilter.init() 时记录ServletContext(包含contextPath), 反正他是全局不变的
	 */
	public val globalServletContext: ServletContext
		get() = JkFilter.instance().servletContext

	/**
	 * 改写 servletContext
	 *   fix bug: jetty异步请求后 req.contextPath 居然为null, 直接使用全局 contextPath
	 */
	override fun getServletContext(): ServletContext {
		return super.getServletContext() ?: globalServletContext
	}

	/**
	 * 改写 contextPath
	 *   fix bug:
	 *   1 jetty异步请求后, req.contextPath 居然为null
	 *   2 内部请求时, req.contextPath 不为null, 但是空字符串, 叼
	 *   => 直接使用全局 contextPath
	 */
	public override fun getContextPath(): String{
		///return super.getContextPath() ?: globalServletContext.contextPath
		return globalServletContext.contextPath
	}

	/**
	 * 改写 getRequestDispatcher()
	 *   fix bug: jetty异步请求后的 req.servletContext 是 null, 因此 req.getRequestDispatcher() 也是 null, 因为他内部调用 req.servletContext => 直接使用全局 servletContext
	 */
	public override fun getRequestDispatcher(path: String): RequestDispatcher{
		return super.getRequestDispatcher(path) ?: globalServletContext.getRequestDispatcher(path)
	}

	/*************************** 路由解析与路由参数 *****************************/
	/**
	 * http方法
	 */
	public val httpMethod: HttpMethod = HttpMethod.valueOf(method.toUpperCase())

	/**
	 * 路由uri, 要进行路由解析
	 *   1 去掉头部的contextPath + filter url前缀
	 *   2 去掉末尾的/
	 */
	public val routeUri:String = if(contextPath == null)
									throw RouteException("req.contextPath is null")
								else
									//requestURI.trim(contextPath + '/', "/")
									requestURI.trim(contextPath + JkFilter.instance().urlPrefix).trim("/", "/")

	/**
	 * 当前匹配的路由结果
	 */
	public lateinit var routeResult: RouteResult

	/**
	 * 当前匹配的路由参数
	 */
	public val routeParams:Map<String, String>
		get() = routeResult.params

	/**
	 * 解析路由
	 * @return
	 */
	public fun parseRoute(): Boolean {
		// 解析路由
		val result = Router.parse(routeUri, httpMethod);

		if(result != null){
			this.routeResult = result
			return true;
		}

		return false;
	}

	/**
	 * 获得当前controller
	 * @return
	 */
	public val controller: String
		get() = routeResult.controller

	/**
	 * 获得当前controller类
	 * @return
	 */
	public val controllerClass: ControllerClass
		get() = ControllerClassLoader.get(controller)!!

	/**
	 * 获得当前action
	 * @return
	 */
	public val action: String
		get() = routeResult.action

	/*************************** 合并http参数+路由参数 *****************************/
	/**
	 * 原始的请求参数
	 */
	public val httpParams: Map<String, Array<String>>
		get() = req.getParameterMap()

	/**
	 * 全部参数 = 路由参数 + 请求参数
	 */
	protected val allParams: Map<String, Array<String>> by lazy{
		mergeRouteParams()
	}

	/**
	 * 合并路由参数
	 * @return
	 */
	protected fun mergeRouteParams(): MutableMap<String, Array<String>> {
		if(routeParams.isEmpty())
			return req.parameterMap

		// 1 请求参数
		val result = HashMap(req.parameterMap)
		// 2 逐个合并路由参数
		for((k, v) in routeParams){
			// 全局配置路由: 不合并controller/action, 防止覆盖http请求参数
			// 方法级注解路由: 压根没有获得controller/action路由参数的需求, 因为你在目标方法中开发了, 不至于反过来问是哪个方法
			if(!(routeResult.route.isGlobal && (k == "controller" || k == "action")))
				result[k] = arrayOf(v)
		}
		return result
	}

	override fun getParameter(key: String): String? {
		//return routeParams[key] ?: super.getParameter(key) // 当结果为null时报错: super.getParameter(key) must not be null
		return getParameterValues(key)?.firstOrNull()
	}

	override fun getParameterMap(): Map<String, Array<String>> {
		return allParams
	}

	override fun getParameterNames(): Enumeration<String> {
		return allParams.keys.enumeration()
	}

	override fun getParameterValues(key: String): Array<String>? {
		return allParams[key]
	}

	/*************************** 参数的获取/判断/校验 *****************************/
	/**
	 * 检查是否有请求参数(包含路由参数)
	 * @param key
	 * @return
	 */
	public fun contains(key: String): Boolean {
		return getParameter(key) != null
	}

	/**
	 * 检查get/post参数是否为空
	 *
	 * @param key 参数名
	 * @return
	 */
	public fun isEmpty(key: String): Boolean {
		return getParameter(key).isNullOrEmpty()
	}


	/**
	 * 智能获得get/post参数值
	 *    注：调用时需明确指定返回类型，来自动转换参数值为指定类型
	 *
	 * <code>
	 *     val id:Int? = req["id"]
	 *     // 或
	 *     val id = req["id"] as Int?
	 *
	 *     // 相当于
	 *     var id:Int? = req.getInt["id"]
	 *     if(id == null)
	 *        id = req.getInt["id"]
	 * </code>
	 *
	 * @param key 参数名
	 * @param defaultValue 默认值
	 * @return
	 */
	public operator inline fun <reified T:Any> get(key: String, defaultValue: T? = null): T? {
		return getParameter(key)?.toNullable(T::class, defaultValue)
	}

	/**
	 * 智能获得非空get/post参数值，先从路由参数中取，如果没有，则从get/post参数中取
	 *    注：调用时需明确指定返回类型，来自动转换参数值为指定类型
	 *
	 * @param key 参数名
	 * @return
	 */
	public inline fun <reified T:Any> getNotNull(key: String): T{
		return get(key)
				?: throw IllegalArgumentException("缺少请求参数[$key]")
	}

	/**
	 * 获得并校验get/post参数值
	 *   注：调用时需明确指定返回类型，来自动转换参数值为指定类型
	 *
	 * @param key 参数名
	 * @param rule 校验规则
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public inline fun <reified T:Any> getAndValidate(key: String, rule: String, defaultValue: T?): T? {
		return validateValue(key, getParameter(key), rule).toNullable(T::class, defaultValue)
	}

	/**
	 * 用于校验的参数
	 *    将value类型Array<String?>转为String
	 */
	public val validatingParams:Map<String, String?> by lazy{
		HttpParamMap(allParams)
	}

	/**
	 * 校验get/post参数值
	 *
	 * @param key 参数名
	 * @param value 参数值
	 * @param rule 校验规则
	 * @return
	 */
	protected inline fun validateValue(key: String, value: Any?, rule: String): String? {
		// 规则校验器
		val validator = RuleValidator(key, rule)
		// null直接返回
		if(value == null && !validator.containsNotNullFunction())
			return null

		// 校验单个字段: 字段值可能被修改
		val result = validator.validate(value, validatingParams)
		return result.getOrThrow() as String
	}

	/**
	 * 获得int类型的请求参数(包含路由参数)
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getInt(key: String, defaultValue: Int? = null): Int? {
		return get(key, defaultValue)
	}

	/**
	 * 获得long类型的请求参数(包含路由参数)
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getLong(key: String, defaultValue: Long? = null): Long? {
		return get(key, defaultValue)
	}

	/**
	 * 获得boolean类型的请求参数(包含路由参数)
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getBoolean(key: String, defaultValue: Boolean? = null): Boolean? {
		return get(key, defaultValue)
	}

	/**
	 * 获得Date类型的请求参数(包含路由参数)
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getDate(key: String, defaultValue: Date? = null): Date? {
		return get(key, defaultValue)
	}

	/**
	 * 获得float类型的请求参数(包含路由参数)
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getFloat(key: String, defaultValue: Float? = null): Float? {
		return get(key, defaultValue)
	}

	/**
	 * 获得double类型的请求参数(包含路由参数)
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getDouble(key: String, defaultValue: Double? = null): Double? {
		return get(key, defaultValue)
	}

	/**
	 * 获得short类型的请求参数(包含路由参数)
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getShort(key: String, defaultValue: Short? = null): Short? {
		return get(key, defaultValue)
	}

	/*************************** 其他 *****************************/
	/**
	 * 来源url
	 */
	public val referer: String? by lazy {
		req.getHeader("referer")
	}

	/**
	 * 来源主机
	 */
	public val refererHost: String? by lazy{
		if(referer.isNullOrBlank())
			null
		else
			URI(referer).host
	}

	/**
	 * 登录后要跳转的url
	 */
	public var redirectUrl: String?
		// 1 未登录时记录当前url为要跳转的url 2 登录跳转后删除
		set(value) {
			if(value == null) // null则删除
				session.removeAttribute("_redirectUrl")
			else
				session.setAttribute("_redirectUrl", value)
		}
		get(){
			return session.getAttribute("_redirectUrl") as String?
		}

	/**
	 * 将当前url作为登录后要跳转的url
	 */
	public fun asRedirectUrl(){
		redirectUrl = requestURL.toString()
	}

	/**
	 * 真正登录后要跳转的url
	 */
	public val savedUrl: String?
		get() = redirectUrl ?: referer

	/**
	 * 会话id
	 */
	public val sessionId: String?
		get() = getCookie("JSESSIONID")?.value

	/**
	 * 获得
	 */
	public val cookiesMap: Map<String, Cookie>
		get(){
			return req.cookies.associate { cookie ->
				cookie.name to cookie
			}
		}

	/**
	 * 获得cookie值
	 *
	 * <code>
	 *     val theme = Cookie::get("theme", "blue");
	 * </code>
	 *
	 * @param  key        cookie名
	 * @param  default    默认值
	 * @return
	 */
	public fun getCookie(name:String): Cookie? {
		return this.cookies?.firstOrNull{
			it.name == name
		}
	}

	/**
	 * 将相对路径转为绝对路径
	 * @param uri 相对路径
	 * @param withContextPath 是否要上 contextPath
	 *        如果是java一般为true
	 *        如果是jsp的url一般是相对于server根节点, 因此一般为false
	 * @return 绝对路径
	 */
	public fun absoluteUrl(uri:String, addContextPath: Boolean = true):String
	{
		if(uri.startsWith("http"))
			return uri;

		val cp = if(addContextPath) contextPath else ""
		val delimiter = if(uri.startsWith('/')) "" else "/"
		return serverUrl + cp + delimiter + uri
	}

	/**
	 * 获得web根目录的路径
	 * @return
	 */
	public val webRootDirectory: String
		get() = servletContext.getRealPath("/");

	/**
	 * 对query string进行 null 转 空字符串
	 * @return
	 */
	public override fun getQueryString(): String {
		return super.getQueryString() ?: ""
	}

	/**
	 * 构建curl命令
	 * @return
	 */
	public fun buildCurlCommand(): String {
		/*// get请求
		if(isGet())
			return "curl '${requestURL}?${queryString}'";

		// post请求
		return "curl -d '${parameterMap.toQueryString()}' '${requestURL}?${queryString}'"
		*/
		// curl命令
		val cmd = StringBuilder("curl ")

		// 方法
		if ("GET".equals(method, ignoreCase = true))
			cmd.append("-G ")

		// 路径: '$url'
		cmd.append('\'').append(requestURL).append('?').append(queryString ?: "").append('\'')

		//请求头： -H '$k:$v' -H '$k:$v'
		val headerNames = headerNames
		for(k in headerNames){
			val v = getHeader(k)
			// -H '$k:$v'
			cmd.append("-H '").append(k).append(':').append(v).append("' ")
		}

		// post参数： -d '$k=$v&$k=$v&'
		if (isPost()) {
			//-d '
			cmd.append("-d '")
			for(k in headerNames){
				val v = getParameter(k)
				// $k=$v&
				cmd.append(k).append('=').append(v).append('&')
			}
			// '
			cmd.append("' ")
		}

		return cmd.toString()
	}
}
