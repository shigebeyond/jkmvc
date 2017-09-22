package com.jkmvc.session

import com.jkmvc.common.Config
import com.jkmvc.orm.IOrm

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
     * create前置处理
     */
    fun beforeCreate(){
        // 加密密码
        val field:String = sessionConfig["passwordField"]!!
        this[field] = Auth.hash(this[field])
    }
}
