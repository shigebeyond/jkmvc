<?php
namespace php\jkmvc\http;

/**
 * Class HttpRequest
 * @package php\jkmvc\http
 */
class HttpRequest
{
    protected function __construct()
    {
    }

    /**
     * @param $name
     * @return string
     */
    function header($name)
    {
    }

    /**
     * @param $name
     * @return string
     */
    function param($name)
    {
    }

    /**
     * @return string
     */
    function query()
    {
    }

    /**
     * @return string
     */
    function uri()
    {
    }

    /**
     * @return string
     */
    function routeUri()
    {
    }

    /**
     * @return string
     */
    function method()
    {
    }

    /**
     * @return string
     */
    function sessionId()
    {
    }

    /**
     * 设置上传的子目录(上传文件要存的子目录)
     *   要在调用file()之前设置
     * @param $uploadSubDir
     */
    function setUploadSubDir($uploadSubDir) {
    }

    /**
     * 保存上传文件, 并返回相对路径
     * @param $name
     * @return string
     */
    function file($name) {
    }

    /**
     * @return string
     */
    function controller()
    {
    }

    /**
     * @return string
     */
    function action()
    {
    }

    /**
     * 使用 http client 转发请求
     * @param url
     * @param useHeaders 是否使用请求头
     * @param useCookies 是否使用cookie
     * @return 异步响应
     */
    function transfer($url, $useHeaders = false, $useCookies = false)

    /**
     * Get request of current execution
     * @return HttpRequest
     */
    public static function current()
    {
    }

    /**
     * Set request of current execution by controller
     */
    public static function setCurrentByController($controller)
    {
    }

    /**
     * Guard controller's action method invocation
     * @return
     */
    public static function guardInvoke($obj, $methodName, ..$args)
    {
    }
}