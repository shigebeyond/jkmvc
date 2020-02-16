package net.jkcode.jkmvc.http

import net.jkcode.jkutil.common.*
import net.jkcode.jkmvc.http.controller.Controller
import net.jkcode.jkmvc.http.controller.ControllerClass
import net.jkcode.jkmvc.http.controller.ControllerClassLoader
import net.jkcode.jkmvc.http.router.HttpMethod
import net.jkcode.jkmvc.http.router.Route
import net.jkcode.jkmvc.http.router.RouteException
import net.jkcode.jkmvc.http.router.Router
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
 *
 * @author shijianhang
 * @date 2016-10-6 上午9:27:56
 *
 */
class HttpRequest(req:HttpServletRequest): MultipartRequest(req)
{
	companion object{

		/**
		 * 全局的ServletContext
		 *   fix bug: jetty异步请求后 req.contextPath/req.servletContext 居然为null, 因此直接在 JkFilter.init() 时记录ServletContext(包含contextPath), 反正他是全局不变的
		 */
		public lateinit var globalServletContext: ServletContext

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
	 * 获得要解析的uri
	 *   1 去掉头部的contextPath
	 *   2 去掉末尾的/
	 */
	public val routeUri:String = if(contextPath == null)
									throw RouteException("req.contextPath is null")
								else
									requestURI.trim(contextPath + '/', "/")

	/**
	 * 当前匹配的路由规则
	 */
	public lateinit var route: Route;

	/**
	 * 当前匹配的路由参数
	 */
	public lateinit var routeParams:Map<String, String>;

	/**
	 * 请求参数
	 *    兼容上传文件的情况, 但由于value类型是 Array<String?>, 因此不兼容File字段值
	 *    for jkerp
	 */
	public val httpParams: Map<String, Array<String?>> by lazy{
			if(isUpload()) // 上传请求的参数类型：Map<String, Array<String>|File>, 但不兼容File字段值
				partMap as Map<String, Array<String?>>
			else // 非上传请求的参数类型：Map<String, Array<String>>
				req.parameterMap
		}

	/**
	 * 全部参数 = 路由参数 + 请求参数
	 *    路由参数的value类型是String, 请求参数的value类型是 Array<String?>, 因此统一改为String
	 */
	public val allParams:Map<String, String?> by lazy{
		CompositeMap(routeParams, HttpParamMap(httpParams)) as Map<String, String>
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
			this.routeParams = result.component1();
			this.route = result.component2();
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
		// 先取get/post参数
		if(containsParameter(key))
			return getParameter(key)!!.to(T::class) as T

		// 再取路由参数
		if(routeParams.containsKey(key))
			return routeParams[key]!!.to(T::class) as T

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
		var value:String? = null
		if(routeParams.containsKey(key)) // 先取路由参数
			value = routeParams[key]!!
		else if(containsParameter(key)) // 再取get/post参数
			value = getParameter(key)!!
		else
			return defaultValue

		// 校验参数值
		return validateValue(key, value, rule).toNullable(T::class)
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

	/*************************** get/post/upload参数 *****************************/
	/**
	 * 检查是否有get/post/upload的参数
	 *    兼容上传文件的情况
     * @param key
     * @return
	 */
	public fun containsParameter(key: String): Boolean {
		return httpParams.contains(key)
	}

	/**
	 * 检查get/post/upload参数是否为空
	 *
	 * @param key 参数名
	 * @return
	 */
	public fun isEmptyParameter(key: String): Boolean {
		return !containsParameter(key) || getParameter(key).isNullOrEmpty()
	}

	/**
	 * 获得get/post/upload的参数名的枚举
	 *    兼容上传文件的情况
	 * @return
	 */
	public override fun getParameterNames():Enumeration<String>{
		if(isUpload())
			return getPartNames()

		return req.parameterNames;
	}

	/**
	 * 获得get/post/upload的参数值
	 *    兼容上传文件的情况
	 * @param key
	 * @return
	 */
	public override fun getParameter(key: String): String? {
		if(isUpload())
			return getPartTexts(key)?.first()

		return req.getParameter(key)
	}

	/**
	 * 获得get/post/upload的参数值
	 *    兼容上传文件的情况
	 * @param key
	 * @return
	 */
	public override fun getParameterValues(key: String): Array<String>? {
		if(isUpload())
			return getPartTexts(key)

		return req.getParameterValues(key)
	}

	/**
	 * 获得get/post/upload参数值
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
	 * 获得并校验get/post/upload参数值
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
	 * 获得int类型的get/post/upload的参数值
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getIntParameter(key: String, defaultValue: Int? = null): Int? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得long类型的get/post/upload的参数值
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getLongParameter(key: String, defaultValue: Long? = null): Long? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得boolean类型的get/post/upload的参数值
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getBooleanParameter(key: String, defaultValue: Boolean? = null): Boolean? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得Date类型的get/post/upload的参数值
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getDateParameter(key: String, defaultValue: Date? = null): Date? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得float类型的get/post/upload的参数值
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getFloatParameter(key: String, defaultValue: Float? = null): Float? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得double类型的get/post/upload的参数值
	 *
	 * @param key 参数名
	 * @param defaultValue 单个参数的默认值
	 * @return
	 */
	public fun getDoubleParameter(key: String, defaultValue: Double? = null): Double? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得short类型的get/post/upload的参数值
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
	public fun getCookie(name:String): Cookie {
		return this.cookies.first(){
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
	public fun getWebPath(): String
	{
		return session.servletContext.getRealPath("/");
	}

	/**
	 * 对query string进行 null 转 空字符串
	 * @return
	 */
	public override fun getQueryString(): String {
		val result = super.getQueryString()
		return if(result == null) "" else result
	}

	/**
	 * 构建curl命令
	 * @return
	 */
	public fun buildCurlCommand(): String {
		// get请求
		if(isGet())
			return "curl '${requestURL}?${queryString}'";

		// post请求
		return "curl -d '${parameterMap.toQueryString()}' '${requestURL}?${queryString}'"
	}
}
