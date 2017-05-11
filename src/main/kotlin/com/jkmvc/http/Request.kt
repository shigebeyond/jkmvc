package com.jkmvc.http

import com.jkmvc.common.Config
import com.jkmvc.common.getOrDefault
import com.jkmvc.common.to
import com.jkmvc.common.toDate
import org.apache.commons.fileupload.FileItem
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.fileupload.servlet.ServletFileUpload
import java.io.File
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
class Request(protected val req:HttpServletRequest /* 请求对象 */):HttpServletRequest by req
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
	}

	/**
	 * 当前匹配的路由规则
	 */
	public lateinit var route:Route;

	/**
	 * 当前匹配的路由参数
	 */
	public lateinit var params:Map<String, String>;

	/**
	 * 上传的数据
	 */
	public var uploadData:Map<String, String>? = null;

	init{
		// 中文编码
		req.characterEncoding = "UTF-8";
	}

	/**
	 * 解析路由
	 * @return
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
	 * @return
	 */
	public fun isPost(): Boolean {
		return this.method === "POST";
	}

	/**
	 * 是否get请求
	 * @return
	 */
	public fun isGet(): Boolean {
		return this.method === "GET";
	}

	/**
	 * 是否ajax请求
	 * @return
	 */
	public fun isajax(): Boolean {
		return "XMLHttpRequest".equals(req.getHeader("x-requested-with")) // // 通过XMLHttpRequest发送请求
				&& "text/javascript, application/javascript, */*".equals(req.getHeader("Accept")); // 通过jsonp来发送请求
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
	public fun getCookie(name:String): Cookie {
		return this.cookies.first(){
			it.name == name
		}
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
	 * @param column 字段名
	 * @param defaultValue 默认值
	 * @return
	 */
	operator inline fun <reified T> get(column: String, defaultValue: Any? = null): T
	{
		val clazz:KClass<*> = T::class
		// 先取路由参数
		if(params.containsKey(column))
			return params[column]!!.to(clazz) as T

		// 再取get/post参数
		if(containsParameter(column))
			return getParameter(column).to(clazz) as T

		return defaultValue as T;
	}

	/**
	 * 获得当前匹配路由的所有参数/单个参数
	 *
	 * @param key 如果是null，则返回所有参数，否则，返回该key对应的单个参数
	 * @param default 单个参数的默认值
	 * @param filter  参数过滤表达式, 如 "trim > htmlspecialchars"
	 * @return
	 */
	public fun getRouteParameter(key:String, default:String? = null):String?
	{
		return this.params.getOrDefault(key, default)
	}

	/**
	 * 获得当前目录
	 * @return
	 */
	public fun directory(): String {
		return this.getRouteParameter("directory", "")!!
	}

	/**
	 * 获得当前controller
	 * @return
	 */
	public fun controller(): String {
		return this.getRouteParameter("controller")!!;
	}

	/**
	 * 获得当前action
	 * @return
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

	/**
	 * 检查是否有get/post/upload的参数
	 *    兼容上传文件的情况
	 */
	public fun containsParameter(key: String): Boolean
	{
		if(uploadData != null)
			return uploadData!!.containsKey(key);

		return req.parameterMap.containsKey(key);
	}

	/**
	 * 获得get/post/upload的数据
	 *   兼容上传文件的情况
	 */
	public fun getParameterWithUpload(key: String): String?
	{
		if(uploadData != null)
			return uploadData!![key];

		return req.getParameter(key)
	}

	public fun getParameter(key: String, defaultValue: String): String? {
		val value = getParameterWithUpload(key);
		return if(value == null)
					defaultValue
				else
					value;
	}

	public fun getIntParameter(key: String, defaultValue: Int? = null): Int? {
		val value = getParameterWithUpload(key)
		return if(value == null)
			defaultValue
		else
			value.toInt();
	}

	public fun getLongParameter(key: String, defaultValue: Long? = null): Long? {
		val value = getParameterWithUpload(key)
		return if(value == null)
			defaultValue
		else
			value.toLong();
	}

	public fun getBooleanParameter(key: String, defaultValue: Boolean? = null): Boolean? {
		var value: String? = getParameterWithUpload(key)
		return if(value == null)
			defaultValue
		else
			value.toBoolean();
	}

	public fun getDateParameter(key: String, defaultValue: Date? = null): Date? {
		val value = getParameterWithUpload(key)
		return if(value == null)
			defaultValue
		else
			value.toDate()
	}

	/**
	* 设置多个属性
	 */
	public fun setAttributes(data:Map<String, Any?>) {
		if (data != null)
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

		return req.getScheme() + "://" + req.getServerName() + ':' + req.getServerPort() + Router.baseUrl + uri;
	}

	/**
	 * 检查并处理上传文件
	 * @param mover 文件处理函数
	 * @return
	 */
	public fun checkUpload(mover: (FileItem) -> String): Boolean
	{
		// 检查是否文件上传
		if(!ServletFileUpload.isMultipartContent(req))
			return false;

		// 处理上传数据
		val config = Config.instance("upload")!!
		val diskFactory = DiskFileItemFactory()
		diskFactory.sizeThreshold = 4 * 1024 // threshold 极限、临界值，即硬盘缓存 1M
		diskFactory.repository = File(config["tmpDir"]!!) // repository 贮藏室，即临时文件目录

		val upload = ServletFileUpload(diskFactory)
		upload.sizeMax = (4 * 1024 * 1024).toLong() // 设置允许上传的最大文件大小 4M

		// 解析请求
		val list:List<FileItem> = upload.parseRequest(req)
		val data:MutableMap<String, String> = HashMap();
		for (item in list) {
			if (item.isFormField) { // 表单字段
				data[item.name] = item.string;
			} else { // 文件字段
				data[item.name] = mover(item); // 移动上传文件
			}
		}

		// 保存上传数据
		uploadData = data;

		return true;
	}

}
