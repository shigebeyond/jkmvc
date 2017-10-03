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
     * @param userId 指定用户的id
     * @return 生成的token
     */
    fun createToken(userId: String): String

    /**
     * 检查token是否有效
     *
     * @param token
     * @param overtime 是否延长过期时间
     * @return
     */
    fun checkToken(token: String, overtime:Boolean = true): Boolean

    /**
     * 清除token
     *
     * @param token
     */
    fun deleteToken(token: String)
}