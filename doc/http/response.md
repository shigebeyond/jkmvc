# Response object

Response class is `net.jkcode.jkmvc.http.HttpResponse`, which is implements `javax.servlet.http.HttpServletResponse`

So, you can call all methods that inherited from `HttpServletResponse`

Here is a list of the properties and methods special in `HttpResponse`

1. Render methods

method | usage
--- | ---
setStatus(status: Int): Unit | set response status code
renderView(view: View): Unit | render view
renderHtml(content: String): Unit | render html
renderText(content: String): Unit | render text
renderJson(content: Any): Unit | render json
renderXml(content: Any): Unit | render xml
renderJs(content: Any): Unit | render js
renderFile(file: File): Unit | render file
renderFile(file: String): Unit | render file

2. Cache Manipulating methods

method | usage
--- | ---
setCache(expires: Long): HttpResponse | set cache time
getCache(): String? | get cache time

3. Cookie Manipulating methods

method | usage
--- | ---
deleteCookie(name: String): HttpResponse | delete cookie
setCookie(name: String, value: String, expiry: Int? = null): HttpResponse | set a cookie 
setCookies(data: Map<String, String>, expiry: Int? = null): HttpResponse | set multiple cookies