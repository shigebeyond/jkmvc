package net.jkcode.jkmvc.http

import net.jkcode.jkutil.common.*
import net.jkcode.jkmvc.http.controller.Controller
import net.jkcode.jkmvc.http.controller.ControllerClass
import net.jkcode.jkmvc.http.controller.ControllerClassLoader
import net.jkcode.jkmvc.http.router.*
import net.jkcode.jkutil.validator.RuleValidator
import org.apache.commons.collections.map.CompositeMap
import java.net.URI
import java.util.*
import javax.servlet.RequestDispatcher
import javax.servlet.ServletContext
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest

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
	 *   fix bug: jetty异步请求后 req.contextPath 居然为null, 直接使用全局 contextPath
	 */
	public override fun getContextPath(): String{
		return globalServletContext.contextPath
	}

	/**
	 * 改写 getRequestDispatcher()
	 *   fix bug: jetty异步请求后的 req.servletContext 是 null, 因此 req.getRequestDispatcher() 也是 null, 因为他内部调用 req.servletContext => 直接使用全局 servletContext
	 */
	public override fun getRequestDispatcher(path: String): RequestDispatcher{
		return globalServletContext.getRequestDispatcher(path)
	}

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
	 * 全部参数 = 路由参数 + 请求参数
	 *    路由参数的value类型是String, 请求参数的value类型是 Array<String?>, 因此统一改为String
	 */
	public val allParams:Map<String, String?> by lazy{
		CompositeMap(routeParams, HttpParamMap(req.parameterMap)) as Map<String, String>
	}

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

	init{
		// 中文编码
		req.characterEncoding = "UTF-8";
	}

	/*************************** 路由解析 *****************************/
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

	/*************************** 各种判断 *****************************/
	/**
	 * 检查请求参数是否为空
	 *
	 * @param key 参数名
	 * @return
	 */
	public fun isEmpty(key:String):Boolean{
		// 先取路由参数
		if(isEmptyRouteParameter(key))
			return true;

		// 再取get/post参数
		return isEmptyParameter(key);
	}

	/**
	 * 判断请求是否包含指定参数
	 *
	 * @param key 参数名
	 * @return
	 */
	public fun contains(key:String):Boolean{
		return allParams.containsKey(key)
	}

	/**
	 * 智能获得请求参数值，先从路由参数中取得，如果没有，则从get/post参数中取
	 *    注：调用时需明确指定返回类型，来自动转换参数值为指定类型
	 *
	 * <code>
	 *     val id:Int? = req["id"]
	 *     // 或
	 *     val id = req["id"] as Int?
	 *
	 *     // 相当于
	 *     var id:Int? = req.getIntRouteParameter["id"]
	 *     if(id == null)
	 *        id = req.getIntParameter["id"]
	 * </code>
	 *
	 * @param key 参数名
	 * @param defaultValue 默认值
	 * @return
	 */
	public operator inline fun <reified T:Any> get(key: String, defaultValue: T? = null): T?
	{
		// 获得参数值
		var value: String? = getParameter(key) // 先取get/post参数
							?: routeParams[key] // 再取路由参数

		if (value != null)
			return value.to(T::class) as T

		return defaultValue;
	}


	/**
	 * 智能获得非空请求参数值，先从路由参数中取，如果没有，则从get/post参数中取
	 *    注：调用时需明确指定返回类型，来自动转换参数值为指定类型
	 *
	 * @param key 参数名
	 * @return
	 */
	public inline fun <reified T:Any> getNotNull(key: String): T{
		val value: T? = get(key)
		if(value == null)
			throw IllegalArgumentException("缺少请求参数[$key]")
		
		return value
	}

	/**
	 * 智能获得并校验请求参数值，先从路由参数中取，如果没有，则从get/post参数中取
	 *    注：调用时需明确指定返回类型，来自动转换参数值为指定类型
	 *
	 * @param key 参数名
	 * @param rule 校验规则
	 * @param defaultValue 默认值
	 * @return
	 */
	public inline fun <reified T:Any> getAndValidate(key: String, rule: String, defaultValue: T? = null): T?
	{
		// 获得参数值
		var value: String? = getParameter(key) // 先取get/post参数
							?: routeParams[key] // 再取路由参数

		// 校验参数值
		if (value != null)
			return validateValue(key, value, rule).toNullable(T::class)

		return defaultValue
	}

	/**
	 * 校验请求值
	 *
	 * @param key 参数名
	 * @param value 参数值
	 * @param rule 校验规则
	 * @return
	 */
	protected inline fun validateValue(key: String, value: Any?, rule: String): String {
		// 校验单个字段: 字段值可能被修改
		val result = RuleValidator(key, rule).validate(value, allParams)
		return result.getOrThrow() as String
	}

	/*************************** 路由参数 *****************************/
	/**
	 * 获得当前controller
	 * @return
	 */
	public val controller: String
		get() = getRouteParameter("controller")!!

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
		get() = getRouteParameter("action")!!

	/**
	 * 检查是否包含指定路由参数
	 * @param key 路由参数名
	 * @return
	 */
	public fun containsRouteParameter(key: String): Boolean {
		return routeParams.containsKey(key)
	}

	/**
	 * 检查路由参数是否为空
	 *
	 * @param key 参数名
	 * @return
	 */
	public fun isEmptyRouteParameter(key: String): Boolean {
		return routeParams.containsKey(key) && routeParams[key].isNullOrEmpty()
	}

	/**
	 * 获得路由参数
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public inline fun <reified T:Any> getRouteParameter(key:String, defaultValue:T? = null):T? {
		return routeParams.getAndConvert(key, defaultValue)
	}

	/**
	 * 获得并校验路由参数
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public inline fun <reified T:Any> getAndValidateRouteParameter(key:String, rule: String, defaultValue:T? = null):T? {
		val value = routeParams[key]
		return validateValue(key, value, rule).toNullable(T::class, defaultValue)
	}

	/**
	 * 获得int类型的路由参数
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getIntRouteParameter(key: String, defaultValue: Int? = null): Int? {
		return getRouteParameter(key, defaultValue)
	}

	/**
	 * 获得long类型的路由参数
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getLongRouteParameter(key: String, defaultValue: Long? = null): Long? {
		return getRouteParameter(key, defaultValue)
	}

	/**
	 * 获得boolean类型的路由参数
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getBooleanRouteParameter(key: String, defaultValue: Boolean? = null): Boolean? {
		return getRouteParameter(key, defaultValue)
	}

	/**
	 * 获得float类型的路由参数
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getFloatRouteParameter(key: String, defaultValue: Float? = null): Float? {
		return getRouteParameter(key, defaultValue)
	}

	/**
	 * 获得double类型的路由参数
	 */
	public fun getDoubleRouteParameter(key: String, defaultValue: Double? = null): Double? {
		return getRouteParameter(key, defaultValue)
	}

	/**
	 * 获得short类型的路由参数
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getShortRouteParameter(key: String, defaultValue: Short? = null): Short? {
		return getRouteParameter(key, defaultValue)
	}

	/**
	 * 获得Date类型的路由参数
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getDateRouteParameter(key: String, defaultValue: Date? = null): Date? {
		return getRouteParameter(key, defaultValue)
	}

	/*************************** get/post参数 *****************************/
	/**
	 * 检查是否有get/post的参数
     * @param key
     * @return
	 */
	public fun containsParameter(key: String): Boolean {
		return getParameter(key) != null
	}

	/**
	 * 检查get/post参数是否为空
	 *
	 * @param key 参数名
	 * @return
	 */
	public fun isEmptyParameter(key: String): Boolean {
		return getParameter(key).isNullOrEmpty()
	}

	/**
	 * 获得get/post参数值
	 *   注：调用时需明确指定返回类型，来自动转换参数值为指定类型
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public inline fun <reified T:Any> getParameter(key: String, defaultValue: T?): T? {
		return getParameter(key).toNullable(T::class, defaultValue)
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
	public inline fun <reified T:Any> getAndValidateParameter(key: String, rule: String, defaultValue: T?): T? {
		val value = getParameter(key)
		return validateValue(key, value, rule).toNullable(T::class, defaultValue)
	}

	/**
	 * 获得int类型的get/post的参数值
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getIntParameter(key: String, defaultValue: Int? = null): Int? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得long类型的get/post的参数值
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getLongParameter(key: String, defaultValue: Long? = null): Long? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得boolean类型的get/post的参数值
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getBooleanParameter(key: String, defaultValue: Boolean? = null): Boolean? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得Date类型的get/post的参数值
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getDateParameter(key: String, defaultValue: Date? = null): Date? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得float类型的get/post的参数值
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getFloatParameter(key: String, defaultValue: Float? = null): Float? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得double类型的get/post的参数值
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getDoubleParameter(key: String, defaultValue: Double? = null): Double? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得short类型的get/post的参数值
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getShortParameter(key: String, defaultValue: Short? = null): Short? {
		return getParameter(key, defaultValue)
	}

	/*************************** 其他 *****************************/
	/**
	 * 会话id
	 */
	public val sessionId: String?
		get() = getCookie("JSESSIONID")?.value

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
	 * @return 绝对路径
	 */
	public fun absoluteUrl(uri:String):String
	{
		if(uri.startsWith("http"))
			return uri;

		return serverUrl + contextPath + '/' + uri;
	}

	/**
	 * 获得web根目录的路径
	 * @return
	 */
	public val webRootDirectory: String
		get() = session.servletContext.getRealPath("/");

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
