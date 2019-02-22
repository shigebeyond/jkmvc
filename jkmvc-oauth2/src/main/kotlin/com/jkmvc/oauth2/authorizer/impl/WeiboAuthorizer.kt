package com.jkmvc.oauth2.authorizer.impl

import com.alibaba.fastjson.JSONObject
import com.jkmvc.common.Http
import com.jkmvc.oauth2.authorizer.IOauth2Authorizer
import com.jkmvc.oauth2.authorizer.Oauth2User
import java.util.*

/**
 * 微博授权处理
 *
 * @author shijianhang
 * @create 2017-10-04 下午3:29
 */
class WeiboAuthorizer : IOauth2Authorizer() {

    /**
     * 授权类型
     */
    protected override val type: String = "weibo"

    /**
     * 有参数的授权url
     *   有3个有序的参数：1 clientId, 2 redirectUri, 3 state
     */
    protected override val authUrl:String = "https://api.weibo.com/oauth2/authorize?scope=email&client_id=%s&redirect_uri=%s&state=%s"

    /**
     * 令牌url，参数通过post表单提交，而不是url提交
     */
    protected override val tokenUrl: String = "https://api.weibo.com/oauth2/access_token"

    /**
     * 有参数的用户url
     *   有2个参数：1 token, 2 uid
     */
    protected override val userUrl: String = "https://api.weibo.com/2/users/show.json?access_token=%s&uid=%s"

    /**
     * 根据code获得token
     *    微博要post请求
     *
     * @param code
     * @return
     */
    protected override fun getAccessToken(code: String): JSONObject {
        val url = tokenUrl.format(config.getString("clientId"), config.getString("clientSecret"), config.getString("redirectUri"), code)

        val params = HashMap<String, String>()
        params.put("grant_type", "authorization_code")
        params.put("client_id", config["clientId"]!!)
        params.put("client_secret", config["lientSecret"]!!)
        params.put("redirect_uri", config["redirectUri"]!!)
        params.put("code", code)

        val response = Http.post(url, params)
        return JSONObject.parseObject(response)
    }

    /**
     * 获得用户
     *
     * @param accessToken
     * @return
     */
    protected override fun getOauth2User(accessToken: JSONObject): Oauth2User {
        val url = userUrl.format(accessToken.getString("access_token"), accessToken.getString("uid"))
        val response = Http.get(url)
        val json = JSONObject.parseObject(response)
        return buildOauth2User(json)
    }

    /**
     * 从响应json中获得授权用户信息
     *
     * @param json
     * @return
     */
    protected override fun buildOauth2User(json: JSONObject): Oauth2User {
        return Oauth2User(type, json.getString("avatar_large"), json.getString("id"), json.getString("screen_name"), json.getString("gender") == "f")
    }
}
