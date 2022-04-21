package net.jkcode.jkmvc.http.jphp;

import net.jkcode.jkmvc.http.HttpResponse;
import php.runtime.Memory;
import php.runtime.annotation.Reflection.Name;
import php.runtime.annotation.Reflection.Namespace;
import php.runtime.annotation.Reflection.Nullable;
import php.runtime.annotation.Reflection.Signature;
import php.runtime.env.Environment;
import php.runtime.lang.BaseObject;
import php.runtime.reflection.ClassEntity;

import java.io.IOException;
import java.nio.charset.Charset;

@Name("HttpServerResponse")
@Namespace(JkmvcHttpExtension.NS)
public class PHttpResponse extends BaseObject {
    private HttpResponse response;

    public PHttpResponse(Environment env, HttpResponse response) {
        super(env);
        this.response = response;
    }

    public PHttpResponse(Environment env, ClassEntity clazz) {
        super(env, clazz);
    }

    public HttpResponse getResponse() {
        return response;
    }

    @Signature
    protected void __construct() {
    }

    @Signature
    public PHttpResponse write(Memory value) throws IOException {
        write(value, "UTF-8");
        return this;
    }

    @Signature
    public PHttpResponse write(Memory value, String charset) throws IOException {
        response.getOutputStream().write(value.getBinaryBytes(Charset.forName(charset)));
        return this;
    }

    @Signature
    public PHttpResponse status(int status) throws IOException {
        status(status, null);
        return this;
    }

    @Signature
    public PHttpResponse status(int status, @Nullable String message) throws IOException {
        response.setStatus(status);

        if (message != null && !message.isEmpty()) {
            response.sendError(status, message);
        }

        return this;
    }

    @Signature
    public PHttpResponse header(String name, Memory value) {
        response.addHeader(name, value.toString());
        return this;
    }

    @Signature
    public PHttpResponse contentType(String value) {
        response.setContentType(value);
        return this;
    }

    @Signature
    public PHttpResponse contentLength(long value) {
        response.setContentLengthLong(value);
        return this;
    }

    @Signature
    public PHttpResponse redirect(String value) throws IOException {
        response.sendRedirect(value);
        return this;
    }

    @Signature
    public PHttpResponse flush() throws IOException {
        response.flushBuffer();
        return this;
    }
}
