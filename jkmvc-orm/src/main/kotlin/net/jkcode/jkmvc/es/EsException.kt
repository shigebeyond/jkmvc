package net.jkcode.jkmvc.es

import net.jkcode.jkutil.common.JkException

/**
 * es操作异常
 *
 * @author shijianhang
 * @date 2016-10-21 下午5:16:59  
 *
 */
class EsException : JkException {

    public constructor(cause: Throwable) : super(cause) {
    }

    public constructor(message: String, cause: Throwable? = null) : super(message, cause) {
    }
}