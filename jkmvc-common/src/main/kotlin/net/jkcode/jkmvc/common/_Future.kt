package net.jkcode.jkmvc.common

import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer
import java.util.function.Supplier

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
 * 对supplier包装try/catch/finally
 *
 * @param supplier 取值函数
 * @param complete 完成后的回调函数, 接收2个参数: 1 结果值 2 异常
 */
public inline fun <T> trySupplier(supplier: () -> T, noinline complete: (T?, Throwable?) -> Unit){
    var result:T? = null
    var rh: Throwable? = null
    try{
        // 调用取值函数
        result = supplier.invoke()
        // 异步结果
        if(result is CompletableFuture<*>)
            (result as CompletableFuture<T>).whenComplete(complete) // 完成后回调
    }catch (r: Throwable){
        rh = r
    }finally {
        // 同步结果
        if(result !is CompletableFuture<*>)
            complete.invoke(result, rh) // 完成后回调
    }
}