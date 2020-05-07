package net.jkcode.jkmvc.http

import com.alibaba.fastjson.JSONObject
import net.jkcode.jkmvc.http.controller.Controller
import net.jkcode.jkmvc.http.util.AllPagination
import net.jkcode.jkmvc.http.util.Pagination
import net.jkcode.jkmvc.http.view.View
import net.jkcode.jkmvc.orm.normalizeData
import net.jkcode.jkmvc.orm.toJson
import net.jkcode.jkutil.collection.LazyAllocatedMap
import net.jkcode.jkutil.common.*
import org.apache.commons.lang.StringEscapeUtils
import java.io.*
import java.net.URLEncoder
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

/**
 * 响应对象
 *
 * @author shijianhang
 * @date 2016-10-7 下午11:32:07 
 */
class HttpResponse(res:HttpServletResponse /* 响应对象 */, protected val req: HttpRequest): HttpServletResponseWrapper(res) {

	companion object{
		/**
		 * 过期的期限
		 */
		protected val EXPIRESOVERDUE = "Mon, 26 Jul 1997 05:00:00 GMT";

		/**
		* 获得cookie配置
		 */
		protected val cookieConfig = Config.instance("cookie");

		/**
		* http状态码及其消息
		 */
		public val messages:Map<Int, String> = mapOf(
				// 信息性状态码 1xx
				100 to "Continue",
				101 to "Switching Protocols",

				// 成功状态码 2xx
				200 to "OK",
				201 to "Created",
				202 to "Accepted",
				203 to "Non-Authoritative Information",
				204 to "No Content",
				205 to "Reset Content",
				206 to "Partial Content",

				// 重定向状态码 3xx
				300 to "Multiple Choices",
				301 to "Moved Permanently",
				302 to "Found", // 1.1
				303 to "See Other",
				304 to "Not Modified",
				305 to "Use Proxy",
				307 to "Temporary Redirect",

				// 客户端错误状态码 4xx
				400 to "Bad Request",
				401 to "Unauthorized",
				402 to "Payment Required",
				403 to "Forbidden",
				404 to "Not Found",
				405 to "Method Not Allowed",
				406 to "Not Acceptable",
				407 to "Proxy Authentication Required",
				408 to "Request Timeout",
				409 to "Conflict",
				410 to "Gone",
				411 to "Length Required",
				412 to "Precondition Failed",
				413 to "Request Entity Too Large",
				414 to "Request-URI Too Long",
				415 to "Unsupported Media Type",
				416 to "Requested Range Not Satisfiable",
				417 to "Expectation Failed",

				// 服务端错误状态码 5xx
				500 to "Internal Server Error",
				501 to "Not Implemented",
				502 to "Bad Gateway",
				503 to "Service Unavailable",
				504 to "Gateway Timeout",
				505 to "HTTP Version Not Supported",
				509 to "Bandwidth Limit Exceeded"
		);

		/**
		 * 空的map, 用在函数 renderView(String, MutableMap) 的参数默认值中
		 */
		internal val emptyData: MutableMap<String, Any?> = LazyAllocatedMap()

		/**
		 * 获得当前响应
		 */
		@JvmStatic
		public fun current(): HttpResponse {
			return currentOrNull()!!
		}

		/**
		 * 获得当前响应
		 */
		@JvmStatic
		public fun currentOrNull(): HttpResponse? {
			return Controller.currentOrNull()?.res
		}
	}

	/**
	 * 响应对象
	 */
	protected val res:HttpServletResponse
		get() = response as HttpServletResponse

	/**
	 * 是否已渲染过
	 */
	 public var rendered: Boolean = false
		internal set

	/**
	 * 获得writer
	 */
	public fun prepareWriter(): PrintWriter
	{
		// 中文编码
		res.characterEncoding = "UTF-8";
		res.contentType = "text/html;charset=UTF-8";
		return res.writer
	}

	override fun sendRedirect(location: String) {
		super.sendRedirect(location)
		httpLogger.debug("Redirect to: {}", location)
	}

	/**
	 * 响应视图
	 *
	 * @param view 视图
	 */
	public fun renderView(view: View)
	{
		rendered = true
		httpLogger.debug("Render view: {}", view)
		view.render();
	}

