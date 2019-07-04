package net.jkcode.jkmvc.common

/**
 * 请求拦截器
 *    1. 泛型 P 是请求参数类型, BR 是before()方法的返回值类型
 *    2. 虽然我很想像 ProtocolFilterWrapper#buildInvokerChain() 一样封装filter的调用链, 其内部是一层层调用各个filter 的 Filter#invoke(Invoker<?> nextInvoker, Invocation invocation) 方法来实现的
 *    但是我考虑到调用方的api无法统一, 最多勉强统一rpc client与rpc server的, 但是无法统一http server的场景, 再加上异步调用的场景, 会一层层的在每个filter的实现中调用 trySupplierFinally() 来处理, 难看又复杂
 *    因此直接将filter拆成before()/after()两步处理
 *    3 before()/after()的状态传递, 拆分后2个方法内状态不互通, 因此直接将before()的调用结果用作after()的调用参数
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-01 11:39 AM
 */
interface IRequestInterceptor<P> {

    companion object {

        /**
         * 对supplier包装try/finally, 并加入拦截器处理
         *
         * @param supplier 取值函数
         * @param complete 完成后的回调函数, 接收2个参数: 1 结果值 2 异常, 返回新结果
         */
        public inline fun <T, R> trySupplierFinallyAroundInterceptor(interceptors: List<IRequestInterceptor<T>>, req:T, supplier: () -> R, crossinline complete: (Any?, Throwable?) -> Any?): R{
            // 缓存前置处理结果
            val beforeResults = arrayOfNulls<Any?>(interceptors.size)
            return trySupplierFinally(
                    { // 1 supplier
                        //调用拦截器前置处理
                        for (i in 0 until interceptors.size)
                            beforeResults[i] = interceptors[i].before(req)

                        // 取值
                        supplier.invoke()
                    },
                    { r, e -> // 2 complete
                        //调用拦截器后置处理
                        for (i in 0 until interceptors.size)
                            interceptors[i].after(req, beforeResults[i], r, e)

                        // 清空前置处理结果
                        beforeResults.clear()

                        // 完成后的回调
                        complete.invoke(r, e)
                    }
            );
        }

    }

    /**
     * 前置处理
     * @param req
     * @return 调用结果作为after()调用的第二参数
     */
    fun before(req: P): Any?

    /**
     * 后置处理
     * @param req 可能会需要通过req来传递before()中操作过的对象, 如
     * @param beforeResult before()方法的调用结果
     * @param result 目标方法的调用结果
     * @param ex 目标方法的调用异常
     */
    fun after(req: P, beforeResult: Any?, result: Any?, ex: Throwable?)

}