# Request object

Request class is `net.jkcode.jkmvc.http.HttpRequest`, which is implements `javax.servlet.http.HttpServletRequest`

So, you can call all methods that inherited from `HttpServletRequest`

Here is a list of the properties and methods special in `HttpRequest`

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

3. Get `get/post/route` parameter

method | usage
--- | ---
contains(key: String): Boolean | Check whether contains `get/post/route` parameter
isEmpty(key: String): Boolean | Check whether `get/post/route` parameter is empty
getParameter(key: String): String? | Get `get/post/route` parameter
get(key: String, defaultValue: T?): T? | Get `get/post/route` parameter，Note: call it with a explicit  return type, so it will convert the parameter value to the specified type
getBoolean(key: String, defaultValue: Boolean? = null): Boolean? | Get boolean type of `get/post/route` parameter
getDate(key: String, defaultValue: Date? = null): Date? | Get Date type of `get/post/route` parameter
getDouble(key: String, defaultValue: Double? = null): Double? | Get double type of `get/post/route` parameter
getFloat(key: String, defaultValue: Float? = null): Float? | Get float type of `get/post/route` parameter
getInt(key: String, defaultValue: Int? = null): Int? | Get int type of `get/post/route` parameter
getLong(key: String, defaultValue: Long? = null): Long? | Get long type of `get/post/route` parameter
getShort(key: String, defaultValue: Short? = null): Short? | Get short type of `get/post/route` parameter

4. Get uploaded file

method | usage
--- | ---
getPartFile(name: String): File? | Get a uploaded file
storePartFileAndGetRelativePath(name: String): String | Save a uploaded file, and return its relative path where file is saved

5. File Path vs Url

method | usage
--- | ---
getFileRelativePath(file: String): String | Obtain an relative path based on file
getUploadUrl(relativePath: String): String | Obtain an absolute url based on the uploaded file's relative path

