package com.jkmvc.session

import com.jkmvc.common.RequestHandledHook
import com.jkmvc.http.Request
import java.io.Closeable

/**
 * 基于token认证用户 -- 使用token来保存登录用户的状态
 *   1 登录
 *   2 注销
 *
 * @author shijianhang
 * @create 2017-10-04 下午3:40
 **/
class TokenAuth : Auth(), Closeable {

    /**
     * 登录用户缓存
     */
    protected val users:ThreadLocal<IAuthUserModel?> = ThreadLocal.withInitial {
        // 获得当前token
        val token = getToken()
        if(token == null)
            null
        else // 根据token获得用户
            tokenManager.getUser(token)?.component1()
    }

    /**
     * token管理器
     */
    protected val tokenManager: ITokenManager = RedisTokenManager

    init{
        RequestHandledHook.addClosing(this)
    }

    /**
     * 获得当前会话的token
     */
    public fun getToken(): String? {
        // web请求？
        val req = Request.currentOrNull()
        if(req == null)
            return null

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
        return users.get()
    }

    /**
     * 登录后的处理
     * @param user
     */
    protected override fun afterLogin(user: IAuthUserModel) {
        //生成登录token
        val token = RedisTokenManager.createToken(user)
        Request.currentOrNull()?.setAttribute("token", token);
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

    /**
     * 清理当前线程的登录用户
     */
    public override fun close() {
        users.remove()
    }


}