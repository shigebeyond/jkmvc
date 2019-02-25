package net.jkcode.jkmvc.oauth2.authorizer.impl

import com.alibaba.fastjson.JSONObject
import net.jkcode.jkmvc.common.Http
import net.jkcode.jkmvc.oauth2.authorizer.IOauth2Authorizer
import net.jkcode.jkmvc.oauth2.authorizer.Oauth2User

/**
 * 微信授权处理
 * http://wiki.connect.qq.com/%E4%BD%BF%E7%94%A8authorization_code
 *
 * @author shijianhang
 * @create 2017-10-04 下午3:29
 */
class QQAuthorizer : IOauth2Authorizer() {

    /**
     * 授权类型
     */
    protected override val type: String = "qq"

    /**
     * 有参数的授权url
     *   有3个有序的参数：1 clientId, 2 redirectUri, 3 state
     */
    protected override val authUrl:String = "https://graph.qq.com/oauth2.0/authorize?response_type=code&client_id=%s&redirect_uri=%s&state=%s"

    /**
     * 有参数的令牌url
     *   有3个有序的参数： 1 clientId，2 clientSecret, 3 redirectUri, 4 code
     */
    protected override val tokenUrl: String = "https://graph.qq.com/oauth2.0/token?grant_type=authorization_code&client_id=%s&client_secret=%s&redirect_uri=%s&code=%s"

    /**
     * 有参数openid的url
     *   有1个参数：token
     */
    protected val openidUrl: String = "https://graph.qq.com/oauth2.0/me?access_token=%s"

    /**
     * 有参数的用户url
     *   有3个参数：1 token， 2 clientId, 3 openid
     */
    protected override val userUrl: String = "https://graph.qq.com/user/get_user_info?access_token=%s&oauth_consumer_key=%s&openid=%s&format=format"

    /**
     * 获得openid
     *
     * @param accessToken
     * @return
     */
    protected fun getOpenId(accessToken: JSONObject): String {
        var url  = openidUrl.format(accessToken.getString("access_token"))
        val response = Http.get(url)
        val json = JSONObject.parseObject(response)
        return json.getString("openid")
    }

    /**
     * 获得用户
     *
     * @param accessToken
     * @return
     */
    protected override fun getOauth2User(accessToken: JSONObject): Oauth2User {
        // 获得openid
        val openid: String = getOpenId(accessToken)

        // 获得用户
        val url = userUrl.format(accessToken.getString("access_token"), config.getString("clientId"), openid)
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
        return Oauth2User(type, json.getString("figureurl_2"), json.getString("openid"), json.getString("nickname"), json.getString("gender"))
    }

}
