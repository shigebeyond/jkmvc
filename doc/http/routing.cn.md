# 路由

jkmvc提供了非常强大的路由系统。本质上，路由就是将url映射到对应的控制器和操作(Controller::action)上。使用正确的路由，几乎可以轻轻松松将大部分url都映射到对应的 Controller 上。

在前文的[请求处理流程](flow.md)中提到，每个请求都是由 `HttpRequest` 类来处理的，它会找到一个匹配当前url的路由，并加载对应的 Controller 该请求。

注意: 多个路由的匹配是有序的，路由的匹配顺序就是它们的添加顺序: 多个路由按照其添加的顺序逐个匹配，一旦前面的路由匹配成功，则后面的路由就直接忽略了.

## 1 路由配置

如果你看 `jkmvc/jkmvc-orm/src/main/resources/routes.yaml` 你会看到默认路由如下:

```
# 1.1 路由名
default:
  # 1.2 uri正则
  regex: <controller>(/<action>(/<id>)?)?
  # 1.3 参数子正则
  paramRegex:
    id: \d+
  # 1.4 默认参数值
  defaults:
    controller: welcome
    action: index
```

注意: 默认路由只是提供一个示例,你可以删除和替换为自己的路由规则。

这只是创建了一个名为 `default` 的路由，它将匹配形如 `<controller>(/<action>(/<id>)?)?` 的uri.

我们来看看详细的路由配置项

### 1.1 路由名

路由名必须是唯一的，不能存在两个同名的路由，否则重复设置会覆盖之前的路由. 路由名用于创建uri，或者标识哪个路由被匹配了.

### 1.2 uri正则

uri是一个字符串，用于标识url中被匹配的部分。用 `<>` 包住的变量名和用 `()?` 包住的任何内容，都是uri的可选部分，换句话说匹配的uri中可以没有该部分的内容。 在Jkmvc的路由uri中，你可以写除了 `()<>` 之外的任何字符，它们都会原样匹配uri。 `/`只是uri分隔符，不用于转义，它会原样匹配uri，切勿混淆。

默认路由中的uri是 `<controller>(/<action>(/<id>)?)?`。 在这里我们有三个参数: `controller`，`action` 和 `id`，其中`action` 和 `id`是可选的，也就是可以省略。

路由可以匹配一个空白uri，这时候匹配的 `controller` 与 `action`都是默认值(用 `defaults` 来设置的默认值)，这会调用 `WelcomeController.indexAction` 来处理请求。

关于参数名，你可以随意命名，但是以下的参数名在[HttpRequest](request.md)对象中有特殊含义的，会影响哪个 `controller` 与 `action` 被调用

 * **controlelr** - 处理请求的控制器
 * **action** - 要调用的操作方法，就是 `Controller` 类中的 `Action` 后缀的方法

### 1.3 参数子正则

Jkmvc路由使用`kotlin.text.Regex`来匹配uri。每个参数(就是被`<>`包住的部分)都会有自己的正则表达式，而它们默认的正则表达式是 `[^\\/]+`。你可以通过指定`paramRegex`来定义自己的参数正则。

但是我建议你使用系统默认的路由配置，这样最简单。

### 1.4 默认参数值

如果路由中的某个参数是可选的，或压根就没写在路由上，但是我们还是希望能够找到相应的 `Controller` 与 `action` 来处理请求，此时我们可以通过`defaults`配置项来指定参数的默认值（键是参数名，值是参数的默认值）。 这样很简单就为你的网站提供一个默认的 `controller` 与 `action`。

注意: 参数`controller` 与 `action` 必须要有值，否则无法正常处理请求，因此你要不在uri中定义这些参数，要不就给它们一个默认值.

注意: Jkmvc会根据标准的命名约定，自动将参数`controller` 与 `action`转换为对应的类与方法来调用。 如对uri `/user/detail/123`，Jkmvc会找到配置文件`http.yaml` 的配置项 `controllerPackages` 定义的包路径下的类`UserController`，并调用其方法 `detailAction()`。

在默认路由中，所有的参数都是可选的，同时参数`controller` 与 `action`都有默认值。 如果我们访问一个空的url，将调用默认的控制器与操作方法 `WelcomeController::indexAction()`。 

如果我们访问 `user`，只会应用默认的操作方法，所以它会调用 `UserController::indexAction()` .

最后，如果我们访问 `user/detail`，不会应用控制器与操作方法的默认值，所以它会调用 `UserController::detailAction()`。

### 1.5 默认路由的解析例子

uri | controller#action
--- | ---
/ | WelcomeController#indexAction()
user | UserController#indexAction()
user/detail | UserController#detailAction()
user/detail/1 | UserController#detailAction()，其中通过`req.req.getIntRouteParameter("id")` 可获得id参数值为1

## 2 获得路由参数

对路由参数 `controller`和`action`，在 [HttpRequest](request.md) 中可以通过公共属性来直接访问

```
	// 在 `Controller` 内:
	req.action;
	req.controller;
	req.directory;

	// 在任何地方:
	HttpRequest.current().action;
	HttpRequest.current().controller;
	HttpRequest.current().directory;
```

而其他路由参数，可以通过 `HttpRequest::getRouteParameter(key)` 来访问

```
	// 在 `Controller` 内:
	req.getRouteParameter('key_name');

	// 在任何地方:
	HttpRequest.current().getRouteParameter('key_name');
```

在`HttpRequest.getRouteParameter(key, default)` 方法中，第二个参数是可选的，用来指定一个默认的返回值. 当路由参数中不存在名为 key 的参数，就会返回这个默认值。 如果没有给出 key，则返回所有的路由参数。

给出例子:

```
# 路由名
default:
  # uri正则
  regex: ad/<ad>(/<affiliate>)?
  # 参宿子正则
  paramRegex:
    id: \d+
  # 默认参数值
  defaults:
    controller: ads
    action: index
```

如果当前url匹配该路由，则调用 `AdsController::indexAction()` 来处理请求。在 Controller 中你可以通过 `req.getRouteParameter(key)` 来访问路由参数：

```
class WelcomeController: Controller() {

    public fun indexAction() {
        val ad:String = req.getRouteParameter("ad")!!
        val affiliate:String? = req.getRouteParameter("affiliate", null) // 第二个参数用来设置参数默认值
    }
}
```
