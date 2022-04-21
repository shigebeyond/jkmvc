package net.jkcode.jkmvc.http.jphp;


import net.jkcode.jkmvc.http.HttpRequest;
import php.runtime.annotation.Reflection.Name;
import php.runtime.annotation.Reflection.Namespace;
import php.runtime.annotation.Reflection.Signature;
import php.runtime.env.Environment;
import php.runtime.lang.BaseObject;
import php.runtime.reflection.ClassEntity;

@Name("HttpServerRequest")
@Namespace(JkmvcHttpExtension.NS)
public class PHttpRequest extends BaseObject {
    private HttpRequest request;

    public PHttpRequest(Environment env, HttpRequest request) {
        super(env);
        this.request = request;
    }

    public PHttpRequest(Environment env, ClassEntity clazz) {
        super(env, clazz);
    }

    @Signature
    protected void __construct() {
    }

    @Signature
    public String header(String name) {
        return request.getHeader(name);
    }

    @Signature
    public String param(String name) {
        return request.getParameter(name);
    }

    @Signature
    public String query() {
        return request.getQueryString();
    }

    @Signature
    public String path() {
        return request.getPathInfo();
    }

    @Signature
    public String method() {
        return request.getMethod();
    }

    @Signature
    public String sessionId() {
        return request.getSession(true).getId();
    }
}
