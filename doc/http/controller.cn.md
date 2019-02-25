# 控制器

控制器 Controller 是介于模型 Model 和视图 View 之间的负责协调的类。 他将请求数据传递给模型，以便由模型来读写数据。 然后把模型返回的数据，传递给视图来渲染，最终的输出到浏览器。 控制器负责控制web应用的流程。

控制器在 `net.jkcode.jkmvc.http.HttpHandler#callController` 中调用，只调用匹配路由的控制器。详情请阅读[routing](routing.cn.md)。

## 1 创建控制器

创建一个继承 `net.jkcode.jkmvc.http.Controller` 的子类，类名以`Controller`作为后缀。

请看我们的第一个控制器类

```
package net.jkcode.jkmvc.example.controller

import net.jkcode.jkmvc.http.Controller

/**
 * 主页控制器
 */
class WelcomeController: Controller() {

    /**
     * 操作方法，用于响应uri "welcome/index"
     */
    public fun actionIndex() {
        res.renderString("hello world");
    }
}
```

## 2 注册控制器

你要先配置控制器所在的包路径，这样 jkmvc 会收集该包下的所有控制器类，并在请求进来时调用

vim src/main/resources/http.yaml

```
# controller类所在的包路径
controllerPackages:
    - net.jkcode.jkmvc.example.controller
```

## 3 `req` 属性

每个控制器都有一个 `req` 属性，他是 [HttpRequest](request.cn.md) 对象，代表当前请求。

当然，在控制器之外，你也可以通过 `HttpRequest.current()` 来获得当前请求。

以下列出常用 `req` 的属性与方法。详情请参考 [HttpRequest](request.cn.md)

属性/方法 | 作用
--- | ---
[req.route](route.cn.md) | 匹配当前url的路由对象
req.controller, <br /> req.action | 当前匹配路由的 controller / action
req.routeParams | 匹配路由的所有参数，包含 controller / action

## 4 `res` 属性

每个控制器都有一个 `res` 属性，他是 [HttpResponse](response.cn.md) 对象。

属性/方法 | 作用
--- | ---
req.setStatus(status:Int)| 设置响应的状态码
res.renderString(content:String) | 设置响应内容为文本
res.renderFile(file: File) | 设置响应内容为文件
res.renderFile(file: String) | 设置响应内容为文件
res.renderView(view:View) | 设置响应内容为视图
res.setHeader(name:String, value:String) | 设置响应头

## 5 Action 操作

Action 操作，其实就是控制器的一个方法，但定义必须满足以下条件
1. public方法
2. 以 `Action` 作为后缀

操作是真正处理请求的方法，包含所有逻辑代码。

每个操作方法都应该 `res.renderXXX(sth)` 来给浏览器响应内容，除非请求被重定向。

我们来看看一个简单的操作方法，如加载 [view](view.cn.md) 视图文件

```
	public function indexAction()
	{
		res.renderView(view("user/detail")); // This will load webapps/user/detail.jsp
	}
```

## 6 路由参数

你可以通过 `req.getRouteParameter('name')` 来访问路由参数，其中 `name` 是定义在路由规则中的参数名

### 6.1 定义路由规则

vim src/main/resources/routes.yaml

```
# 路由名
default:
  #  url正则
  regex: <controller>(/<action>(/<id>)?)?
  #  参数子正则
  paramRegex:
    id: \d+
  # 默认参数值
  defaults:
    controller: welcome
    action: index
```

### 6.2 在控制器中获得路由参数

```
	public function detailAction()
	{
		val id:Int = req.getRouteParameter('id');
		val action:String = req.getRouteParameter('action');
		val username:String = req.getRouteParameter('username', null); // 第二个参数设置默认值
	}
```

## 7 `res`属性

你可以使用 `res.renderXXX()` 方法来向浏览器返回渲染结果

注意：

1. 在调用 `res.renderXXX()` 方法后程序并不会立即返回，如果需要立即返回，要使用 `return` 语句
2. 在一个action方法中多次调用 `res.renderXXX()` 方法只有最后一次有效。


## 8 例子

显示用户详情的操作方法

```
class UserController: Controller()
{
    /**
     * 用户详情页
     */
    public fun detailAction()
    {
        // 获得路由参数id: 2种写法
        // val id = req.getIntRouteParameter("id"); // req.getRouteParameter["xxx"]
        val id:Int? = req["id"] // req["xxx"]
        // 查询单个用户
        //val user = UserModel.queryBuilder().where("id", id).findModel<UserModel>()
        val user = UserModel(id)
        if(!user.isLoaded()){
            res.renderString("用户[$id]不存在")
            return
        }
        // 渲染视图
        val view = view("user/detail")
        view["user"] = user; // 设置视图参数
        res.renderView(view)
    }
}
```

## 9 事件

每个controller都有2个事件

1. action方法的前置事件，通过重写 `before()` 方法来处理
2. action方法的后置事件，通过重写 `after()` 方法来处理

```
class UserController: Controller()
{
    /**
     * action前置处理
     */
    public override fun before() {
        // 如检查权限
        httpLogger.info("action前置处理")
    }

    /**
     * action后置处理
     */
    public override fun after() {
        // 如记录日志
        httpLogger.info("action后置处理")
    }
}
```
