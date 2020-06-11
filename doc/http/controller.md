# Controller

A Controller stands in between the models and the views in an application. It passes information on to the model when data needs to be changed and it requests information from the model when data needs to be loaded.

Controllers then pass on the data of the model to the views where the data is used to render for the users.  Controllers essentially control the flow of the application.

Controllers are called by the `net.jkcode.jkmvc.http.HttpHandler#callController` function based on the Route that the url matched.  Be sure to read the [routing](routing.md) page to understand how to use routes to map urls to your controllers.

## 1 Create Controller

Create a subclass that extends parent class `net.jkcode.jkmvc.http.Controller`, with `Controller` postfix.

Now, we have our first Controller

```
package net.jkcode.jkmvc.example.controller

import net.jkcode.jkmvc.http.Controller

/**
 * home controller
 */
class WelcomeController: Controller() {

    /**
     * action，response to uri "welcome/index"
     */
    public fun actionIndex() {
        res.renderHtml("hello world");
    }
}
```

## 2 Register Controller

Configure controller classes's package paths, so jkmvc can collect all controller classes in this package, and call it when request comes.

vim src/main/resources/http.yaml

```
# controller classes's package paths
controllerPackages:
    - net.jkcode.jkmvc.example.controller
```

## 3 `req` property

Every controller has the `req` property which is the [HttpRequest](request.md) object and represents current request. 

Outside controler, you can get current request by `HttpRequest.current()`

Here is a partial list of the properties and methods available to `req`. See the [HttpRequest](request.md) class for more information on any of these.

Property/method | What it does
--- | ---
[req.route](route.md) | The Route that matched the current request url
req.controller, <br /> req.action | The controller and action that matched for the current route
req.routeParams | params which is defined in your route, including controller/action

## 4 `res` property

Every controller has the `res` property which is the [HttpResponse](response.md) object. 

Property/method | What it does
--- | ---
req.setStatus(status:Int)|Set HTTP status for the request (200, 404, 500, etc.)
res.renderHtml(content:String) | Set content to return for this request
res.renderFile(file: File) | Set file to return for this request
res.renderFile(file: String) | Set file to return for this request
res.renderView(view:View) | Set view to return for this request
res.setHeader(name:String, value:String) | Set HTTP headers to return with the response


## 5 Action method

You create actions for your controller just by defining a public function.

An action method handles the current request, it contains all logic code for this request. 

Every action should call `res.renderXXX(sth)` to send sth to the browser, unless it redirected.

A very basic action method that simply loads a [view](view.md) file.

```
	public function index()
	{
		res.renderView(view("user/detail")); // This will load webapps/user/detail.jsp
	}
```

## 6 Route parameters

Route parameters are accessed by calling `req.get('name')` or `req.getParameter('name')` where `name` is the parameter name defined in the route.

### 6.1 Define routing configuration

vim src/main/resources/routes.yaml

```
# route name
default:
  #  uri pattern
  regex: <controller>(/<action>(/<id>)?)?
  # param pattern
  paramRegex:
    id: \d+
  # default param
  defaults:
    controller: welcome
    action: index
```

### 6.2 Get route parameters in Controller

```
	public function detail()
	{
		val id:Int = req.get('id');
		val action:String = req.get('action');
		val username:String = req.get('username', null); // the second parameter will give default value if that param is not set.
```

## 7 `res`属性

You can use `res.renderXXX()` to render sth to browser

Note：

1. After calling `res.renderXXX()` won't return immediately，so you must call `return`
2. If you call `res.renderXXX()` multiple times in an action, but only the last call works

## 8 Examples

A view action for a user detail page.

```
class UserController: Controller()
{
    /**
     * user detail page
     */
    public fun detail()
    {
        // 2 ways to get route parameter: "id"
        // val id = req.getInt("id");
        val id:Int? = req["id"] // req["xxx"]
        // find a user
        //val user = UserModel.queryBuilder().where("id", id).findModel<UserModel>()
        val user = UserModel(id)
        if(!user.isLoaded()){
            res.renderHtml("user[$id] not exist")
            return
        }
        // render view
        val view = view("user/detail")
        view["user"] = user; // set view data
        res.renderView(view)
    }
}
```

## 9 Event

Every controller has 2 events:

1. `before action` event: you can override `before()` method to handle it
2. `after action` event: you can override `after()` method to handle it

```
class UserController: Controller()
{
    /**
     * Automatically executed before the controller action
     */
    public override fun before() {
        // eg. do authorization checks
        httpLogger.info("before action")
    }

    /**
     * Automatically executed after the controller action
     */
    public override fun after() {
        // eg. write logs
        httpLogger.info("after action")
    }
}
```