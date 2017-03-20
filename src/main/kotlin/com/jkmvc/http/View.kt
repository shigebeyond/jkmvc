package com.jkmvc.http

import java.util.concurrent.ConcurrentHashMap

/**
 * 视图
 *
 * @Package packagename 
 * @category 
 * @author shijianhang
 * @date 2016-10-21 下午3:14:54  
 *
 */
class View(protected val req: Request /* 请求对象 */, protected val res: Response /* 响应对象 */, protected val file:String/* 视图文件 */, protected var data:MutableMap<String, Any?> /* 局部变量 */)
{
	companion object{
		/**
		 * 全局变量
		 * @var array
		 */
		protected val globalData:MutableMap<String, Any?> = ConcurrentHashMap<String, Any?>();

		/**
		 * 设置全局变量
		 * @param string key
		 * @param mixed value
		 * @return View
		 */
		public fun setGlobal(key:String, value:Any?): Companion {
			globalData.set(key, value);
			return this;
		}
	}

	/**
	 * 设置局部变量
	 * @param string key
	 * @param mixed value
	 * @return View
	 */
	public fun set(key:String, value:Any?): View {
		this.data[key] = value;
		return this;
	}

	/**
	 * 渲染视图
	 */
	public fun render(){

		// 设置全局变量
		renderData(globalData)

		// 设置局部变量
		renderData(data)

		// 渲染jsp
		req.getRequestDispatcher(file).forward(req, res)
	}

	/**
	* 设置渲染参数
	 */
	private fun renderData(data:MutableMap<String, Any?>) {
		if (data != null)
			for ((k, v) in data)
				req.setAttribute(k, v);
	}

}