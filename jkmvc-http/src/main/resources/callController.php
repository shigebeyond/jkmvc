<?php
// 引入controller文件
$controller = ucfirst($req->controller());
$file = dirname(__FILE__) . $controller . '.php';
include $file;

// 实例化controller
$controller = new $controller($req, $res);

// 调用action方法
$action = $req->action();
$controller->$action();
