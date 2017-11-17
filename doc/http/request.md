# Request object

Request class is `com.jkmvc.http.Request`, which is implements `javax.servlet.http.HttpServletRequest`

So, you can call all methods that inherited from `HttpServletRequest`

Here is a list of the properties and methods special in `Request`

1. routing

property/method | usage
--- | ---
controller: String | Get current controller
action: String | Get current action
route: Route | Get current route
routeParams: Map<String, String> | Get current route's parameters
parseRoute(): Boolean | parse uri and get route matched


2. check `http method` 

method | usage
--- | ---
isAjax(): Boolean | check whether ajax request
isGet(): Boolean | check whether get request
isMultipartContent(): Boolean | check whether ajax multipart request
isPost(): Boolean | check whether post request
isStaticFile(): Boolean | check whether static file request
isUpload(): Boolean | check whether upload request

3. get routing parameter

method | usage
--- | ---
containsRouteParameter(key: String): Boolean | Check whether contains rouinge parameter
isEmptyRouteParameter(key: String): Boolean | Check whether routing parameter is empty
getRouteParameter(key: String, defaultValue: T? = null): T? | Get routing parameter，Note: call it with a explicit  return type, so it will convert the parameter value to the specified type
getBooleanRouteParameter(key: String, defaultValue: Boolean? = null): Boolean? | Get boolean type of routing parameter
getDateRouteParameter(key: String, defaultValue: Date? = null): Date? | Get Date type of routing parameter
getDoubleRouteParameter(key: String, defaultValue: Double? = null): Double? | Get double type of routing parameter
getFloatRouteParameter(key: String, defaultValue: Float? = null): Float? | Get float type of routing parameter
getIntRouteParameter(key: String, defaultValue: Int? = null): Int? | Get int type of routing parameter
getLongRouteParameter(key: String, defaultValue: Long? = null): Long? | Get long type of routing parameter
getShortRouteParameter(key: String, defaultValue: Short? = null): Short? | Get short type of routing parameter

4. Get `get/post` parameter

method | usage
--- | ---
containsParameter(key: String): Boolean | Check whether contains `get/post` parameter 
isEmptyParameter(key: String): Boolean | Check whether `get/post` parameter is empty
getParameter(key: String, defaultValue: T?): T? | Get `get/post` parameter，Note: call it with a explicit  return type, so it will convert the parameter value to the specified type
getParameter(key: String): String? | Get `get/post` parameter 
getBooleanParameter(key: String, defaultValue: Boolean? = null): Boolean? | Get boolean type of `get/post` parameter
getDateParameter(key: String, defaultValue: Date? = null): Date? | Get Date type of `get/post` parameter
getDoubleParameter(key: String, defaultValue: Double? = null): Double? | Get double type of `get/post` parameter
getFloatParameter(key: String, defaultValue: Float? = null): Float? | Get float type of `get/post` parameter
getIntParameter(key: String, defaultValue: Int? = null): Int? | Get int type of `get/post` parameter
getLongParameter(key: String, defaultValue: Long? = null): Long? | Get long type of `get/post` parameter
getShortParameter(key: String, defaultValue: Short? = null): Short? | Get short type of `get/post` parameter

5. Get uploaded file

method | usage
--- | ---
containsFile(key: String): Boolean | Check whether contains uploaed file
getFile(name: String): File | Get a uploaed file
getFileMap(): Map<String, File> | Get all uploaed files
getFileNames(): Enumeration<String> | Get all uploaed file names
getFileRelativePath(name: String): String | Get a uploaed file's relative path where file is saved
toUploadUrl(relativePath: String): String | Obtain an absolute url based on the uploaded file's relative path

6. Get request parameter（including routing paramter and `get/post` parameter）

method | usage
--- | ---
contains(key: String): Boolean | Check whether contains parameter
isEmpty(key: String): Boolean | Check whether parameter is empty
get(key: String, defaultValue: T? = null): T? | Get request parameter，Note: call it with a explicit  return type, so it will convert the parameter value to the specified type

