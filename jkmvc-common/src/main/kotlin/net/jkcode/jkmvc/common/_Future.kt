package net.jkcode.jkmvc.common

import java.util.concurrent.CompletableFuture

/**
 * 空的future
 */
public val VoidFuture: CompletableFuture<Void> = CompletableFuture.completedFuture(null)
public val UnitFuture: CompletableFuture<Unit> = CompletableFuture.completedFuture(null)

/**
 * 等待并输出异步结果
 */
public fun List<CompletableFuture<*>>.print() {
    this.toTypedArray().print()
}

/**
 * 等待并输出异步结果
 */
public fun Array<CompletableFuture<*>>.print() {
    val f: CompletableFuture<Void> = CompletableFuture.allOf(*this)
    f.get() // 等待
    println(this.joinToString(", ", "异步结果: [", "]") {
        it.get()?.toString() ?: ""
    })
}

/**
 * 将(单参数)的方法调用 转为future工厂
 * @param supplier
 * @return
 */
public fun <RequestArgumentType, ResponseType> toFutureSupplier(supplier: (RequestArgumentType) -> ResponseType):(RequestArgumentType) -> CompletableFuture<ResponseType> {
    return { singleArg ->
        CompletableFuture.supplyAsync({
            supplier.invoke(singleArg)
        })
    }
}

/**
 * 对supplier包装try/catch, 并包装异步结果, 兼容结果值是 CompletableFuture 的情况
 *
 * @param supplier 取值函数
 * @return
 */
public inline fun trySupplierFuture(supplier: () -> Any?): CompletableFuture<*>{
    try{
        // 调用取值函数
        val result = supplier.invoke()

        // 异步结果
        if(result is CompletableFuture<*>)
            return result

        // 空结果
        if(result == null || result == Unit)
            return UnitFuture

        // 非空结果
        return CompletableFuture.completedFuture(result)
    }catch (r: Throwable){
        val result2 = CompletableFuture<Any?>()
        result2.completeExceptionally(r)
        return result2
    }
}

/**
 * 对supplier包装try/finally, 兼容结果值是 CompletableFuture 的情况
 *
 * @param supplier 取值函数
 * @param complete 完成后的回调函数, 接收2个参数: 1 结果值 2 异常, 返回新结果
 */
public inline fun <T> trySupplierFinally(supplier: () -> T, crossinline complete: (Any?, Throwable?) -> Any?): T{
    var result:Any? = null
    var rh: Throwable? = null
    try{
        // 调用取值函数
        result = supplier.invoke()
        // 异步结果
        if(result is CompletableFuture<*>) {
            val result2 = CompletableFuture<Any?>()
            // return result.whenComplete(complete) -> //完成后回调
            result.whenComplete{ r, ex -> //完成后回调
                try{
                    // 回调执行依然会抛异常
                    val r2 = complete.invoke(r, ex)
                    // 回调结果作为最终结果
                    result2.complete(r2)
                }catch (ex: Throwable){
                    result2.completeExceptionally(ex)
                }
            }
            result = result2
        }
    }catch (r: Throwable){
        rh = r
        r.printStackTrace()
    }finally {
        // 同步结果
        if(result !is CompletableFuture<*>)
            result = complete.invoke(result, rh) // 完成后回调, 同时回调结果作为最终结果

        return result as T
    }
}

/**
 * 对supplier包装try/catch, 兼容结果值是 CompletableFuture 的情况
 *
 * @param supplier 取值函数
 * @param catch 异常回调函数
 * @return
 */
public inline fun <T> trySupplierCatch(supplier: () -> T, noinline catch: (Throwable) -> Any?): T{
    
    var result:Any?
    var rh: Throwable? = null
    try{
        // 调用取值函数
        result = supplier.invoke()
        // 异步结果, 返回新的 CompletableFuture
        if(result is CompletableFuture<*>)
            return (result as CompletableFuture<Any>).exceptionally(catch) as T // 异常回调

        return result
    }catch (r: Throwable){
        return catch.invoke(r) as T // 异常回调
    }
}