# 请求处理流程

每个应用都遵守同样的请求处理流程：

应用入口是 `com.jkmvc.http.JkFilter`, 他的实现只是简单的调用 `HttpHandler.handle(req as HttpServletRequest, res as HttpServletResponse)`

现在，让我们看看 `com.jkmvc.http.HttpHandler#handle()`的实现

1. 创建 [Request](request.cn.md) 对象
2. 创建 [Response](response.cn.md)  对象
3. 调用 `req.parseRoute()` 来解析uri，及其相应的controller与action
4. 创建 [Controller](controller.cn.md) 对象
5. 调用 Controller 对象的 action 方法
