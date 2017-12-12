# Request Flow

Every application follows the same flow:

Application's entry is `com.jkmvc.http.JkFilter`, and nd it just call `HttpHandler.handle(req as HttpServletRequest, res as HttpServletResponse)`

Now, let's read `com.jkmvc.http.HttpHandler#handle()`

1. create [Request](request)  object
2. create [Response](response)  object
3. call `req.parseRoute()` to parse uri and its corresponding controller and action.
4. create [Controller](controller) object
5. call Controller object's action method
