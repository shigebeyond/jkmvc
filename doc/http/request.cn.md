# 请求对象

jkmvc的请求类是 `com.jkmvc.http.Request`

他是继承并代理 `javax.servlet.http.HttpServletRequest`

因此，你可以使用从 `HttpServletRequest` 继承的方法

下面仅列出请求类特有的属性与方法

1. 路由相关的属性与方法

属性/方法 | 作用
--- | ---
controller: String | 获得当前controller
action: String | 获得当前action
route: Route | 当前匹配的路由规则
routeParams: Map<String, String> | 当前匹配的路由参数
parseRoute(): Boolean | 解析路由


2. 检查 `http method` 的方法

方法 | 作用
--- | ---
isAjax(): Boolean | 是否ajax请求
isGet(): Boolean | 是否get请求
isMultipartContent(): Boolean | 是否 multipart 请求
isPost(): Boolean | 是否post请求
isStaticFile(): Boolean | 是否是静态文件请求，如果是则不进行路由解析
isUpload(): Boolean | 是否上传文件的请求

3. 获得路由参数的方法

方法 | 作用
--- | ---
containsRouteParameter(key: String): Boolean | 检查是否包含指定路由参数
isEmptyRouteParameter(key: String): Boolean | 检查路由参数是否为空
getRouteParameter(key: String, defaultValue: T? = null): T? | 获得路由参数，注：调用时需明确指定返回类型，来自动转换参数值为指定类型
getBooleanRouteParameter(key: String, defaultValue: Boolean? = null): Boolean? | 获得boolean类型的路由参数
getDateRouteParameter(key: String, defaultValue: Date? = null): Date? | 获得Date类型的路由参数
getDoubleRouteParameter(key: String, defaultValue: Double? = null): Double? | 获得double类型的路由参数
getFloatRouteParameter(key: String, defaultValue: Float? = null): Float? | 获得float类型的路由参数
getIntRouteParameter(key: String, defaultValue: Int? = null): Int? | 获得int类型的路由参数
getLongRouteParameter(key: String, defaultValue: Long? = null): Long? | 获得long类型的路由参数
getShortRouteParameter(key: String, defaultValue: Short? = null): Short? | 获得short类型的路由参数

4. 获得get/post参数的方法

方法 | 作用
--- | ---
containsParameter(key: String): Boolean | 检查是否有get/post参数 
isEmptyParameter(key: String): Boolean | 检查get/post参数是否为空
getParameter(key: String, defaultValue: T?): T? | 获得get/post参数值，注：调用时需明确指定返回类型，来自动转换参数值为指定类型
getParameter(key: String): String? | 获得get/post参数值 
getBooleanParameter(key: String, defaultValue: Boolean? = null): Boolean? | 获得boolean类型的get/post参数值
getDateParameter(key: String, defaultValue: Date? = null): Date? | 获得Date类型的get/post参数值
getDoubleParameter(key: String, defaultValue: Double? = null): Double? | 获得double类型的get/post参数值
getFloatParameter(key: String, defaultValue: Float? = null): Float? | 获得float类型的get/post参数值
getIntParameter(key: String, defaultValue: Int? = null): Int? | 获得int类型的get/post参数值
getLongParameter(key: String, defaultValue: Long? = null): Long? | 获得long类型的get/post参数值
getShortParameter(key: String, defaultValue: Short? = null): Short? | 获得short类型的get/post参数值

5. 获得请求参数（包含路由参数与get/post参数）的方法

方法 | 作用
--- | ---
contains(key: String): Boolean | 判断请求是否包含指定参数
isEmpty(key: String): Boolean | 判断请求参数是否为空
get(key: String, defaultValue: T? = null): T? | 获得请求参数，先从路由参数中取得，如果没有，则从get/post参数中取，注：调用时需明确指定返回类型，来自动转换参数值为指定类型