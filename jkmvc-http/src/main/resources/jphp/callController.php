<?php
namespace php\jkmvc\http;
define('APPPATH', dirname(__FILE__)); // 应用目录

// controller基类
abstract class IController {
	public $req; // 请求对象
	public $res; // 响应对象

	public function __construct(HttpRequest $req, HttpResponse $res){
		$this->req = $req;
		$this->res = $res;
	}

    /**
	 * 渲染视图
	 * @param $file
	 * @param $data
	 * @param $is_return 是否返回视图内容，否则直接响应输出
	 * @return string
	 */
	public function view($file, $data = NULL, $is_return = FALSE)
	{
		// 释放变量
		if($data)
		    extract($data, EXTR_REFS | EXTR_SKIP);

		// 开输出缓冲
		ob_start();

		// 找到视图
		$file = APPPATH . '/views/' . $file . '.php';
		if(!file_exists($file))  // 检查视图文件
			throw new Exception("View not exists: $file");

		try {
			// 加载视图, 并输出
			include $file;

			// 获得输出缓存
			$content = ob_get_contents();
			if($is_return)
			    return $content;

			// 响应输出
			$this->res->write($content);
		}
		finally
		{
			// 结束输出缓存
			ob_end_clean();
		}
	}
}

// 引入controller文件
$controller = ucfirst($req->controller());
$file = APPPATH . '/controller/' . $controller . '.php';
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
