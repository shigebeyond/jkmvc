package com.jkmvc.http

import com.jkmvc.common.travel
import com.jkmvc.common.trim
import java.io.File
import java.util.*

/**
 * 获得某个包下的类
 *
 * <code>
 *     val classes = ControllerLoader().listClasses("com.test");
 * </code>
 * @author shi
 */
object ControllerLoader{

    /**
     * 自动扫描的包
     */
    val packages:MutableList<String> = LinkedList<String>();

    /**
     * controller类缓存
     *   key为相对路径
     *   value为类
     */
    val controllers:MutableMap<String, Class<*>> by lazy {
        scan()
    };

    /**
     * 添加单个包
     */
    public fun addPackage(pck:String){
        packages.add(pck)
    }

    /**
     * 添加多个包
     */
    public fun addPackage(pcks:Collection<String>){
        packages.addAll(pcks)
    }

    /**
     * 扫描指定包下的Controller类
     * @return
     */
    fun scan(): MutableMap<String, Class<*>> {

        // 获得类加载器
        val cld = Thread.currentThread().contextClassLoader ?: throw ClassNotFoundException("Can't get class loader.")
        val controllers:MutableMap<String, Class<*>> = HashMap<String, Class<*>>()

        // 遍历包来扫描
        for (pck in packages){
            // 获得该路径的资源
            val path = pck.replace('.', '/')
            val resource = cld.getResource(path) ?: throw ClassNotFoundException("No resource for " + path)

            // 获得指定包的目录
            val directory = File(resource.file)

            // 获得包下的类
            directory.travel { file:File ->
                // 过滤Controller的类文件
                if(file.name.endsWith("Controller.class")){
                    // 去掉 .class 后缀
                    var name = file.absolutePath.trim(directory.absolutePath, "Controller.class")
                    // 获得类
                    val clazz = Class.forName(pck + '.' + name.replace('.', '/'))
                    // 过滤Controller子类
                    val ctrl = Controller::class.java
                    if(ctrl != clazz && ctrl.isAssignableFrom(clazz)){
                        controllers.put(name, clazz)
                    }
                }
            }
        }

        return controllers;
    }

    /**
     * 根据相对路径来获得controller类
     */
    public fun getControllerClass(name: String): Class<*>? {
        return controllers.get(name);
    }

}
