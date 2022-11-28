package net.jkcode.jkmvc.http.jphp

import net.jkcode.jkmvc.http.HttpResponse
import net.jkcode.jphp.ext.toJavaObject
import org.asynchttpclient.Response
import php.runtime.Memory
import php.runtime.annotation.Reflection
import php.runtime.env.Environment
import php.runtime.lang.BaseObject
import php.runtime.memory.ArrayMemory
import php.runtime.memory.ObjectMemory
import java.io.InputStream
import java.nio.charset.Charset

@Reflection.Name("HttpResponse")
@Reflection.Namespace(JkmvcHttpExtension.NS)
class PHttpResponse(env: Environment, public val res: HttpResponse) : BaseObject(env) {

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
        res.outputStream.write(value.getBinaryBytes(Charset.forName(charset)))
        return this
    }

    @Reflection.Signature
    fun status(status: Int): PHttpResponse {
        status(status, null)
        return this
    }

    @Reflection.Signature
    fun status(status: Int, @Reflection.Nullable message: String?): PHttpResponse {
        res.status = status
        if (message != null && !message.isEmpty()) {
            res.sendError(status, message)
        }
        return this
    }

    @Reflection.Signature
    fun header(name: String, value: Memory): PHttpResponse {
        res.addHeader(name, value.toString())
        return this
    }

    @Reflection.Signature
    fun contentType(value: String?): PHttpResponse {
        res.contentType = value
        return this
    }

    @Reflection.Signature
    fun contentLength(value: Long): PHttpResponse {
        res.setContentLengthLong(value)
        return this
    }

    @Reflection.Signature
    fun redirect(value: String): PHttpResponse {
        res.sendRedirect(value)
        return this
    }

    @Reflection.Signature
    fun flush(): PHttpResponse {
        res.flushBuffer()
        return this
    }

    @Reflection.Signature
    public fun renderHtml(content:String){
        res.renderHtml(content)
    }

    @Reflection.Signature
    public fun renderText(content:String){
        res.renderText(content)
    }

    @Reflection.Signature
    public fun renderXml(content:String){
        res.renderXml(content)
    }

    @Reflection.Signature
    public fun renderFile(file: String){
        res.renderFile(file)
    }

    @Reflection.Signature
    @JvmOverloads
    public fun renderJson(code:Int, message:String = "success", data: Any? = null){
        var data = data
        if(data is ArrayMemory)
            data = data?.toJavaObject()
        res.renderJson(code, message, data)
    }

    companion object{

        @Reflection.Signature
        @JvmStatic
        fun current(env: Environment): Memory {
            return ObjectMemory(PHttpResponse(env, HttpResponse.current()))
        }
    }
}