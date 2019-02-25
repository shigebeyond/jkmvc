package net.jkcode.jkmvc.oauth2.authorizer.impl

import com.alibaba.fastjson.JSONObject
import net.jkcode.jkmvc.oauth2.authorizer.IOauth2Authorizer
import net.jkcode.jkmvc.oauth2.authorizer.Oauth2User

/**
 * 开源中国授权处理
 * http://www.oschina.net/openapi/docs/oauth2_authorize
 *
 * @author shijianhang
 * @create 2017-10-04 下午3:29
 */
class OSChinaAuthorizer : IOauth2Authorizer() {

    /**
     * 授权类型
     */
    protected override val type: String = "oschina"

    /**
     * 有参数的授权url
     *   有3个有序的参数：1 clientId, 2 redirectUri, 3 state
     */
    protected override val authUrl:String = "http://www.oschina.net/action/oauth2/authorize?response_type=code&client_id=%s&redirect_uri=%s&state=%s"

    /**
     * 有参数的令牌url
     *   有3个有序的参数： 1 clientId，2 clientSecret, 3 redirectUri, 4 code
     */
    protected override val tokenUrl: String = "http://www.oschina.net/action/openapi/token?grant_type=authorization_code&dataType=json&client_id=%s&client_secret=%s&redirect_uri=%s&code=%s"

    /**
     * 有参数的用户url
     *   有1个参数：token
     */
    protected override val userUrl: String = "http://www.oschina.net/action/openapi/user?dataType=json&access_token=%s"

    /**
     * 从响应json中获得授权用户信息
     *
     * @param json
     * @return
     */
    protected override fun buildOauth2User(json: JSONObject): Oauth2User {
        return Oauth2User(type, json.getString("avatar"), json.getString("id"), json.getString("name"), json.getString("gender"))
    }

}
