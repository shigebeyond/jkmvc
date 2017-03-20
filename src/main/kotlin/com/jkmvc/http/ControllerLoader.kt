package com.jkmvc.http


import com.jkmvc.common.Loader
import java.io.File

/**
 * 获得某个包下的类
 * @author shi
 */
object controllerLoader: Loader() {

    /**
     * 过滤文件
     */
    public override fun filteFile(dir: File, name: String): Boolean{
        // 过滤Controller的类文件
        return name.endsWith("Controller.class");
    }

    /**
     * 过滤类
     */
    public override fun filteClass(clazz:Class<*>): Boolean{
        // 过滤Controller子类
        val ctrl = Controller::class.java
        return ctrl.isAssignableFrom(clazz) && ctrl != clazz)
    }

}
