package com.jkmvc.oauth2.authorizer

import com.alibaba.fastjson.JSONObject
import com.jkmvc.oauth2.IOauth2Authorizer
import com.jkmvc.oauth2.Oauth2User

/**
 * github授权处理
 *
 * @author shijianhang
 * @create 2017-10-04 下午3:29
 */
class GithubAuthorizer : IOauth2Authorizer() {

    /**
     * 授权类型
     */
    protected override val type: String = "github"

    /**
     * 有参数的授权url
     *   有3个有序的参数：1 clientId, 2 redirectUri, 3 state
     */
    protected override val authUrl:String = "https://github.com/login/oauth/authorize?scope=user&client_id=%s&redirect_uri=%s&state=%s"

    /**
     * 有参数的令牌url
     *   有3个有序的参数： 1 clientId，2 clientSecret, 3 redirectUri, 4 code
     */
    protected override val tokenUrl: String = "https://github.com/login/oauth/access_token?client_id=%s&client_secret=%s&redirect_uri=redirectUri&code=%s"

    /**
     * 有参数的用户url
     *   有1个参数：token
     */
    protected override val userUrl: String = "https://api.github.com/user?access_token=%s"

    /**
     * 从响应json中获得授权用户信息
     *
     * @param json
     * @return
     */
    protected override fun buildOauth2User(json: JSONObject): Oauth2User {
        return Oauth2User(type, json.getString("avatar_url"), json.getString("id"), json.getString("login"))
    }
}
