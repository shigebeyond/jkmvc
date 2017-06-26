package com.jkmvc.http

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 服务端对象，用于处理请求
 *
 * @author shijianhang
 * @date 2016-10-6 上午9:27:56
 *
 */
interface IServer {

    /**
     * 处理请求
     *
     * @param HttpServletRequest req
     * @param HttpServletResponse res
     */
    public fun run(request: HttpServletRequest, response: HttpServletResponse): Boolean

}
