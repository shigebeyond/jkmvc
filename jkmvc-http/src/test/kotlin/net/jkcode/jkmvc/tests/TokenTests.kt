package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.http.session.token.TokenManager
import org.junit.Test

class TokenTests {

    @Test
    fun testToken(){
        val uid = "123"
        val token = TokenManager.generateToken(uid)
        println("生成token: $token")
        val uid2 = TokenManager.parseToken(token)
        println("解析token: $uid2")
    }
}