package net.jkcode.jkmvc.http

import net.jkcode.jkmvc.common.IInterceptor

// http请求处理的拦截器
typealias IHttpRequestInterceptor = IInterceptor<HttpRequest>