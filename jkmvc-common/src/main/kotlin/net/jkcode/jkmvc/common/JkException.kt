package net.jkcode.jkmvc.common

/**
 * 异常基类
 *   只是加了一个id, 以便前后端识别, 根据前端输出的异常id去匹配后端输出的日志记录
 */
abstract class JkException : RuntimeException {

    /**
     * 唯一标识
     */
    public val id: String by lazy{
        generateUUID()
    }

    /**
     * 消息要带上id
     */
    public override val message: String?
        get(){
            val message = super.message ?: ""
            return "$message - $id"
        }

    public constructor(message: String) : super(message) {
    }

    public constructor(cause: Throwable) : super(cause) {
    }

    public constructor(message: String, cause: Throwable) : super(message, cause) {
    }
}