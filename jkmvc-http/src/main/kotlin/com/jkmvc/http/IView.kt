package com.jkmvc.http

/**
 * 视图
 *
 * @author shijianhang
 * @date 2016-10-21 下午3:14:54  
 */
interface IView
{
	/**
	* 请求对象
	 */
	val req: Request

	/**
	 * 响应对象
	 */
	val res: Response

	/**
	 * 视图文件
	 */
	val file:String

	/**
	 * 局部变量
	 */
	var data:MutableMap<String, Any?>

	/**
	 * 设置局部变量
	 * @param key
	 * @param value
	 * @return
	 */
	public operator fun set(key:String, value:Any?): View;

	/**
	 * 渲染视图
	 */
	public fun render();
}