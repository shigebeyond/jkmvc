<?php
namespace php\jkmvc\http; // 定义 IController 类时需要
define('APPPATH', dirname(__FILE__)); // 应用目录

// TODO: 为了优化性能, 可适当减少判断的代码, 如判断文件/类/方法存不存在, 以便减少php代码, 压榨点性能; 这样最后包一层try, 然后转化下对用户友好的异常

/**
 * callController.php负责工作
 *   1 定义controller基类
 *   2 创建controller实例
 *   3 HttpState.setCurrentByController()
 *   4 guardInvoke(), 即调用action
 */
// 1 定义controller基类
if(!class_exists('php\jkmvc\http\IController', FALSE)) // 检查controller基类, 一般不需要, 但如果修改了该文件, 会重新加载就会导致重复创建类
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
                extract($data);

            // 开输出缓冲
            ob_start();

            // 找到视图
            $file = APPPATH . '/views/' . $file . '.php';
            if(!file_exists($file))  // 检查视图文件
                throw new \Exception("View not exists: $file");

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

        /**
         * 转发请求，并返回响应
         *    因为是异步处理, 因此在action方法最后一行必须返回该函数的返回值
         * @param $url
         * @param $useHeaders 是否使用请求头
         * @param $useCookies 是否使用cookie
         * @return 异步响应
         */
        function transferAndReturn($url, $useHeaders = false, $useCookies = false){
            return $this->req->transferAndReturn($url, $this->res, $useHeaders, $useCookies);
        }
    }

// 引入controller文件
$controller = ucfirst($req->controller());
$file = APPPATH . '/controller/' . $controller . '.php';
if(!file_exists($file)) // 检查controller文件
    throw new \Exception("$404[Controller file not exists: $controller]");

include $file;

if(!class_exists($controller, FALSE)) // 检查controller类
    throw new \Exception("$404[Controller class not exists: $controller]");

$action = $req->action();
if (!method_exists($controller, $action)) // 检查action方法
    throw new \Exception("$404[Controller {$controller} has no method: {$action}()]");

// 3 实例化controller
$controller = new $controller($req, $res);

// 4 设置当前http状态
HttpRequest::setCurrentByController($controller);

// 5 调用action方法
$id = $req->param('id');
// 需return, 有可能返回值是 CompleteFuture, 而外部调用方需要
// return $controller->$action($id);
return HttpRequest::guardInvoke($controller, $action, [$id]);