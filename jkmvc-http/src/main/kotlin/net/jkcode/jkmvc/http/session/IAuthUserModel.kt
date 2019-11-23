package net.jkcode.jkmvc.http.session

import net.jkcode.jkutil.common.Config
import net.jkcode.jkmvc.orm.IOrm
import org.apache.commons.codec.digest.DigestUtils

/**
 * 会话相关的用户模型接口
 *    如需引入会话，要实现该接口，并在 session.properties 将该实现类赋值给 userModel 项
 *
 * @author shijianhang
 * @create 2017-09-19 下午11:35
 */
interface IAuthUserModel : IOrm {

    companion object{
        /**
         * 会话配置
         */
        protected val sessionConfig: Config = Config.instance("session");
    }

    /**
     * 加密字符串，用于加密密码
     *
     * @param str
     * @return
     */
    fun hash(str: String): String {
        return DigestUtils.md5Hex(str + sessionConfig["salt"]);
    }

    /**
     * create前置处理
     */
    override fun beforeCreate(){
        // 加密密码
        val field:String = sessionConfig["passwordField"]!!
        this[field] = hash(this[field])
    }
}
