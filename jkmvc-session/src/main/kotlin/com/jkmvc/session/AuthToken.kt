package com.jkmvc.session

import com.jkmvc.http.Request

/**
 * 认证用户 -- 使用token来保存登录用户的状态
 *   1 登录
 *   2 注销
 *
 * @author shijianhang
 * @create 2017-10-04 下午3:40
 **/
class AuthToken: Auth() {

    /**
     * token管理器
     */
    protected val tokenManager: ITokenManager = RedisTokenManager

    /**
     * 获得当前会话的token
     */
    public fun getToken(): String? {
        val req = Request.current()
        // 先找请求参数
        val token = req.getParameter("token")
        if(!token.isNullOrEmpty())
            return token

        // 后找请求头
        return req.getHeader("token")
    }

    /**
     * 获得当前登录用户
     * @return
     */
    public override fun getUser(): IAuthUserModel?{
        // 获得当前token
        val token = getToken()
        if(token == null)
            return null

        // 根据token获得用户
        val result = tokenManager.getUser(token)
        return result?.component1()
    }

    /**
     * 登录后的处理
     * @param user
     */
    protected override fun afterLogin(user: IAuthUserModel) {
        //生成登录token
        user["token"] = RedisTokenManager.createToken(user)
    }

    /**
     * 注销登录
     */
    public override fun logout(){
        // 获得当前token
        val token = getToken()
        if(token == null)
            return

        // 删除token
        tokenManager.deleteToken(token)
    }

}