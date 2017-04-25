package com.jkmvc.http

import com.jkmvc.common.getOrDefault
import com.jkmvc.common.toDate
import java.util.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest

/**
 * 请求对象
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-6 上午9:27:56
 *
 */
class Request(protected val req:HttpServletRequest /* 请求对象 */):HttpServletRequest by req
{
	companion object{
		/**
		 * 请求对象缓存
		 */
		protected val reqs:ThreadLocal<Request> = ThreadLocal();

		/**
		 * 可信任的代理服务器ip
		 * @var array
		 */
		public val proxyips = arrayOf("127.0.0.1", "localhost", "localhost.localdomain");

	}

	/**
	 * 当前匹配的路由规则
	 * @var Route
	 */
	public lateinit var route:Route;

	/**
	 * 当前匹配的路由参数
	 * @var array
	 */
	public lateinit var params:Map<String, String>;

	/**
	 * 解析路由
	 * @return bool
	 */
	public fun parseRoute(): Boolean {
		// 解析路由
		val result = Router.parse(requestURI);

		if(result != null){
			this.params = result.component1();
			this.route = result.component2();
			return true;
		}

		return false;
	}

	/**
	 * 是否post请求
	 * @return bool
	 */
	public fun isPost(): Boolean {
		return this.method === "POST";
	}

	/**
	 * 是否get请求
	 * @return bool
	 */
	public fun isGet(): Boolean {
		return this.method === "GET";
	}

	/**
	 * 是否ajax请求
	 * @return boolean
	 */
	public fun isajax(): Boolean {
		return "XMLHttpRequest".equals(req.getHeader("x-requested-with")) // // 通过XMLHttpRequest发送请求
				&& "text/javascript, application/javascript, */*".equals(req.getHeader("Accept")); // 通过jsonp来发送请求
	}

	/**
	 * 获得cookie值
	 *
	 * <code>
	 *     theme = Cookie::get("theme", "blue");
	 * </code>
	 *
	 * @param   string  key        cookie名
	 * @param   mixed   default    默认值
	 * @return  string
	 */
	public fun getCookie(name:String): Cookie {
		return this.cookies.first(){
			it.name == name
		}
	}

	/**
	 * 获得当前匹配路由的所有参数/单个参数
	 *
	 * @param string key 如果是null，则返回所有参数，否则，返回该key对应的单个参数
	 * @param string default 单个参数的默认值
	 * @param   string filter  参数过滤表达式, 如 "trim > htmlspecialchars"
	 * @return multitype
	 */
	/*public fun getRouteParameter(key = null, default = null, filterexp = null)
	{
		return Arr::filtervalue(this.params, key, default, filterexp);
	}*/

	public fun getRouteParameter(key:String, default:String? = null):String?
	{
		return this.params.getOrDefault(key, default)
	}

	/**
	 * 获得当前目录
	 * @return string
	 */
	public fun directory(): String {
		return this.getRouteParameter("directory", "")!!
	}

	/**
	 * 获得当前controller
	 * @return string
	 */
	public fun controller(): String {
		return this.getRouteParameter("controller")!!;
	}

	/**
	 * 获得当前action
	 * @return string
	 */
	public fun action(): String {
		return this.getRouteParameter("action")!!;
	}

	public fun getIntRouteParameter(key: String, defaultValue: Int? = null): Int? {
		val value = params.get(key)
		return if(value == null)
			defaultValue
		else
			value.toInt();
	}

	public fun getLongRouteParameter(key: String, defaultValue: Long? = null): Long? {
		val value = params.get(key)
		return if(value == null)
			defaultValue
		else
			value.toLong();
	}

	public fun getBooleanRouteParameter(key: String, defaultValue: Boolean? = null): Boolean? {
		var value: String? = params.get(key)
		return if(value == null)
			defaultValue
		else
			value.toBoolean();
	}

	public fun containsRouteParameter(key: String): Boolean {
		return params.containsKey(key)
	}

	public fun getParameter(key: String, defaultValue: String): String? {
		val value = req.getParameter(key);
		return if(value == null)
					defaultValue
				else
					value;
	}

	public fun getIntParameter(key: String, defaultValue: Int? = null): Int? {
		val value = req.getParameter(key)
		return if(value == null)
			defaultValue
		else
			value.toInt();
	}

	public fun getLongParameter(key: String, defaultValue: Long? = null): Long? {
		val value = req.getParameter(key)
		return if(value == null)
			defaultValue
		else
			value.toLong();
	}

	public fun getBooleanParameter(key: String, defaultValue: Boolean? = null): Boolean? {
		var value: String? = req.getParameter(key)
		return if(value == null)
			defaultValue
		else
			value.toBoolean();
	}

	public fun getDateParameter(key: String, defaultValue: Date? = null): Date? {
		val value = req.getParameter(key)
		return if(value == null)
			defaultValue
		else
			value.toDate()
	}

	public fun containsParameter(key: String): Boolean {
		return req.parameterMap.containsKey(key);
	}

	/**
	* 设置多个属性
	 */
	public fun setAttributes(data:Map<String, Any?>) {
		if (data != null)
			for ((k, v) in data)
				req.setAttribute(k, v);
	}

}
