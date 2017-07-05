package com.jkmvc.http

import com.jkmvc.common.*
import java.util.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import kotlin.reflect.KClass

/**
 * 请求对象
 *
 * @author shijianhang
 * @date 2016-10-6 上午9:27:56
 *
 */
class Request(req:HttpServletRequest):MultipartRequest(req)
{
	companion object{
		/**
		 * 请求对象缓存
		 */
		protected val reqs:ThreadLocal<Request> = ThreadLocal();

		/**
		 * 可信任的代理服务器ip
		 */
		public val proxyips = arrayOf("127.0.0.1", "localhost", "localhost.localdomain");

		/**
		 * 获得当前请求
		 */
		@JvmStatic
		public fun current(): Request {
			return reqs.get()!!;
		}
	}

	/**
	 * 获得要解析的uri
	 *   1 去掉头部的contextPath
	 *   2 去掉末尾的/
	 */
	public val routeUri:String = requestURI.trim(contextPath + '/', "/")
	/**
	 * 当前匹配的路由规则
	 */
	public lateinit var route:Route;

	/**
	 * 当前匹配的路由参数
	 */
	public lateinit var params:Map<String, String>;

	init{
		// 中文编码
		req.characterEncoding = "UTF-8";
		reqs.set(this);
	}

	/*************************** 路由解析 *****************************/
	/**
	 * 是否是静态文件请求，如果是则不进行路由解析
	 * @return
	 */
	public fun isStaticFile(): Boolean {
		return Router.staticFileRegex.toRegex().matches(routeUri)
	}

	/**
	 * 解析路由
	 * @return
	 */
	public fun parseRoute(): Boolean {
		// 解析路由
		val result = Router.parse(routeUri);

		if(result != null){
			this.params = result.component1();
			this.route = result.component2();
			return true;
		}

		return false;
	}

	/*************************** 各种判断 *****************************/
	/**
	 * 是否post请求
	 * @return
	 */
	public fun isPost(): Boolean {
		return method === "POST";
	}

	/**
	 * 是否get请求
	 * @return
	 */
	public fun isGet(): Boolean {
		return req.isGet()
	}

	/**
	 * 是否 multipart 请求
	 * @return
	 */
	public fun isMultipartContent(): Boolean{
		return req.isMultipartContent()
	}

	/**
	 * 是否上传文件的请求
	 * @return
	 */
	public fun isUpload(): Boolean{
		return uploaded
	}

	/**
	 * 是否ajax请求
	 * @return
	 */
	public fun isAjax(): Boolean {
		return req.isAjax()
	}

	/**
	 * 智能获得请求参数，先从路由参数中取得，如果没有，则从get/post参数中取
	 *    注：智能获得请求参数时，需要根据返回值的类型来转换参数值的类型，因此调用时需明确返回值的类型
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
		val clazz:KClass<T> = T::class
		// 先取路由参数
		if(params.containsKey(key))
			return params[key]!!.to(clazz) as T

		// 再取get/post参数
		if(containsParameter(key))
			return getParameter(key)!!.to(clazz) as T

		return defaultValue;
	}

	/*************************** 路由参数 *****************************/
	/**
	 * 检查是否包含指定路由参数
	 * @param key 路由参数名
	 * @return
	 */
	public fun containsRouteParameter(key: String): Boolean {
		return params.containsKey(key)
	}

	/**
	 * 获得当前匹配路由的所有参数/单个参数
	 *
	 * @param key 如果是null，则返回所有参数，否则，返回该key对应的单个参数
	 * @param defaultValue 单个参数的默认值
	 * @param filter  参数过滤表达式, 如 "trim > htmlspecialchars"
	 * @return
	 */
	public inline fun <reified T:Any> getRouteParameter(key:String, defaultValue:T? = null):T? {
		return params.getAndConvert(key, defaultValue)
	}

	/**
	 * 获得当前目录
	 * @return
	 */
	public fun directory(): String {
		return getRouteParameter("directory", "")!!
	}

	/**
	 * 获得当前controller
	 * @return
	 */
	public fun controller(): String {
		return getRouteParameter("controller")!!;
	}

	/**
	 * 获得当前action
	 * @return
	 */
	public fun action(): String {
		return getRouteParameter("action")!!;
	}

	/**
	 * 获得int类型的路由参数
	 */
	public fun getIntRouteParameter(key: String, defaultValue: Int? = null): Int? {
		return getRouteParameter(key, defaultValue)
	}

	/**
	 * 获得long类型的路由参数
	 */
	public fun getLongRouteParameter(key: String, defaultValue: Long? = null): Long? {
		return getRouteParameter(key, defaultValue)
	}

	/**
	 * 获得boolean类型的路由参数
	 */
	public fun getBooleanRouteParameter(key: String, defaultValue: Boolean? = null): Boolean? {
		return getRouteParameter(key, defaultValue)
	}


	/**
	 * 获得float类型的路由参数
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
	 */
	public fun getShortRouteParameter(key: String, defaultValue: Short? = null): Short? {
		return getRouteParameter(key, defaultValue)
	}

	/**
	 * 获得Date类型的路由参数
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
	public fun containsParameter(key: String): Boolean
	{
		if(isUpload())
			return mulReq.getParameterMap().containsKey(key)

		return req.parameterMap.containsKey(key);
	}

	/**
	 * 获得get/post/upload的参数名的枚举
	 *    兼容上传文件的情况
	 * @return
	 */
	public override fun getParameterNames():Enumeration<String>{
		if(isUpload())
			return mulReq.parameterNames as Enumeration<String>;

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
			return mulReq.getParameter(key);

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
			return mulReq.getParameterValues(key)

		return req.getParameterValues(key)
	}

	/**
	 * 获得get/post/upload参数
	 *    兼容上传文件的情况
	 * @return
	 */
	public override fun getParameterMap(): Map<String, Array<String>>{
		if(isUpload())
			return mulReq.getParameterMap()

		return req.parameterMap
	}

	/**
	 * 获得参数值，自动转换为指定类型
	 */
	public inline fun <reified T:Any> getParameter(key: String, defaultValue: T?): T? {
		return getParameter(key).toNullable(T::class, defaultValue)
	}

	/**
	 * 获得int类型的参数值
	 */
	public fun getIntParameter(key: String, defaultValue: Int? = null): Int? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得long类型的参数值
	 */
	public fun getLongParameter(key: String, defaultValue: Long? = null): Long? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得boolean类型的参数值
	 */
	public fun getBooleanParameter(key: String, defaultValue: Boolean? = null): Boolean? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得Date类型的参数值
	 */
	public fun getDateParameter(key: String, defaultValue: Date? = null): Date? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得float类型的参数值
	 */
	public fun getFloatParameter(key: String, defaultValue: Float? = null): Float? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得double类型的参数值
	 */
	public fun getDoubleParameter(key: String, defaultValue: Double? = null): Double? {
		return getParameter(key, defaultValue)
	}

	/**
	 * 获得short类型的参数值
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
	* 设置多个属性
	 */
	public fun setAttributes(data:Map<String, Any?>) {
		for ((k, v) in data)
			req.setAttribute(k, v);
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

}
