package net.jkcode.jkmvc.http.router

/**
 * 路由匹配结果
 * @author shijianhang<772910474@qq.com>
 * @date 2020-04-08 6:00 PM
 */
class RouteResult(
        public val params: Map<String, String>, // 路由实参
        public val route: Route // 路由
)