	/**
	 * 响应视图
	 *
	 * @param file 视图文件
	 * @param vm 视图模型
	 */
	public fun renderView(file:String, vm:Map<String, Any?> = emptyData)
	{
		renderView(View(HttpRequest.current(), this, file, vm))
	}

	/**
	 * 响应字符串
	 *
	 * @param content
	 */
	public fun renderString(content:String)
	{
		rendered = true
		prepareWriter().print(content);
	}

	/**
	 * 响应文件
	 *
	 * @param file
	 */
	public fun renderFile(file: String)
	{
		renderFile(File(file))
	}

	/**
	 * 响应文件
	 *
	 * @param file
	 */
	public fun renderFile(file: File)
	{
		rendered = true
		httpLogger.debug("Render file: {}", file)

		// 文件不存在
		if (file == null || file.isDirectory || !file.exists()) {
			sendError(HttpServletResponse.SC_NOT_FOUND)
			setDateHeader("Expires", System.currentTimeMillis() + 0)
			setHeader("Cache-Control", "no-cache, no-store")
			return
		}

		//通知客户端文件的下载    URLEncoder.encode解决文件名中文的问题
		res.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(file.name, "utf-8"))
		val contentType = req.session.servletContext.getMimeType(file.getName()) ?: "application/octet-stream"
		res.setHeader("Content-Type", contentType)

