package net.jkcode.jkmvc.http.router

/**
 * 路由匹配结果
 * @author shijianhang<772910474@qq.com>
 * @date 2020-04-08 6:00 PM
 */
class RouteResult(
        public val params: Map<String, String>, // 路由实参
        public val route: Route // 路由
){
    /**
     * controller
     */
    public val controller: String
        get() = if(route.isMethodLevel)
                    route.controller!! // 方法级注解路由
                else
                    params["controller"]!! // 全局配置路由

    /**
     * action
     */
    public val action: String
        get() = if(route.isMethodLevel)
                    route.action!! // 方法级注解路由
                else
                    params["action"]!! // 全局配置路由
}