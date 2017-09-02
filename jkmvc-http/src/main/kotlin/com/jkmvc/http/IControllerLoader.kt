package com.jkmvc.http

/**
 * 获得某个包下的类
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
interface IControllerLoader{

    /**
     * 添加单个包
     * @param pck 包名
     * @return
     */
    fun addPackage(pck:String): IControllerLoader;

    /**
     * 添加多个包
     */
    fun addPackages(pcks:Collection<String>): IControllerLoader;

    /**
     * 扫描指定包下的Controller类
     * @return
     */
    fun scan(): MutableMap<String, ControllerClass>;

    /**
     * 获得controller类
     * @param controller名
     * @return
     */
    fun getControllerClass(name: String): ControllerClass?;

}
