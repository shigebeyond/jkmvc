package com.jkmvc.oauth2

import com.alibaba.fastjson.JSONObject
import com.jkmvc.http.Controller
import java.util.*

/**
 * oauth2授权的控制器
 *
 * @author shijianhang
 * @create 2017-10-04 下午3:29
 */
public abstract class Oauth2Controller : Controller() {

    /**
     * 授权
     */
    public fun grantAction() {
        val type: String = req["type"]!!
        val oauth2: IOauth2Authorizer = IOauth2Authorizer.instance(type)

        // 构建状态
        val state = UUID.randomUUID().toString().replace("-", "")

        // 保存状态
        req.session.setAttribute("oauth2_state", state)

        // 获得授权url并跳转
        val url = oauth2.getAuthorizeUrl(state)
        redirect(url)
    }

    /**
     * 根据授权码来获得用户
     */
    protected fun getOauth2UserByCode(): Oauth2User {
        // 1 检查state
        val state: String = req["state"]!!
        val sessionState: String = req.session.getAttribute("oauth2_state") as String

        if (sessionState != state) {
            throw IllegalArgumentException("state not validated")
        }

        val type: String = req["type"]!!
        val oauth2: IOauth2Authorizer = IOauth2Authorizer.instance(type)

        // 根据code来获得用户
        val code: String = req["code"]!!
        return oauth2.getOauth2User(code)
    }

    /**
     * 授权回调的处理 -- 参考代码
     */
    /*public fun callbackAction() {
        try {
            // 获得授权用户
            val user = getOauth2UserByCode()
            res.renderString(JSONObject.toJSONString(user))
        }catch (e: Exception){
            res.renderString("错误： ${e.message}")
        }
    }*/

}
