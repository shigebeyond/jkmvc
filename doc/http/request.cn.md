# 请求对象

jkmvc的请求类是 `net.jkcode.jkmvc.http.HttpRequest`

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

3. 获得get/post/路由参数的方法

方法 | 作用
--- | ---
contains(key: String): Boolean | 检查是否有get/post/路由参数
isEmpty(key: String): Boolean | 检查get/post/路由参数是否为空
getParameter(key: String): String? | 获得get/post/路由参数值 
get(key: String, defaultValue: T?): T? | 获得get/post/路由参数值，注：调用时需明确指定返回类型，来自动转换参数值为指定类型
getBoolean(key: String, defaultValue: Boolean? = null): Boolean? | 获得boolean类型的get/post/路由参数值
getDate(key: String, defaultValue: Date? = null): Date? | 获得Date类型的get/post/路由参数值
getDouble(key: String, defaultValue: Double? = null): Double? | 获得double类型的get/post/路由参数值
getFloat(key: String, defaultValue: Float? = null): Float? | 获得float类型的get/post/路由参数值
getInt(key: String, defaultValue: Int? = null): Int? | 获得int类型的get/post/路由参数值
getLong(key: String, defaultValue: Long? = null): Long? | 获得long类型的get/post/路由参数值
getShort(key: String, defaultValue: Short? = null): Short? | 获得short类型的get/post/路由参数值

4. 获得上传文件的方法

方法 | 作用
--- | ---
getPartFile(name: String): File? | 获得某个上传文件
storePartFileAndGetRelativePath(name: String): String | 保存某个上传文件, 并返回其相对路径

5. 文件路径与url的相互转换

方法 | 作用
--- | ---
getFileRelativePath(file: String): String | 获得指定文件的相对路径
getUploadUrl(relativePath: String): String | 获得上传文件的url