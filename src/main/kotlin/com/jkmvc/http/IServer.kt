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
     * @return 是否处理，如果没有处理，则交给下一个filter/默认servlet来处理，如处理静态文件请求
     */
    public fun run(request: HttpServletRequest, response: HttpServletResponse): Boolean

}
