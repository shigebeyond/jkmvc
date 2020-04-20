# Routing

Jkmvc provides a very powerful routing system.  In essence, routes provide an interface between the urls and your controllers and actions.  With the correct routes you could make almost any url scheme correspond to almost any arrangement of controllers.

As mentioned in the [Request Flow](flow.md) section, a request is handled by the `HttpRequest` class, which will look for a matching Route and load the appropriate controller to handle that request.

[!!] It is important to understand that *routes are matched in the order they are added*, and as soon as a URL matches a route, routing is essentially "stopped" and *the remaining routes are never tried*.  Because the default route matches almost anything, including an empty url, new routes must be place before it.

## 1 Define routing configuration

If you look in `jkmvc/jkmvc-orm/src/main/resources/routes.yaml` you will see the "default" routing configuration as follows:

```
# 1.1 route name
default:
  # 1.2 uri pattern
  regex: <controller>(/<action>(/<id>)?)?
  # 1.3 parameters regex
  paramRegex:
    id: \d+
  # 1.4 default parameters
  defaults:
    controller: welcome
    action: index
```

[!!] The default route is simply provided as a sample, you can remove it and replace it with your own routes.

So this creates a route with the name `default` that will match urls in the format of `<controller>(/<action>(/<id>)?)?`.  

Let's take a closer look at each of routing configuration item.

### 1.1 route name

The name of the route must be a **unique** string.  If it is not it will overwrite the older route with the same name. The name is used for creating urls by reverse routing, or checking which route was matched.

### 1.2 uri pattern

The uri is a string that represents the format of urls that should be matched.  The tokens surrounded with `<>` are *keys* and anything surrounded with `()?` are *optional* parts of the uri. In Jkmvc routes, any character is allowed and treated literally aside from `()<>`.  The `/` has no meaning besides being a character that must match in the uri.  Usually the `/` is used as a static seperator but as long as the regex makes sense, there are no restrictions to how you can format your routes.

Lets look at the default route again, the uri is `<controller>(/<action>(/<id>)?)?`.  We have three keys or params: `controller`, `action`, and `id`. But `action` and `id` are optional.

And a blank uri would match default controller and action (set by `defaults` configuration item) would be assumed resulting in the `WelcomeController` class being loaded and the `index` method being called to handle the request.

You can use any name you want for your keys, but the following keys have special meaning to the [HttpRequest](request.md) object, and will influence which controller and action are called:

 * **controller** - The controller that the request should execute.
 * **action** - The action method to call.

### 1.3 parameters regex

The Jkmvc route system uses `kotlin.text.Regex` in its matching process.  By default each key (surrounded by `<>`) will match `[^\\/]+` (or in english: anything that is not a slash.  You can define your own patterns for each key by `paramRegex` configuration item.

And I also suggest you to use and follow jkmvc's convention routing configuration. It's simple.

### 1.4 default parameters

If a key in a route is optional (or not present in the route), you can provide a default value for that key by setting `defaults` configuration item.  This can be useful to provide a default controller or action for your site, among other things.

[!!] The `controller` and `action` key must always have a value, so they either need to be required in your route (not inside of parentheses) or have a default value provided.

[!!] Jkmvc automatically converts controllers to follow the standard naming convention. For example /user/detail/123 would look for the controller `UserController` in `controllerPackages` configured in `http.yaml` and trigger the `detail()` method on it.

In the default route, all the keys are optional, and the controller and action are given a default.   If we called an empty url, the defaults would fill in and `WelcomeController::index()` would be called.  If we called `user` then only the default for action would be used, so it would call `UserController::index()` and finally, if we called `user/detail` then neither default would be used and `UserController::detail()` would be called.

You can also use defaults to set a key that isn't in the route at all.

### 1.5 default route's parsing process

uri | controller#action
--- | ---
/ | WelcomeController#index()
user | UserController#index()
user/detail | UserController#detail()
user/detail/1 | UserController#detail()，you can call `req.req.getInt("id")` to get route parameter `id`

## 2 Get routing parameters

The `controller` and `action` can be accessed from the [HttpRequest](request.md) as public properties like so:

```
	// From within a controller:
	req.action;
	req.controller;
	req.directory;

	// Can be used anywhere:
	HttpRequest.current().action;
	HttpRequest.current().controller;
	HttpRequest.current().directory;
```

All other keys specified in a route can be accessed via `HttpRequest::get(key)` or `HttpRequest::getParameter(key)`(exclude `controller`/`action` in default route):

```
	// From within a controller:
	req.get('key_name'); // return T
	req.getParameter('key_name'); // return String

	// Can be used anywhere:
	HttpRequest.current().get('key_name');
```

The `HttpRequest.get(key, default)` method takes an optional second argument to specify a default return value in case the key is not set by the route. If no arguments are given, all keys are returned as an associative array.

For example, with the following route:

```
# route name
default:
  # uri pattern
  regex: ad/<ad>(/<affiliate>)?
  # parameters regex
  paramRegex:
    id: \d+
  # default parameters
  defaults:
    controller: ads
    action: index
```

If a url matches the route, then `AdsController::index()` will be called.  You can access the parameters by using  `req.get(key)` or `req.getParameter(key)` in controller (exclude `controller`/`action` in default route).

```
class WelcomeController: Controller() {

    public fun index() {
        val ad:String = req.get("ad")!!
        val affiliate:String? = req.get("affiliate", null) // second parameter for default value
    }
}
```

