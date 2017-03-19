package com.jkmvc.http

/**
 * 服务端对象，用于处理请求
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-6 上午9:27:56
 *
 */
class Server 
{

	/**
	 * 结束输出缓冲
	 */
	public static fun obend()
	{
		if (funexists("fastcgifinishrequest")) {
			fastcgifinishrequest();
		} elseif ("cli" !== PHPSAPI) {
			// obgetlevel() never returns 0 on some Windows configurations, so if
			// the level is the same two times in a row, the loop should be stopped.
			previous = null;
			obStatus = obgetstatus(1);
			while ((level = obgetlevel()) > 0 && level !== previous) {
				previous = level;
				if (obStatus[level - 1] && isset(obStatus[level - 1]["del"]) && obStatus[level - 1]["del"]) {
					obendflush();
				}
			}
			flush();
		}
	}

	/**
	 * 处理请求
	 *
	 * @param Request req
	 * @param Response res
	 */
	public static fun run()
	{
		// 开始输出缓冲
		obstart();

		try {
			// 构建请求与响应对象
			req = new Request();
			res = new Response();

			// 解析路由
			if(!req.parseroute())
				throw new RouteException("当前uri没有匹配路由：".req.uri());

			// 调用路由对应的controller与action
			self::callcontroller(req, res);

			// 输出响应
			res.send();
		} 
		/* catch (RouteException e) 
		{
			// 输出404响应
			res.status(404).send();
		}  */
		catch (Exception e) 
		{
			echo "异常 - ", e.getMessage();
		}

		//结束输出缓冲
		//die();
		static::obend();
	}

	/**
	 * 调用controller与action
	 *
	 * @param Request req
	 * @param Response res
	 */
	private static fun callcontroller(req, res)
	 {
		// 获得controller类
		class = req.controllerclass();
		if (!classexists(class))
			throw new RouteException("Controller类不存在：".req.controller());

		// 创建controller
		controller = new class(req, res);

		// 获得action方法
		action = "action".req.action();
		if (!methodexists(controller, action))
			throw new RouteException(class."类不存在方法：".action);

		// 调用controller的action方法
		controller.action();
	}


}
