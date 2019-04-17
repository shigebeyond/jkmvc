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