package net.jkcode.jkmvc.http.router

/**
 * http方法
 * @author shijianhang<772910474@qq.com>
 * @date 2020-1-6 6:04 PM
 */
enum class HttpMethod {

    ALL, GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE;

    /**
     * 匹配方法
     * @param method
     * @return
     */
    public fun match(method: HttpMethod): Boolean {
        return this == HttpMethod.ALL || this == method
    }
}