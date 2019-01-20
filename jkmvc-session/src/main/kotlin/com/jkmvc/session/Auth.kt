package com.jkmvc.session

import com.jkmvc.common.*
import com.jkmvc.db.dbLogger
import com.jkmvc.orm.rowTransformer
import com.jkmvc.orm.Orm
import com.jkmvc.orm.modelOrmMeta
import kotlin.reflect.KClass

/**
 * 认证用户
 *   1 登录
 *   2 注销
 *
 * @author shijianhang
 * @create 2017-09-19 下午11:35
 **/
abstract class Auth: IAuth, ClosingOnRequestEnd() {

    companion object: NamedSingleton<Auth>() {

        /**
         * 会话配置
         */
        public val sessionConfig:Config = Config.instance("session")

        /**
         * 用户模型的类
         */
        public val userModel: KClass<out Orm> by lazy {
            // 获得配置的用户模型类
            val className: String = sessionConfig["userModel"]!!
            val clazz = Class.forName(className).kotlin as KClass<out Orm>
            // 检查是否实现了 IAuthUserModel 接口
            if(!IAuthUserModel::class.java.isSuperClass(clazz.java))
                throw IllegalArgumentException("无效用户模型的类[$className]，必须是实现[com.jkmvc.session.IAuthUserModel]接口");
            clazz
        }

        init{
            dbLogger.error("会话相关的用户模型为：" + sessionConfig.getString("userModel"))
        }

        /************************ NamedSingleton 的实现 *************************/
        /**
         * 单例类的配置，内容是哈希 <单例名 to 单例类>
         */
        public override val instsConfig: IConfig = Config.instance("auth", "yaml")

        /**
         * 根据单例名来获得单例
         * @return
         */
        public fun instance(): Auth{
            return instance(sessionConfig["authType"]!!)
        }
    }

    /**
     * 登录验证
     *
     * @param username  用户名
     * @param password  密码
     * @param withs 联查的关联对象名
     * @return Orm?
     */
    public override fun login(username:String, password:String, withs: Array<String>): IAuthUserModel? {
        // 动态获得queryBuilder，即UserModel.queryBuilder()
        val query = userModel.modelOrmMeta.queryBuilder(false, true)

        // 联查
        if(withs.isNotEmpty())
            query.withs(*withs)

        // 根据用户名查找用户
        val user = query.where(sessionConfig["usernameField"]!!, "=", username).find(transform = userModel.rowTransformer) as IAuthUserModel?;
        if(user == null)
            return null;

        //　检查密码
        val p1 = user.hash(password)
        val p2 = user.get(sessionConfig["passwordField"]!!, "")
        if(p1 != p2)
            return null;

        afterLogin(user)
        return user;
    }

    /**
     * 登录后的处理
     * @param user
     */
    protected abstract fun afterLogin(user: IAuthUserModel)

}