package com.jkmvc.session

import com.jkmvc.http.Request
import javax.servlet.http.HttpSession

/**
 * 基于session的认证用户 -- 使用session来保存登录用户
 *   1 登录
 *   2 注销
 *
 * @author shijianhang
 * @create 2017-10-04 下午3:40
 **/
class SessionAuth : Auth() {

    /**
     * 获得当前会话
     *
     * @param create 是否创建新的会话
     * @return
     */
    private fun getSession(create:Boolean = false): HttpSession {
        return Request.current().getSession(create);
    }

    /**
     * 获得当前登录用户
     * @return
     */
    public override fun getUser(): IAuthUserModel?{
        // 从session中读取登录用户
        return getSession(false).getAttribute("user") as IAuthUserModel?
    }

    /**
     * 登录后的处理
     * @param user
     */
    protected override fun afterLogin(user: IAuthUserModel) {
        // 保存登录用户到session中
        getSession(true).setAttribute("user", user);
    }

    /**
     * 注销登录
     */
    public override fun logout(){
        getSession(false).invalidate();
    }

    public override fun close() {
    }

}