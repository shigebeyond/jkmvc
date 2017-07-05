package com.jkmvc.http

import com.jkmvc.common.Config
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import java.net.URLEncoder
import javax.servlet.http.HttpServletResponse

/**
 * 响应对象
 * 	TODO: 支持响应文件
 * 
 * @author shijianhang
 * @date 2016-10-7 下午11:32:07 
 *
 */
class Response(protected val res:HttpServletResponse /* 响应对象 */): HttpServletResponse by res
{
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
	}

	/**
	 * 响应视图
	 *
	 * @param View content
	 */
	public fun render(view:View):Unit
	{
		view.render();
	}

	/**
	 * 响应文本
	 *
	 * @param content
	 */
	public fun render(content:String):Unit
	{
		prepareWriter().print(content);
	}

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

	/**
	 * 响应文件
	 *
	 * @param File file
	 */
	public fun render(file: File):Unit
	{
		//通知客户端文件的下载    URLEncoder.encode解决文件名中文的问题
		res.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(file.name, "utf-8"))
		res.setHeader("Content-Type", "application/octet-stream")

		// 输出文件
		val `in` = FileInputStream(file)
		val out = res.getOutputStream()
		var length = -1
		val buffer = ByteArray(1024)
		do{
			length = `in`.read(buffer)
			out.write(buffer, 0, length)
		}while(length != -1)
		`in`.close()
	}

	/**
	 * 设置响应状态码
	 * 
	 * @param status 状态码
	 * @return
	 */
	public override fun setStatus(status: Int):Unit {
		if(!messages.containsKey(status))
			throw IllegalArgumentException("无效响应状态码");

		res.setStatus(status)
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
	 * @param long expires 过期时间
	 * @return
	 */
	public fun setCache(expires:Long): Response {
		// setter
		if (expires > 0) { // 有过期时间, 则缓存
			val now:Long =  System.currentTimeMillis()
			this.addDateHeader("Last-Modified", now);
			this.addDateHeader("Expires", now + expires);
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
	public fun setCookie(name:String, value:String, expiry:Int? = null): Response {
		val cookie: javax.servlet.http.Cookie = javax.servlet.http.Cookie(name, value);
		// expiry
		val maxAage:Int? = cookieConfig?.getInt("expiry", expiry);
		if(maxAage != null)
			cookie.maxAge = maxAage
		// path
		val path:String? = cookieConfig?.get("path");
		if(path != null)
			cookie.path = path
		// domain
		val domain:String? = cookieConfig?.get("domain");
		if(domain != null)
			cookie.domain = domain
		// secure
		val secure:Boolean? = cookieConfig?.getBoolean("secure");
		if(secure != null)
			cookie.secure = secure
		// httponly
		val httponly:Boolean? = cookieConfig?.getBoolean("httponly");
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
	public fun setCookies(data:Map<String, String>, expiry:Int? = null): Response {
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
	public fun deleteCookie(name:String): Response {
		setCookie(name, "", -86400) // 让他过期
		return this;
	}
}