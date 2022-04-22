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
    function path()
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
}