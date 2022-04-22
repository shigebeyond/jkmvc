<?php
namespace php\jkmvc\http;

// controller基类
abstract class IController {
	public $req; // 请求对象
	public $res; // 响应对象

	public function __construct(HttpRequest $req, HttpResponse $res){
		$this->req = $req;
		$this->res = $res;
	}
}

// 引入controller文件
$controller = ucfirst($req->controller());
$file = dirname(__FILE__) . '/' . $controller . '.php';
if(!file_exists($file)) // 检查controller文件
    throw new Exception("$404[Controller file not exists: $controller]");

include $file;

if(!class_exists($controller, FALSE)) // 检查controller类
    throw new Exception("$404[Controller class not exists: $controller]");

$action = $req->action();
if (!method_exists($controller, $action)) // 检查action方法
    throw new Exception("$404[Controller {$controller} has no method: {$action}()]");

// 实例化controller
$controller = new $controller($req, $res);

// 调用action方法
$controller->$action();
