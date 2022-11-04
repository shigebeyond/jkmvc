package net.jkcode.jkmvc.http

import net.jkcode.jkutil.interceptor.IRequestInterceptor

// http请求处理的拦截器, 注: 拦截前已做好路由解析
typealias IHttpRequestInterceptor = IRequestInterceptor<HttpRequest>