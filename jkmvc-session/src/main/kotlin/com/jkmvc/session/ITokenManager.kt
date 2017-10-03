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
     * 创建一个token关联上指定用户
     * @param userId 指定用户的id
     * *
     * @return 生成的token
     */
    fun createToken(userId: String): String

    /**
     * 检查token是否有效
     * @param model token
     * *
     * @return 是否有效
     */
    fun checkToken(model: String): Boolean

    /**
     * 从字符串中解析token
     * @param authentication 加密后的字符串
     * *
     * @return
     */
    fun getToken(authentication: String): String

    /**
     * 清除token
     * @param userId 登录用户的唯一标识
     */
    fun deleteToken(userId: String)
}