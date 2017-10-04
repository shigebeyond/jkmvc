package com.jkmvc.session

import com.jkmvc.common.SnowflakeIdWorker
import redis.clients.jedis.Jedis

/**
 * @ClassName: RedisTokenManager
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-10-03 11:23 AM
 */
object RedisTokenManager:ITokenManager {

    /**
     * token有效期（秒）
     */
    public val TOKEN_EXPIRES:Int = 72 * 3600

    /**
     * redis连接
     */
    private val jedis:Jedis = JedisFactory.instance()

    /**
     * 为指定用户创建一个token
     *
     * @param userId 指定用户的id
     * @return 生成的token
     */
    public override fun createToken(userId: String): String {
        //创建tokenId
        val tokenId:Long = SnowflakeIdWorker.instance().nextId()
        //存储token，key为tokenId, value是userId, 并设置过期时间
        jedis.set("tokenId-$tokenId", userId, "NX", "EX", TOKEN_EXPIRES)
        // token = userId + tokenId
        return "$userId.$tokenId"
    }

    /**
     * 检查token是否有效
     *
     * @param token
     * @param overtime 是否延长过期时间
     * @return
     */
    public override fun checkToken(token: String, overtime: Boolean): Boolean {
        if(token.isEmpty())
            return false

        // 验证token
        val result = checkToken(token)
        if(result == null)
            return false;

        //如果验证成功，说明此用户进行了一次有效操作，延长token的过期时间
        val (userId, tokenId) = result
        if(overtime)
            jedis.expire("tokenId-$tokenId", TOKEN_EXPIRES);

        return true;
    }

    /**
     * 检查token是否有效
     *
     * @param token
     * @return
     */
    private fun checkToken(token: String): Pair<String, String>? {
        // 解析token
        //val (userId, tokenId) = token.split(',')
        val i = token.lastIndexOf('.')
        val userId = token.substring(0, i)
        val tokenId = token.substring(i + 1)

        // 获得缓存的token
        val userId2 = jedis.get("tokenId-$tokenId")

        // 校验
        if(userId != userId2)
            return null

        return Pair(userId, tokenId)
    }

    /**
     * 清除token
     *
     * @param string token
     */
    public override fun deleteToken(token: String) {
        // 验证token
        val result = checkToken(token)
        if(result == null)
            return;

        // 删除token
        val (userId, tokenId) = result
        jedis.del("tokenId-$tokenId")
    }

}