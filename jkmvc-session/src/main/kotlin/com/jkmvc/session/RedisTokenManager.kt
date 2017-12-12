package com.jkmvc.session

import com.jkmvc.cache.JedisFactory
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
    private val jedis:Jedis
        get(){
            return JedisFactory.instance()
        }

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
        //jedis.set("token-$tokenId", user.pk.toString(), "NX", "EX", TOKEN_EXPIRES)
        // 序列化user
        jedis.set("token-$tokenId".toByteArray(), user.serialize(), "NX".toByteArray(), "EX".toByteArray(), TOKEN_EXPIRES)

        // token = userId + tokenId
        return "${user.pk}.$tokenId"
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
        val result = getUser(token)
        if(result == null)
            return false;

        //如果验证成功，说明此用户进行了一次有效操作，延长token的过期时间
        if(overtime){
            val tokenId = result.component2()
            jedis.expire("token-$tokenId", TOKEN_EXPIRES);
        }

        return true;
    }

    /**
     * 获得token相关的用户
     *
     * @param token
     * @return
     */
    public override fun getUser(token: String): Pair<IAuthUserModel, String>? {
        // 解析token
        //val (userId, tokenId) = token.split(',')
        val i = token.lastIndexOf('.')
        val userId = token.substring(0, i)
        val tokenId = token.substring(i + 1)

        // 获得缓存的token
        val data = jedis.get("token-$tokenId".toByteArray())
        if(data == null)
            return null

        // 反序列化user
        val user = Auth.userModel.java.newInstance() as IAuthUserModel
        user.unserialize(data)

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
        jedis.del("token-$tokenId")
    }

}