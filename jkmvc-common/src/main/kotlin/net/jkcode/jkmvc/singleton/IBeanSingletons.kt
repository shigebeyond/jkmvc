package net.jkcode.jkmvc.singleton

/**
 * 全局的bean实例池
 *
 * @author shijianhang
 * @create 2019-1-24 下午3:17
 **/
interface IBeanSingletons {

    /**
     * 根据类来获得单例
     *
     * @param clazz 类
     * @return
     */
    fun instance(clazz: Class<*>): Any

    /**
     * 根据类名来获得单例
     *
     * @param clazz 类名
     * @return
     */
    fun instance(clazz: String): Any{
        return instance(Class.forName(clazz))
    }
}