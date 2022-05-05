package net.jkcode.jkmvc.http.jphp

import net.jkcode.jkmvc.http.HttpRequest
import net.jkcode.jkmvc.http.HttpResponse
import php.runtime.Memory
import php.runtime.annotation.Reflection
import php.runtime.env.Environment
import php.runtime.lang.BaseObject
import php.runtime.memory.ObjectMemory
import php.runtime.reflection.ClassEntity
import java.io.IOException
import java.nio.charset.Charset

@Reflection.Name("HttpResponse")
@Reflection.Namespace(JkmvcHttpExtension.NS)
class PHttpResponse(env: Environment, public val response: HttpResponse) : BaseObject(env) {

    @Reflection.Signature
    protected fun __construct() {
    }

    /**
     * 2个write()方法，不能使用kotlin带参数值的方式来合并，因为php转java调用时, 会根据有无第二个参数, 去选择2个java方法中的一个
     */
    @Reflection.Signature
    fun write(value: Memory): PHttpResponse {
        write(value, "UTF-8")
        return this
    }

    @Reflection.Signature
    fun write(value: Memory, charset: String): PHttpResponse {
        response.outputStream.write(value.getBinaryBytes(Charset.forName(charset)))
        return this
    }

    @Reflection.Signature
    fun status(status: Int): PHttpResponse {
        status(status, null)
        return this
    }

    @Reflection.Signature
    fun status(status: Int, @Reflection.Nullable message: String?): PHttpResponse {
        response.status = status
        if (message != null && !message.isEmpty()) {
            response.sendError(status, message)
        }
        return this
    }

    @Reflection.Signature
    fun header(name: String, value: Memory): PHttpResponse {
        response.addHeader(name, value.toString())
        return this
    }

    @Reflection.Signature
    fun contentType(value: String?): PHttpResponse {
        response.contentType = value
        return this
    }

    @Reflection.Signature
    fun contentLength(value: Long): PHttpResponse {
        response.setContentLengthLong(value)
        return this
    }

    @Reflection.Signature
    fun redirect(value: String): PHttpResponse {
        response.sendRedirect(value)
        return this
    }

    @Reflection.Signature
    fun flush(): PHttpResponse {
        response.flushBuffer()
        return this
    }

    @Reflection.Signature
    fun current(env: Environment): Memory {
        return ObjectMemory(PHttpResponse(env, HttpResponse.current()))
    }
}