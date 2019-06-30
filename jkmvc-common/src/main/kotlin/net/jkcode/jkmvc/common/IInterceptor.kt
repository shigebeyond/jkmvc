package net.jkcode.jkmvc.common

import net.jkcode.jkmvc.singleton.BeanSingletons

/**
 * 拦截器
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-01 11:39 AM
 */
interface IInterceptor<T> {

    companion object {

        /**
         * 从配置文件中加载对应的拦截器实例
         * @param config
         * @param prop
         * @return
         */
        public fun <T> load(config: IConfig, prop: String = "interceptors"): List<IInterceptor<T>>{
            val classes: List<String>? = config[prop]
            if(classes.isNullOrEmpty())
                return emptyList()


            return classes!!.map { clazz ->
                BeanSingletons.instance(clazz) as IInterceptor<T>
            }
        }

        /**
         * 对supplier包装try/finally, 并加入拦截器处理
         *
         * @param supplier 取值函数
         * @param complete 完成后的回调函数, 接收2个参数: 1 结果值 2 异常, 返回新结果
         */
        public inline fun <T, R> trySupplierFinallyAroundInterceptor(interceptors: List<IInterceptor<T>>, req:T, supplier: () -> R, crossinline complete: (Any?, Throwable?) -> Any?): R{
            return trySupplierFinally(
                    { // 1 supplier
                        //调用拦截器前置处理
                        for (i in interceptors)
                            if (!i.before(req))
                                throw InterceptException("Interceptor [${i.javaClass.name}] handle request fail");

                        // 取值
                        supplier.invoke()
                    },
                    { r, e -> // 2 complete
                        //调用拦截器后置处理
                        for (i in interceptors)
                            i.after(req, r as R, e)

                        // 完成后的回调
                        complete.invoke(r, e)
                    }
            );
        }

    }

    /**
     * 前置处理
     * @param req
     * @return 是否通过
     */
    fun before(req: T): Boolean

    /**
     * 后置处理
     * @param req 可能会需要通过req来传递before()中操作过的对象, 如
     * @param result
     * @param ex
     */
    fun after(req: T, result: Any?, ex: Throwable?)

}