package net.jkcode.jkmvc.http.controller

import net.jkcode.jkmvc.common.*
import net.jkcode.jkmvc.http.httpLogger
import java.lang.reflect.Modifier

/**
 * 加载Controller类
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
object ControllerClassLoader : IControllerClassLoader, ClassScanner() {

    /**
     * http配置
     */
    public val config = Config.instance("http", "yaml")

    /**
     * controller类缓存
     *   key为相对路径
     *   value为类
     */
    private val controllerClasses:MutableMap<String, ControllerClass> = HashMap()

    init{
        // 加载配置的包路径
        val pcks:List<String>? = config["controllerPackages"]
        if(pcks != null)
            addPackages(pcks)
    }

    /**
     * 收集controller类
     *
     * @param relativePath 类文件相对路径
     */
    public override fun collectClass(relativePath: String):Unit {
        // 过滤Controller的类文件
        if(!relativePath.endsWith("Controller.class"))
            return

        // 获得类
        val clazz = relativePath.classPath2class()
        val modifiers = clazz.modifiers
        // 过滤Controller子类
        if(Controller::class.java.isSuperClass(clazz) /* 继承Controller */ && !Modifier.isAbstract(modifiers) /* 非抽象类 */ && !Modifier.isInterface(modifiers) /* 非接口 */){
            // 收集controller的构造函数+所有action方法
            httpLogger.debug("收集controller: {}", clazz.name)
            controllerClasses[getControllerName(clazz.name)] = ControllerClass(clazz.kotlin)
        }
    }

    /**
     * 根据类名获得controller名
     *
     * @param
     * @return
     */
    private fun getControllerName(clazz:String): String {
        val start = clazz.lastIndexOf('.') + 1
        val end = clazz.length - 10
        return clazz.substring(start, end).lcFirst() /* 首字母小写 */
    }

    /**
     * 根据名字来获得controller类
     *
     * @param controller名
     * @return
     */
    public override fun getControllerClass(name: String): ControllerClass? {
        return controllerClasses.get(name);
    }

    /**
     * 获得所有的controller类
     *
     * @return
     */
    public override fun getControllerClasses(): Collection<ControllerClass> {
        return controllerClasses.values
    }

}
