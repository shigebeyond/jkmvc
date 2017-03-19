package com.jkmvc.http

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 控制器
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 *
 */
class Controller(val req: HttpServletRequest /* 请求对象 */, val res: HttpServletResponse /* 响应对象 */) {


    /**
     * 给默认的视图
     *
     * @param array $data
     * @return View
     */
    public fun view(data:Map<String, Any?>)
    {
        return View($this->req->controller()."/".$this->req->action(), $data);
    }
}