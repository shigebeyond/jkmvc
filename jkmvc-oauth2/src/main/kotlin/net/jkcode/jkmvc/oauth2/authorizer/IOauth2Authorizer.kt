package net.jkcode.jkmvc.oauth2.authorizer

import com.alibaba.fastjson.JSONObject
import net.jkcode.jkmvc.common.Config
import net.jkcode.jkmvc.common.Http
import net.jkcode.jkmvc.common.IConfig
import net.jkcode.jkmvc.singleton.NamedConfiguredSingletons

/**
 * 授权者
 *
 * @author shijianhang
 * @create 2017-10-04 下午3:29
 */
public abstract class IOauth2Authorizer {

    companion object: NamedConfiguredSingletons<IOauth2Authorizer>() {

        /**
         * 单例类的配置，内容是哈希 <单例名 to 单例类>
         */
        override val instsConfig: IConfig = Config.instance("oauth2-authorizer")

    }

    /**
     * 授权类型
     */
    protected abstract val type: String

    /**
     * oauth2配置
     */
    protected val config: IConfig by lazy {
        Config.instance("oauth2.$type", "yaml")
    }

    /**
     * 有参数的授权url
     *   有3个有序的参数：1 clientId, 2 redirectUri, 3 state
     */
    protected abstract val authUrl: String

    /**
     * 有参数的令牌url
     *   有3个有序的参数： 1 clientId，2 clientSecret, 3 redirectUri, 4 code
     */
    protected abstract val tokenUrl: String

    /**
     * 有参数的用户url
     *   有1个参数：token
     */
    protected abstract val userUrl: String

    /**
     * 获得完整的授权url
     *
     * @param state
     * @return
     */
    fun getAuthorizeUrl(state: String): String {
        return authUrl.format(config.getString("clientId"), config.getString("redirectUri"), state)
    }

    /**
     * 根据code获得token
     *
     * @param code
     * @return
     */
    protected open fun getAccessToken(code: String): JSONObject {
        val url = tokenUrl.format(config.getString("clientId"), config.getString("clientSecret"), config.getString("redirectUri"), code)
        val response = Http.get(url)
        return JSONObject.parseObject(response)
    }

    /**
     * 获得授权用户
     *
     * @param code
     * @return
     */
    public fun getOauth2User(code: String): Oauth2User {
        // 根据code获得token
        val accessToken = getAccessToken(code)

        // 根据token获得用户
        return getOauth2User(accessToken)
    }

    /**
     * 获得用户
     *
     * @param accessToken
     * @return
     */
    protected open fun getOauth2User(accessToken: JSONObject): Oauth2User {
        val url = userUrl.format(accessToken.getString("access_token"))
        val response = Http.get(url)
        val json = JSONObject.parseObject(response)
        return buildOauth2User(json)
    }

    /**
     * 构建授权用户信息
     *
     * @param json
     * @return
     */
    protected abstract fun buildOauth2User(json: JSONObject): Oauth2User
}
