<?php
namespace php\lang;

/**
 * Class WrapJavaObject
 * @packages std, core
 */
final class WrapJavaObject
{

    /**
     * constructor.
     * @param string $obj
     * @throws IOException
     */
    function __construct($obj) {}

    function __call($method, $args) {}

    // ---------------- 抄final类JavaObject实现 ---------------
    /**
     * Get class of object
     * @return JavaClass
     */
    public function getClass() { }

    /**
     * Get name of class of object
     * @return string
     */
    public function getClassName() { }
}