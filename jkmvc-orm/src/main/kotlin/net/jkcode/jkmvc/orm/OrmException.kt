package net.jkcode.jkmvc.orm

import net.jkcode.jkutil.common.JkException

/**
 * orm操作异常
 *
 * @author shijianhang
 * @date 2016-10-21 下午5:16:59  
 *
 */
class OrmException : JkException {

    public constructor(cause: Throwable) : super(cause) {
    }

    public constructor(message: String, cause: Throwable? = null) : super(message, cause) {
    }
}