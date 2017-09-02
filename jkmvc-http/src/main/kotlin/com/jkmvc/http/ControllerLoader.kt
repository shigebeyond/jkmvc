package com.jkmvc.http

import com.jkmvc.common.lcFirst
import com.jkmvc.common.travel
import com.jkmvc.common.trim
import java.io.File
import java.util.*

/**
 * 获得某个包下的类
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
object ControllerLoader:IControllerLoader{

    /**
     * 自动扫描的包
     */
    private val packages:MutableList<String> = LinkedList<String>();

    /**
     * controller类缓存
     *   key为相对路径
     *   value为类
     */
    private val controllers:MutableMap<String, ControllerClass> by lazy {
        scan()
    }

    /**
     * 添加单个包
     * @param pck 包名
     * @return
     */
    public override fun addPackage(pck:String): IControllerLoader {
        packages.add(pck)
        return this;
    }

    /**
     * 添加多个包
     */
    public override fun addPackages(pcks:Collection<String>): IControllerLoader {
        packages.addAll(pcks)
        return this;
    }

    /**
     * 扫描指定包下的Controller类
     * @return
     */
    public override fun scan(): MutableMap<String, ControllerClass> {
        val result:MutableMap<String, ControllerClass> = HashMap<String, ControllerClass>()

        // 获得类加载器
        val cld = Thread.currentThread().contextClassLoader

        // 遍历包来扫描
        for (pck in packages){
            // 获得该包的所有资源
            val path = pck.replace('.', '/')
            val urls = cld.getResources(path)
            // 遍历资源
            for(url in urls){
                // 遍历某个资源下的文件
                url.travel { relativePath, isDir ->
                    // 收集controller类
                    collectControllerClass(relativePath, isDir, result)
                }
            }
        }

        return result
    }

    /**
     * 收集controller类
     *
     * @param relativePath
     * @param isDir
     * @return
     */
    fun collectControllerClass(relativePath: String, isDir: Boolean, result: MutableMap<String, ControllerClass>){
        // 过滤Controller的类文件
        if(!relativePath.endsWith("Controller.class"))
            return

        // 获得类名
        val className = relativePath.substringBefore(".class").replace(File.separatorChar, '.')
        // 获得类
        val clazz = Class.forName(className)
        // 过滤Controller子类
        val ctrl = Controller::class.java
        if(ctrl != clazz && ctrl.isAssignableFrom(clazz)){
            // 收集controller的构造函数+所有action方法
            result.put(getControllerName(className), ControllerClass(clazz.kotlin))
        }
    }

    /**
     * 根据类名获得controller名
     */
    fun getControllerName(clazz:String): String {
        val start = clazz.lastIndexOf('.') + 1
        val end = clazz.length - 10
        return clazz.substring(start, end).lcFirst() /* 首字母小写 */
    }

    /**
     * 获得controller类
     * @param controller名
     * @return
     */
    public override fun getControllerClass(name: String): ControllerClass? {
        return controllers.get(name);
    }

}
