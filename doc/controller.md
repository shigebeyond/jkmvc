# Controllers

A Controller stands in between the models and the views in an application. It passes information on to the model when data needs to be changed and it requests information from the model when data needs to be loaded.

Controllers then pass on the data of the model to the views where the data is used to render for the users.  Controllers essentially control the flow of the application.

Controllers are called by the `com.jkmvc.http.Server#callController` function based on the [Route] that the url matched.  Be sure to read the [routing](routing) page to understand how to use routes to map urls to your controllers.

## 1 Create Controller

Create a subclass that extends parent class `com.jkmvc.http.Controller`

Now, we have our first Controller

```
package com.jkmvc.example.controller

import com.jkmvc.http.Controller

/**
 * controller
 */
class WelcomeController: Controller() {

    /**
     * action，response to uri "welcome/index"
     */
    public fun indexAction() {
        res.render("hello world");
    }
}
```

## 2 Register Controller

configure controller classes's package paths

vim src/main/resources/http.yaml

```
# controller类所在的包路径
# controller classes's package paths
controllerPackages:
    - com.jkmvc.example.controller
```

## 3 `req` property

Every controller has the `req` property which is the [Request](request) object and represents current request. 

Outside controler, you can get current request by `Request.current()`

Here is a partial list of the properties and methods available to `request`. See the [Request](request) class for more information on any of these.

Property/method | What it does
--- | ---
[req.route()](../api/Request#property:route) | The [Route] that matched the current request url
[req.directory()](../api/Request#property:directory), <br /> [req.controller](../api/Request#property:controller), <br /> [req.action](../api/Request#property:action) | The directory, controller and action that matched for the current route
[req.param()](../api/Request#param) | Any other params defined in your route

## response
[response->body()](../api/Response#property:body) | The content to return for this request
[response->status()](../api/Response#property:status) | The HTTP status for the request (200, 404, 500, etc.)
[response->headers()](../api/Response#property:headers) | The HTTP headers to return with the response


## Actions

You create actions for your controller by defining a public function with an `action_` prefix.  Any method that is not declared as `public` and prefixed with `action_` can NOT be called via routing.

An action method will decide what should be done based on the current request, it *controls* the application.  Did the user want to save a blog post?  Did they provide the necessary fields?   Do they have permission to do that?  The controller will call other classes, including models, to accomplish this.  Every action should set `response->body($view)` to the [view file](mvc/views) to be sent to the browser, unless it redirected or otherwise ended the script earlier.

A very basic action method that simply loads a [view](mvc/views) file.

	public function action_hello()
	{
		response->body(View::factory('hello/world')); // This will load views/hello/world.php
	}

### Parameters

Parameters are accessed by calling `req.param('name')` where `name` is the name defined in the route.

	// Assuming Route::set('example','<controller>(/<action>(/<id>(/<new>)))');

	public function action_foobar()
	{
		$id = req.param('id');
		$new = req.param('new');

If that parameter is not set it will be returned as NULL.  You can provide a second parameter to set a default value if that param is not set.

	public function action_foobar()
	{
		// $id will be false if it was not supplied in the url
		$id = req.param('user',FALSE);

### Examples

A view action for a product page.

	public function action_view()
	{
		$product = new Model_Product(req.param('id'));

		if ( ! $product->loaded())
		{
			throw HTTP_Exception::factory(404, 'Product not found!');
		}

		response->body(View::factory('product/view')
			->set('product', $product));
	}

A user login action.

	public function action_login()
	{
		$view = View::factory('user/login');

		if (req.post())
		{
			// Try to login
			if (Auth::instance()->login(req.post('username'), req.post('password')))
			{
				redirect('home', 303);
			}

			$view->errors = 'Invalid email or password';
		}

		response->body($view);
	}
