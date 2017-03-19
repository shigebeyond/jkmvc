package com.jkmvc.http

import com.jkmvc.common.clear

/**
 * 响应对象
 * 	TODO: 支持响应文件
 * 
 * @Package packagename 
 * @category 
 * @author shijianhang
 * @date 2016-10-7 下午11:32:07 
 *
 */
class Response 
{
	companion object{
		/**
		 * 过期的期限
		 * @var string
		 */
		protected val EXPIRESOVERDUE = "Mon, 26 Jul 1997 05:00:00 GMT";

		// http状态码及其消息
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
	 * 响应状态码
	 * @var  integer
	*/
	protected val status:Int = 200;
	
	/**
	 * http协议
	 * @var string
	 */
	protected val protocol:String = "HTTP/1.1";
	
	/**
	 * 响应头部
	 * @var array
	 */
	protected val headers = array();
	
	/**
	 * 响应主体
	 * @var string
	 */
	protected var body:StringBuilder = StringBuilder();
	
	/**
	 * 设置响应主体
	 * 
	 * @param string content
	 * @return string|Response
	 */
	public fun body(content:String)
	{
		this.body.clear().append(content);
		return this;
	}
	/**
	 * 设置响应主体
	 *
	 * @param View content
	 * @return string|Response
	 */
	public fun body(content:View)
	{
		return this.body(content.render());
	}

	/**
	 * 追加响应主体
	 *
	 * @param string content
	 * @return string|Response
	 */
	public fun append(content:String)
	{
		this.body.append(content);
		return this;
	}

	/**
	 * 追加响应主体
	 *
	 * @param string content
	 * @return string|Response
	 */
	public fun append(content:View)
	{
		return this.append(content.render())
	}

	/**
	 * 获得与设置http协议
	 * 
	 * @param string protocol 协议
	 * @return Response|string
	 */
	public fun protocol(protocol = null)
	{
		//getter
		if (protocol === null)
			return this.protocol;
		
		//setter
		this.protocol = strtoupper(protocol);
		return this;
	}
	
	/**
	 * 读取与设置响应状态码
	 * 
	 * @param string status 状态码
	 * @return number|Response
	 */
	public fun status(status = null)
	{
		// getter
		if (status === null)
			return this.status;
		
		// setter
		if(!isset(static::messages[status]))
			throw new Exception("无效响应状态码");
		
		this.status = (int) status;
		return this;
	}
	
	/**
	 * 读取与设置全部头部字段
	 *
	 *       // 获得全部头部字段
	 *       headers = response.headers();
	 *
	 *       // 设置头部
	 *       response.headers(array("Content-Type" => "text/html", "Cache-Control" => "no-cache"));
	 *
	 * @param array headers 头部字段 
	 * @return mixed
	 */
	public fun headers(array headers = null, merge = true)
	{
		// getter
		if (headers === null)
			return this.headers;
		
		// setter
		this.headers = merge ? mergearray(this.headers, headers) : headers;
		return this;
	}
	
	/**
	 * 获得与设置单个头部字段
	 * 
	 *       // 获得一个头部字段
	 *       accept = response.header("Content-Type");
	 *
	 *       // 设置一个头部字段
	 *       response.header("Content-Type", "text/html");
	 * 
	 * @param string key 字段名
	 * @param string value 字段值
	 * @return string|Response
	 */
	public fun header(key, value = null)
	{
		// getter
		if (value === null)
			return Arr::get(this.headers, key);
		
		// setter
		this.headers[key] = value;
		return this;
	}
	
	/**
	 * 设置响应缓存
	 *
	 * @param int|string expires 过期时间
	 * @return string|Response
	 */
	public fun cache(expires = null) {
		// getter
		if (expires === null) 
		{
			// 无缓存
			if(!isset(this.headers["Expires"]) OR this.headers["Expires"] == static::EXPIRESOVERDUE)
				return false;
			
			// 有缓存
			return this.headers["Expires"];
		}
		
		// setter
		if (expires) { // 有过期时间, 则缓存
			expires = isint(expires) ? expires : strtotime(expires);
			this.headers["Expires"] = gmdate("D, d M Y H:i:s", expires) . " GMT";
			this.headers["Cache-Control"] = "max-age=".(expires - time());
			if (isset(this.headers["Pragma"]) && this.headers["Pragma"] == "no-cache")
				unset(this.headers["Pragma"]);
		}else{ // 否则, 不缓存
			this.headers["Expires"] = static::EXPIRESOVERDUE;
			this.headers["Cache-Control"] = array(
					"no-store, no-cache, must-revalidate",
					"post-check=0, pre-check=0",
					"max-age=0"
			);
			this.headers["Pragma"] = "no-cache";
		}
		return this;
	}
	
	/**
	 * 发送头部给客户端
	 * @return Response
	 */
	public fun sendheaders()
	{
		if(headerssent())
			return;
		
		// 1 状态行
		header(this.protocol." ".this.status." ".static::messages[this.status]);
	
		// 2 各个头部字段
		foreach (this.headers as header => value)
		{
			// cookie字段
			if(key == "Set-Cookie")
			{
				Cookie::set(value);
				continue;
			}
			
			// 其他字段
			if (isarray(value)) // 多值拼接
				value = implode(", ", value);
		
			header(Text::ucfirst(header).": ".value, true);
		}
		
		// 正文大小
		if ((length = strlen(this.body)) > 0) {
			header("Content-Length: ".length);
		}
	
		return this;
	}
	
	/**
	 * 发送响应该客户端
	 */
	public fun send()
	{
		// 先略过, 不排除有其他输出
		// 清空内容缓存
		/* if (obgetlength() > 0)
			obendclean(); */
		
		// 先头部，后主体
		echo this.sendheaders().body();
	}
}