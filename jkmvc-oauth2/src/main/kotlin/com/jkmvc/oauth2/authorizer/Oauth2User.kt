package net.jkcode.jkmvc.oauth2.authorizer

/**
 * oauth2授权用户
 *
 * @author shijianhang
 * @create 2017-10-04 下午3:29
 **/
data class Oauth2User(
        val type: String, // 授权类型
        val openId: String,
        val nickname: String,
        val avatar: String,
        val gender: Boolean = true // 性别
) {
    public constructor(type: String, openId: String, nickname: String, avatar: String, gender: String): this(type, openId, nickname, avatar, gender == "male"){
    }
}
