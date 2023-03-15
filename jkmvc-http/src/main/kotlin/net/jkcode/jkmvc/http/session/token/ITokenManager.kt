package net.jkcode.jkmvc.http.session.token

import net.jkcode.jkmvc.http.session.IAuthUserModel

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
     * 为指定用户生成一个token+缓存用户信息
     *
     * @param user 指定用户
     * @return 生成的token
     */
    fun generateToken(user: IAuthUserModel): String

    /**
     * 获得token相关的用户
     *
     * @param token
     * @return 用户
     */
    fun getUser(token: String): IAuthUserModel?

    /**
     * 清除token
     *
     * @param token
     */
    fun deleteToken(token: String)
}