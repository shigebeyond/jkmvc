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
     * @param $valueAsArray 值是否作为数组
     * @return string
     */
    function param($name, $valueAsArray = false)
    {
    }

    /**
     * @param $valueAsArray 值是否作为数组
     * @return array
     */
    function params($valueAsArray = false)
    {
    }

    /**
     * @return string
     */
    function query()
    {
    }


    /**
     * 合并多个参数为对象
     *    值是数组的多个参数, 转对象数组
     * @param names 参数名数组, 必须保证所有参数值的数组长度都一致
     */
    function combineParams(array names){
    }

    /**
     * 将参数名以 namePrefix 为前缀的参数合并为对象 -- 一维参数转多维对象
     *    如 fields[0][name]=a&fields[0][type]=int(10) unsigned&fields[0][default]=0&fields[0][comment]=a&fields[0][is_null]=NOT NULL
     *    合并转为对象 [{"name":"a","type":"int(10) unsigned","default":"0","comment":"a","is_null":"NOT NULL"}]
     * @param namePrefix 参数名前缀
     * @return
     */
    function params2Object($namePrefix: string){
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
     * 是否内部请求: INCLUDE/FORWARD
     * @return
     */
    function isInner(){
    }

    /**
     * 是否post请求
     * @return
     */
    function isPost(){
    }

    /**
     * 是否option请求
     * @return
     */
    function isOptions(){
    }

    /**
     * 是否get请求
     * @return
     */
    function isGet(){
    }

    /**
     * 是否 multipart 请求
     * @return
     */
    function isMultipartContent(){
    }

    /**
     * 是否上传文件的请求
     * @return
     */
    function isUpload(){
    }

    /**
     * 是否ajax请求
     * @return
     */
    function isAjax(){
    }

    /**
     * 转发请求，并返回响应
     *    因为是异步处理, 因此在action方法最后一行必须返回该函数的返回值
     * @param $url
     * @param $res
     * @param $useHeaders 是否使用请求头
     * @param $useCookies 是否使用cookie
     * @return 异步响应
     */
    function transferAndReturn($url, $res, $useHeaders = false, $useCookies = false)
    {
    }

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