package net.jkcode.jkmvc.http.session.token

import net.jkcode.jkutil.cache.ICache
import net.jkcode.jkutil.common.Config
import net.jkcode.jkmvc.http.session.Auth
import net.jkcode.jkmvc.http.session.IAuthUserModel
import net.jkcode.jkmvc.orm.modelOrmMeta
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.HmacUtils

/**
 * 用缓存来管理token, 改进的jwt
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
     * token有效期（秒）:一天
     */
    public val TOKEN_EXPIRES:Long = 86400

    /**
     * 缓存
     */
    private val cache:ICache = ICache.instance(sessionConfig["tokenCache"] ?: "jedis")

    /**
     * base64加密
     */
    private fun encodeBase64(plaintext: String): String {
        return Base64.encodeBase64String(plaintext.toByteArray())
    }

    /**
     * base64解密
     */
    private fun decodeBase64(ciphertext: String): String {
        return Base64.decodeBase64(ciphertext).toString(Charsets.UTF_8)
    }

    /**
     * 签名
     */
    private fun sign(text: String, key: String): String{
        val sign = HmacUtils.hmacSha256(key, text)
        return Base64.encodeBase64String(sign)
    }

    /**
     * 为指定用户生成一个token+缓存用户信息
     *
     * @param user 指定用户的id
     * @return 生成的token
     */
    public override fun generateToken(user: IAuthUserModel): String {
        val userId = user.pk.first().toString()

        //缓存token对应的用户数据，key为userId, value是user, 并设置过期时间
        cache.put("token-$userId", user.toMap(), TOKEN_EXPIRES)

        return generateToken(userId)
    }

    /**
     * 生成token
     */
    public fun generateToken(userId: String): String {
        // 过期时间: 一天
        val expired = System.currentTimeMillis() / 1000 + TOKEN_EXPIRES

        // 计算签名
        var payload = encodeBase64(userId) + '.' + encodeBase64(expired.toString())
        val sign = sign(payload, sessionConfig["salt"]!!);

        // token = userId + 过期时间 + 签名
        return payload + '.' + sign
    }

    /**
     * 解析token
     * @param token
     * @return 用户id
     */
    public fun parseToken(token: String): String {
        // 解析token
        val parts = token.split('.')
        if(parts.size < 3)
            throw IllegalArgumentException("token格式错误")
        var (userId, expired, sign) = parts

        // 校验签名
        val sign2 = sign(userId + '.' + expired, sessionConfig["salt"]!!);
        if(sign != sign2)
            throw IllegalArgumentException("token校验签名错误")

        userId = decodeBase64(userId)
        expired = decodeBase64(expired)

        // 校验过期
        val expiredTs = expired.toLong() * 1000
        if (System.currentTimeMillis() > expiredTs)
            throw IllegalArgumentException("token已过期")

        return userId
    }

    /**
     * 获得token相关的用户
     *
     * @param token
     * @return [用户, tokenId]
     */
    public override fun getUser(token: String): IAuthUserModel? {
        val userId = parseToken(token)

        // 获得缓存的数据
        val data = cache.get("token-$userId") as Map<String, Any?>?
        if(data == null)
            return null

        // 创建用户模型
        //val user = Auth.userModel.java.newInstance() as IAuthUserModel
        val user = Auth.userModel.modelOrmMeta.newInstance() as IAuthUserModel
        user.fromMap(data)

        // 校验
        if(userId != user.pk.toString())
            return null

        return user
    }

    /**
     * 清除token
     *
     * @param string token
     */
    public override fun deleteToken(token: String) {
        // 验证token
        val user = getUser(token)
        if(user == null)
            return;

        // 删除缓存
        val userId = user.pk.first().toString()
        cache.remove("token-$userId")
    }

}