		// 输出文件
		res.outputStream.writeFile(file)
		res.outputStream.flush()
	}

	/**
	 * 响应输入流
	 *   会帮你关掉输入流
	 *
	 * @param input
	 */
	public fun renderFromInput(input: InputStream, name: String? = null)
	{
		rendered = true

		//通知客户端文件的下载    URLEncoder.encode解决文件名中文的问题
		if(name != null)
			res.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(name, "utf-8"))
		val contentType = req.session.servletContext.getMimeType(name) ?: "application/octet-stream"
		res.setHeader("Content-Type", contentType)

		// 输出文件
		res.outputStream.writeFromInput(input)
	}

	/**
	 * 设置响应状态码
	 * 
	 * @param status 状态码
	 * @return
	 */
	public override fun setStatus(status: Int) {
		if(!messages.containsKey(status))
			throw IllegalArgumentException("无效响应状态码");

		res.setStatus(status)
	}

	/**
	 * 转发, 补全req.contextPath
	 * @param location
	 */
	public fun redirectTo(location: String) {
		var url = location
		if(!url.startsWith("http"))
			url = req.contextPath + url
		sendRedirect(url)
	}


	/**
	* 获得缓存时间
	 */
	public fun getCache():String?{
		// 无缓存
		val expires = getHeader("Expires")
		if(expires == EXPIRESOVERDUE)
			return null

		// 有缓存
		return expires;
	}
	
	/**
	 * 设置响应缓存
	 *
	 * @param expires 过期时间, 单位秒
	 * @return
	 */
	public fun setCache(expires:Long): HttpResponse {
		// setter
		if (expires > 0) { // 有过期时间, 则缓存
			val now:Long =  System.currentTimeMillis()
			this.addDateHeader("Last-Modified", now);
			this.addDateHeader("Expires", now + expires * 1000);
			this.addHeader("Cache-Control", "max-age=$expires");
			this.addHeader("Pragma", "Pragma")
		}else{ // 否则, 不缓存
			this.addHeader("Expires", EXPIRESOVERDUE);
			this.addHeader("Cache-Control", "no-cache")
			this.addHeader("Pragma", "no-cache");
		}
		return this;
	}

	/**
	 * 设置cookie值
	 *
	 * <code>
	 *     set("theme", "red");
	 * </code>
	 *
	 * @param  name       cookie名
	 * @param  value      cookie值
	 * @param expiration 期限
	 * @return
	 */
	public fun setCookie(name:String, value:String, expiry:Int? = null): HttpResponse {
		val cookie: javax.servlet.http.Cookie = javax.servlet.http.Cookie(name, value);
		// expiry
		val maxAage:Int? = cookieConfig.getInt("expiry", expiry);
		if(maxAage != null)
			cookie.maxAge = maxAage
		// path
		val path:String? = cookieConfig.get("path");
		if(path != null)
			cookie.path = path
		// domain
		val domain:String? = cookieConfig.get("domain");
		if(domain != null)
			cookie.domain = domain
		// secure
		val secure:Boolean? = cookieConfig.getBoolean("secure");
		if(secure != null)
			cookie.secure = secure
		// httponly
		val httponly:Boolean? = cookieConfig.getBoolean("httponly");
		if(httponly != null)
			cookie.isHttpOnly = httponly
		addCookie(cookie);
		return this;
	}

	/**
	 * 设置cookie值
	 *
	 * <code>
	 *     set("theme", "red");
	 * </code>
	 *
	 * @param  name       cookie名
	 * @param  value      cookie值
	 * @param expiration 期限
	 * @return
	 */
	public fun setCookies(data:Map<String, String>, expiry:Int? = null): HttpResponse {
		for((name, value) in data){
			setCookie(name, value, expiry);
		}
		return this;
	}

	/**
	 * 删除cookie
	 *
	 * <code>
	 *     delete("theme");
	 * </code>
	 *
	 * @param  name   cookie名
	 * @return
	 */
	public fun deleteCookie(name:String): HttpResponse {
		setCookie(name, "", -86400) // 让他过期
		return this;
	}

	/**************************************** 渲染json结果 ************************************************/
	/**
	 * 渲染任意对象
	 * @param data
	 */
	public fun renderJson(data: Any) {
		renderString(data.toJson())
	}

	/**
	 * 渲染jsonp
	 * @param data
	 * @param callback 回调函数
	 */
	public fun renderJson(data: Any, callback: String?) {
		rendered = true
		val writer = prepareWriter()
		writer.apply {
			if (!callback.isNullOrBlank()) {
				print(StringEscapeUtils.escapeHtml(callback))
				print("(")
			}

			print(data.toJson())

			if (!callback.isNullOrBlank()) {
				print(")")
			}
		}
	}

	/**
	 * 渲染选项
	 * @param data
	 * @param withEmpty 是否带空选项
	 * @param callback 回调函数
	 * @param transform
	 */
	public fun <T> renderOptions(data: Collection<T>, withEmpty: Boolean = false, callback: String? = null, transform: (T) -> Pair<String, String>?){
		val options = data.toOptions(withEmpty, transform)
		renderJson(options, callback)
	}

	/**
	 * 渲染标准3属性响应
	 *
	 * @param code 错误码，0是成功，非0是失败
	 * @param message 消息
	 * @param items 数据
	 */
	public fun renderJson(code:Int, message:String?, data:Any? = null){
		//打印错误
		if(code != 0)
			httpLogger.error(message);

		// json响应
		val obj = JSONObject()
		obj["code"] = code
		obj["message"] = message
		obj["data"] = normalizeData(data)
		renderJson(obj)
	}

	/**
	 * 渲染标准3属性响应: 数据是orm列表，支持分页
	 *
	 * @param code 错误码，0是成功，非0是失败
	 * @param message 消息
	 * @param items 当前页的数据
	 * @param pagination 分页
	 */
	public fun renderJson(code:Int, message:String?, items:List<*>, pagination: Pagination? = null){
		//打印错误
		if(code != 0)
			httpLogger.error(message);

		// json响应
		val obj = JSONObject()
		obj["code"] = code
		obj["message"] = message
		obj["data"] = normalizeData(items)

		// 构造分页json
		if(pagination != null)
			obj["pagination"] = buildPaginationJson(pagination)
		renderJson(obj)
	}

	/**
	 * 渲染标准3属性响应: 数据是orm列表，全量分页
	 *
	 * @param code 错误码，0是成功，非0是失败
	 * @param message 消息
	 * @param pagination 全量分页
	 */
	public fun renderJson(code:Int, message:String?, pagination: AllPagination<*>){
		renderJson(code, message, pagination.pageItems, pagination)
	}

	/**
	 * 构造分页json
	 *
	 * @param pagination 分页
	 * @return json
	 */
	protected fun buildPaginationJson(pagination: Pagination): JSONObject {
		val data = JSONObject()
		data["page"] = pagination.page
		data["total"] = pagination.total
		data["totalPages"] = pagination.totalPages
		return data
	}

}