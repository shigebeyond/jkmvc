package net.jkcode.jkmvc.common

import java.util.concurrent.CompletableFuture

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
 * 转为future工厂
 */
public fun <RequestArgumentType, ResponseType> toFutureSupplier(supplier: (RequestArgumentType) -> ResponseType):(RequestArgumentType) -> CompletableFuture<ResponseType> {
    return { arg ->
        CompletableFuture.supplyAsync({
            supplier.invoke(arg)
        })
    }
}

/**
 * 对supplier包装try/finally, 兼容结果值是 CompletableFuture 的情况
 *
 * @param supplier 取值函数
 * @param complete 完成后的回调函数, 接收2个参数: 1 结果值 2 异常
 */
public inline fun trySupplierFinally(supplier: () -> Any?, noinline complete: (Any?, Throwable?) -> Unit){
    var result:Any? = null
    var rh: Throwable? = null
    try{
        // 调用取值函数
        result = supplier.invoke()
        // 异步结果
        if(result is CompletableFuture<*>)
            result.whenComplete(complete) // 完成后回调
    }catch (r: Throwable){
        rh = r
    }finally {
        // 同步结果
        if(result !is CompletableFuture<*>)
            complete.invoke(result, rh) // 完成后回调
    }
}

/**
 * 对supplier包装try/catch, 兼容结果值是 CompletableFuture 的情况
 *
 * @param supplier 取值函数
 * @param catch 异常回调函数
 * @return
 */
public inline fun trySupplierCatch(supplier: () -> Any?, noinline catch: (Throwable?) -> Any?): Any?{
    
    var result:Any?
    var rh: Throwable? = null
    try{
        // 调用取值函数
        result = supplier.invoke()
        // 异步结果, 返回新的 CompletableFuture
        if(result is CompletableFuture<*>)
            return (result as CompletableFuture<Any?>).exceptionally(catch) // 异常回调

        return result
    }catch (r: Throwable){
        return catch.invoke(r) // 异常回调
    }
}