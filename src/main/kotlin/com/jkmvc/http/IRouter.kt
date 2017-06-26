package com.jkmvc.http

/**
 * 路由器
 *	1 加载路由规则
 *	2 解析路由：匹配规则
 *
 * @author shijianhang
 * @date 2016-10-6 上午12:01:17
 *
 */
interface IRouter
{
	/**
	 * 要跳过的目录
	 * 　　对指定目录下的uri不进行路由解析，主要用于处理静态文件或上传文件
	 */
	var skipedDirectories: Array<String>?

	/**
	 * 添加路由
	 * @param name 路由名
	 * @parma route 路由对象
	 */
	fun addRoute(name:String, route:Route): Router

	/**
	 * 是否跳过解析该url
	 * 　　对属性 skipedDirectories 指定目录下的uri不进行路由解析，主要用于处理静态文件或上传文件
	 * @return
	 */
	fun isSkip(uri:String): Boolean

	/**
	 * 解析路由：匹配规则
	 * @param uri
	 * @return [路由参数, 路由规则]
	 */
	fun parse(uri:String):Pair<Map<String, String>, Route>?;
}