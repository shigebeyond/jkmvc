package com.jkmvc.http

/**
 * 请求对象
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-6 上午9:27:56
 *
 */
class Request
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
	protected route;

	/**
	 * 当前匹配的路由参数
	 * @var array
	 */
	protected params = array();

	/**
	 * post的原始数据
	 * @var string
	 */
	protected body;

	/**
	 * 客户端ip
	 * @var string
	 */
	protected clientip;

	/**
	 * 构建函数，可以指定uri，主要是为了应对单元测试
	 * @param string uri
	 */
	public constructor(uri = null){
		this.uri = uri;
		reqs.set(this);
	}

	/**
	 * 解析路由
	 * @return bool
	 */
	public fun parseroute(){
		// 解析路由
		list(params, route) = Router::parse(this.uri());

		if(params){
			this.params = params;
			this.route = route;
			return true;
		}

		return false;
	}

	/**
	 * 获得当前uri
	 */
	public fun uri()
	{
		if(this.uri === null)
			this.uri = static::prepareuri();

		return this.uri;
	}

	/**
	 * 获得当前匹配的路由规则
	 * @return Route
	 */
	public fun route(){
		return this.route;
	}

	/**
	 * 获得当前匹配路由的所有参数/单个参数
	 *
	 * @param string key 如果是null，则返回所有参数，否则，返回该key对应的单个参数
	 * @param string default 单个参数的默认值
	 * @param   string filter  参数过滤表达式, 如 "trim > htmlspecialchars"
	 * @return multitype
	 */
	public fun param(key = null, default = null, filterexp = null)
	{
		return Arr::filtervalue(this.params, key, default, filterexp);
	}

	/**
	 * 获得当前目录
	 * @return string
	 */
	public fun directory()
	{
		return this.param("directory");
	}

	/**
	 * 获得当前controller
	 * @return string
	 */
	public fun controller()
	{
		return this.param("controller");
	}

	/**
	 * 获得当前controller的类名
	 * @return string
	 */
	public fun controllerclass()
	{
		// 类前缀
		class = "Controller";

		// 目录
		if(this.directory())
			class .= Text::ucfirst(this.directory());

		// controller
		return class.Text::ucfirst(this.controller());
	}

	/**
	 * 获得当前action
	 * @return string
	 */
	public fun action()
	{
		return this.param("action");
	}

	/**
	 * 获得get参数
	 *
	 * @param   string key    参数名
	 * @param   string default  参数默认值
	 * @param   string filter  参数过滤表达式, 如 "trim > htmlspecialchars"
	 * @return  mixed
	 */
	public fun get(key = null, default = null, filterexp = null)
	{
		return Arr::filtervalue(GET, key, default, filterexp);
	}

	/**
	 * 获得post参数
	 *
	 * @param   string key    参数名
	 * @param   string default  参数默认值
	 * @param   string filter  参数过滤表达式, 如 "trim > htmlspecialchars"
	 * @return  mixed
	 */
	public fun post(key = null, default = null, filterexp = null)
	{
		return Arr::filtervalue(POST, key, default, filterexp);
	}
	
	/**
	 * 请求方法
	 * @return string
	 */
	public fun method(){
		return SERVER["REQUESTMETHOD"];
	}

	/**
	 * 是否post请求
	 * @return bool
	 */
	public fun ispost()
	{
		return this.method() === "POST";
	}

	/**
	 * 是否get请求
	 * @return bool
	 */
	public fun isget()
	{
		return this.method() === "GET";
	}

	/**
	 * 是否ajax请求
	 * @return boolean
	 */
	public fun isajax()
	{
		return Arr::equal(SERVER, "HTTPXREQUESTEDWITH", "XMLHttpRequest") // 通过XMLHttpRequest发送请求
				|| this.accepttypes() == "text/javascript, application/javascript, */*"; // 通过jsonp来发送请求
	}

	/**
	 * 是否是https请求
	 * @return boolean
	 */
	public fun ishttps()
	{
		return Arr::equal(SERVER, "HTTPS", "off", true);
	}

	/**
	 * 获得协议
	 * @return string
	 */
	public fun scheme()
	{
		if (isset(SERVER["REQUESTSCHEME"]))
			return SERVER["REQUESTSCHEME"];

		return this.ishttps() ? "https" : "http";
	}

	/**
	 * 获得post的原始数据
	 * @return string
	 */
	public fun body(){
		if(this.isGet())
			return null;

		if(this.body === null)
			this.body = filegetcontents("php://input");

		return this.body;
	}

	/**
	 * 获得cookie
	 * @param string key
	 * @param string default
	 */
	public fun cookie(key, default = null){
		return Cookie::get(key, default);
	}

	/**
	 * 客户端要接受的数据类型
	 * @return string
	 */
	public fun accepttypes()
	{
		return Arr::get(SERVER, "HTTPACCEPT");
	}

	/**
	 * 获得客户端ip
	 * @return string
	 */
	public fun clientip()
	{
		// 读缓存
		if(this.clientip !== null)
			return this.clientip;

		// 未知ip
		if(!isset(SERVER["REMOTEADDR"]))
			return this.clientip = "0.0.0.0";

		// 客户端走代理
		if(inarray(SERVER["REMOTEADDR"], static::proxyips)){
			foreach (array("HTTPXFORWARDEDFOR", "HTTPCLIENTIP") as header){
				// Use the forwarded IP address, typically set when the
				// client is using a proxy server.
				// Format: "X-Forwarded-For: client1, proxy1, proxy2"
				if(isset(SERVER[header]))
					return this.clientip = strstr(SERVER[header], ",", true);
			}
		}

		// 客户端没走代理
		return this.clientip = SERVER["REMOTEADDR"];
	}

	/**
	 * 获得user agent
	 * @return string
	 */
	public fun useragent()
	{
		return Arr::get(SERVER, "HTTPUSERAGENT");
	}

}
