package com.jkmvc.http

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * http请求处理者
 *
 * @author shijianhang
 * @date 2016-10-6 上午9:27:56
 *
 */
interface IRequestHandler {

    /**
     * 处理请求
     *
     * @param HttpServletRequest req
     * @param HttpServletResponse res
     * @return 是否处理，如果没有处理（如静态文件请求），则交给下一个filter/默认servlet来处理
     */
    public fun handle(request: HttpServletRequest, response: HttpServletResponse): Boolean

}
