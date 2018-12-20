package com.jkmvc.session.token

import com.jkmvc.cache.ICache
import com.jkmvc.common.Config
import com.jkmvc.common.SnowflakeIdWorker
import com.jkmvc.session.Auth
import com.jkmvc.session.IAuthUserModel

/**
 * 用缓存来管理token
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2017-10-03 11:23 AM
 */
object TokenManager : ITokenManager {

    /**
     * 会话配置
     */
    public val sessionConfig: Config = Config.instance("session")

    /**
     * token有效期（秒）
     */
    public val TOKEN_EXPIRES:Int = 72 * 3600

    /**
     * 缓存
     */
    private val cache:ICache = ICache.instance(sessionConfig["tokenCache"]!!)

    /**
     * 为指定用户创建一个token
     *
     * @param user 指定用户的id
     * @return 生成的token
     */
    public override fun createToken(user: IAuthUserModel): String {
        //创建tokenId
        val tokenId:Long = SnowflakeIdWorker.instance().nextId()

        //存储token，key为tokenId, value是user, 并设置过期时间
        cache.put("token-$tokenId", user.toMap(), TOKEN_EXPIRES)

        // token = userId + tokenId
        return "${user.pk}.$tokenId"
    }

    /**
     * 获得token相关的用户
     *
     * @param token
     * @return [用户, tokenId]
     */
    public override fun getUser(token: String): Pair<IAuthUserModel, String>? {
        // 解析token
        //val (userId, tokenId) = token.split(',')
        val i = token.lastIndexOf('.')
        val userId = token.substring(0, i)
        val tokenId = token.substring(i + 1)

        // 获得缓存的数据
        val data = cache.get("token-$tokenId") as Map<String, Any?>?
        if(data == null)
            return null

        // 创建用户模型
        val user = Auth.userModel.java.newInstance() as IAuthUserModel
        user.fromMap(data)

        // 校验
        if(userId != user.pk.toString())
            return null

        return Pair(user, tokenId)
    }

    /**
     * 清除token
     *
     * @param string token
     */
    public override fun deleteToken(token: String) {
        // 验证token
        val result = getUser(token)
        if(result == null)
            return;

        // 删除token
        val (user, tokenId) = result
        cache.remove("token-$tokenId")
    }

}