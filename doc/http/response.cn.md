# 响应对象

jkmvc的响应类是 `net.jkcode.jkmvc.http.HttpResponse`

他是继承并代理 `javax.servlet.http.HttpServletResponse`

因此，你可以使用从 `HttpServletResponse` 继承的方法

下面仅列出响应类特有的属性与方法

1. 渲染的方法

方法 | 作用
--- | ---
setStatus(status: Int): Unit | 设置响应状态码
renderView(view: View): Unit | 响应视图
renderString(content: String): Unit | 响应字符串
renderFile(file: File): Unit | 响应文件
renderFile(file: String): Unit | 响应文件

2. 操作缓存的方法

方法 | 作用
--- | ---
setCache(expires: Long): HttpResponse | 设置响应缓存
getCache(): String? | 获得缓存时间

3. 操作cookie的方法

方法 | 作用
--- | ---
deleteCookie(name: String): HttpResponse | 删除cookie
setCookie(name: String, value: String, expiry: Int? = null): HttpResponse | 设置cookie值
setCookies(data: Map<String, String>, expiry: Int? = null): HttpResponse | 设置cookie值