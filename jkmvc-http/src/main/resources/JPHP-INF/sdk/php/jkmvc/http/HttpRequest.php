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