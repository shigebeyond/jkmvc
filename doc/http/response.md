# Response object

Response class is `com.jkmvc.http.Response`, which is implements `javax.servlet.http.HttpServletResponse`

So, you can call all methods that inherited from `HttpServletResponse`

Here is a list of the properties and methods special in `Response`

1. Render methods

method | usage
--- | ---
setStatus(status: Int): Unit | set response status code
renderView(view: View): Unit | render view
renderString(content: String): Unit | render string
renderFile(file: File): Unit | render file
renderFile(file: String): Unit | render file

2. Cache Manipulating methods

method | usage
--- | ---
setCache(expires: Long): Response | set cache time
getCache(): String? | get cache time

3. Cookie Manipulating methods

method | usage
--- | ---
deleteCookie(name: String): Response | delete cookie
setCookie(name: String, value: String, expiry: Int? = null): Response | set a cookie 
setCookies(data: Map<String, String>, expiry: Int? = null): Response | set multiple cookies