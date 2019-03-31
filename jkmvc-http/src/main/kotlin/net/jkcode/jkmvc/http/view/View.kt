package net.jkcode.jkmvc.http.view

import net.jkcode.jkmvc.http.HttpRequest
import net.jkcode.jkmvc.http.HttpResponse
import net.jkcode.jkmvc.http.router.RouteException
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap

/**
 * 视图
 *   放在web根目录下的jsp文件
 *
 * @author shijianhang
 * @date 2016-10-21 下午3:14:54
 */
open class View(override val req: HttpRequest /* 请求对象 */,
                override val res: HttpResponse /* 响应对象 */,
                override val file:String/* 视图文件 */,
                override var data:MutableMap<String, Any?> /* 局部变量 */
): IView {

	companion object{

		/**
		 * 空map
		 */
		public val emptyMutableMap:MutableMap<String, Any?> = HashMap()

		/**
		 * 全局变量
		 */
		public val globalData: ConcurrentHashMap<String, Any?> = ConcurrentHashMap();

		/**
		 * 设置全局变量
		 * @param key
		 * @param value
		 * @return
		 */
		public fun setGlobal(key:String, value:Any?): Companion {
			globalData.set(key, value);
			return this;
		}
	}

	/**
	 * 设置局部变量
	 * @param key
	 * @param value
	 * @return
	 */
	public override operator fun set(key:String, value:Any?): View {
		this.data[key] = value;
		return this;
	}

	/**
	 * 渲染视图
	 */
	public override fun render(){
		// 设置全局变量
		req.setAttributes(globalData)

		// 设置局部变量
		req.setAttributes(data)

		// 渲染jsp
		val jsp = "/" + file + ".jsp"
		val reqDispatcher = req.getRequestDispatcher(jsp)
		if(reqDispatcher == null)
			throw FileNotFoundException("RequestDispatcher for resource [$jsp] is null")
		// 在 org.akhikhl.gretty 运行环境中只能使用原始的请求与响应
		reqDispatcher.forward(req.request, res.response)
	}

}
