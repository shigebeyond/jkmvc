package net.jkcode.jkmvc.http.controller

import net.jkcode.jkutil.common.ClassScanner
import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.classPath2class
import net.jkcode.jkutil.common.isSuperClass
import net.jkcode.jkutil.common.httpLogger
import java.lang.reflect.Modifier
import kotlin.collections.Collection
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.set

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
        // 扫描包: 做了去重
        if(pcks != null)
            addPackages(pcks)
    }

    /**
     * 收集controller类
     *
     * @param relativePath 类文件相对路径
     */
    public override fun collectClass(relativePath: String) {
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
            val controllerClass = ControllerClass(clazz.kotlin)
            controllerClasses[controllerClass.name] = controllerClass
        }
    }

    /**
     * 根据名字来获得controller类
     *
     * @param controller名
     * @return
     */
    public override fun get(name: String): ControllerClass? {
        return controllerClasses.get(name);
    }

    /**
     * 获得所有的controller类
     *
     * @return
     */
    public override fun getAll(): Collection<ControllerClass> {
        return controllerClasses.values
    }

}
