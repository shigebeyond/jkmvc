package com.jkmvc.orm

/**
 * orm操作异常
 *
 * @author shijianhang
 * @date 2016-10-21 下午5:16:59  
 *
 */
class OrmException : RuntimeException {
    constructor(message: String) : super(message) {
    }

    constructor(cause: Throwable) : super(cause) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }
}