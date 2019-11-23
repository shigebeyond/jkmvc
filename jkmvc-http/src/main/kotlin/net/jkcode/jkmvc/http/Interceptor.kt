package net.jkcode.jkmvc.http

import net.jkcode.jkutil.interceptor.IRequestInterceptor

// http请求处理的拦截器
typealias IHttpRequestInterceptor = IRequestInterceptor<HttpRequest>