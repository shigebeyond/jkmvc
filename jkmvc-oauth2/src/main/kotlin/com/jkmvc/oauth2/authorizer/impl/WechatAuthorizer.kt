package com.jkmvc.oauth2.authorizer.impl

import com.alibaba.fastjson.JSONObject
import com.jkmvc.common.Http
import com.jkmvc.oauth2.authorizer.IOauth2Authorizer
import com.jkmvc.oauth2.authorizer.Oauth2User


/**
 * 微信授权处理
 * https://open.weixin.qq.com/cgi-bin/showdocument?action=dir_list&id=open1419316505
 *
 * @author shijianhang
 * @create 2017-10-04 下午3:29
 */
class WechatAuthorizer : IOauth2Authorizer() {

    /**
     * 授权类型
     */
    protected override val type: String = "wechat"

    /**
     * 有参数的授权url
     *   有3个有序的参数：1 clientId, 2 redirectUri, 3 state
     */
    protected override val authUrl:String = "https://open.weixin.qq.com/connect/qrconnect?response_type=code&scope=snsapi_login&appid=%s&redirect_uri=%s&state=%s#wechat_redirect"

    /**
     * 有参数的令牌url
     *   有3个有序的参数： 1 clientId，2 clientSecret, 3 redirectUri, 4 code
     */
    protected override val tokenUrl: String = "https://api.weixin.qq.com/sns/oauth2/access_token?grant_type=authorization_code&appid=%s&secret=%s&redirect_uri=%s&code=%s"

    /**
     * 有参数的用户url
     *   有1个参数：token
     */
    protected override val userUrl: String = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s"

    /**
     * 获得用户
     *
     * @param accessToken
     * @return
     */
    protected override fun getOauth2User(accessToken: JSONObject): Oauth2User {
        val url = userUrl.format(accessToken.getString("access_token"), accessToken.getString("openid"))
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
        return Oauth2User(type, json.getString("headimgurl"), json.getString("openid"), json.getString("nickname"), json.getIntValue("sex") == 1)
    }

}
