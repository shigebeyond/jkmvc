package com.jkmvc.http.router

/**
 * 路由正则处理
 *
 *  1 编译正则
 *  将简化的路由正则，转换为完整的正则：主要是将<参数>替换为子正则
 *
 * <code>
 * 	   // 将 <controller>(\/<action>(\/<id>)?)? 编译为 /([^\/]+)(\/([^\/]+)\/(\d+)?)?/
 *     // 其中参数对子正则的映射关系保存在 paramGroupMapping 中
 *     val route = Route(
 *        "<controller>(\/<action>(\/<id>)?)?",
 *         mapOf(
 *           "controller" to "[a-z]+",
 *           "action" to "[a-z]+",
 *           "id" to "\d+",
 *         )
 *     );
 * </code>
 *
 *  2 匹配路由正则
 * <code>
 *     route.mathes('welcome/index');
 * </code>
 */
interface IRoute{

	/**
	* 原始正则: <controller>(\/<action>(\/<id>)?)?
	 */
	val regex:String

	/**
	 * 参数的子正则
	 */
	val paramRegex:Map<String, String>

	/**
	 * 参数的默认值
	 */
	val defaults:Map<String, String>?

	/**
	 * 检查uri是否匹配路由正则
	 *
	 * @param uri
	 * @return 匹配的路由参数，如果为null，则没有匹配
	 */
	fun match(uri:String):Map<String, String>?;
}