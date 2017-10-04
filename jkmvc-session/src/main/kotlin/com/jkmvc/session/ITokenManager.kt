package com.jkmvc.session

/**
 * 管理会话的token
 *
 * @ClassName: TokenManager
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-10-03 11:21 AM
 */
interface ITokenManager {
    /**
     * 为指定用户创建一个token
     *
     * @param user 指定用户
     * @return 生成的token
     */
    fun createToken(user: IAuthUserModel): String

    /**
     * 检查token是否有效
     *
     * @param token
     * @param overtime 是否延长过期时间
     * @return
     */
    fun checkToken(token: String, overtime:Boolean = true): Boolean

    /**
     * 获得token相关的用户
     *
     * @param token
     * @return
     */
    fun getUser(token: String): Pair<IAuthUserModel, String>?

    /**
     * 清除token
     *
     * @param token
     */
    fun deleteToken(token: String)
}