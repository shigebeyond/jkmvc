package com.jkmvc.session

import com.jkmvc.common.Config
import com.jkmvc.db.dbLogger
import com.jkmvc.db.recordTranformer
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
abstract class Auth:IAuth {

    companion object{

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
            if(!IAuthUserModel::class.java.isAssignableFrom(clazz.java))
                throw IllegalArgumentException("无效用户模型的类[$className]，必须是实现[com.jkmvc.session.IAuthUserModel]接口");
            clazz
        }

        init{
            dbLogger.error("会话相关的用户模型为：" + sessionConfig.getString("userModel"))
        }

        private var _inst:Auth? = null

        /**
         * 获得单例
         */
        public fun instance():Auth{
            if(_inst == null) {
                val authType = sessionConfig.getString("authType", "Session")!!
                val clazz = "com.jkmvc.session.Auth$authType" // AuthSession 或 AuthToken
                _inst = Class.forName(clazz).newInstance() as Auth
            }
            return _inst!!
        }
    }

    /**
     * 登录验证
     *
     * @param username  用户名
     * @param password  密码
     * @return Orm?
     */
    public override fun login(username:String, password:String): IAuthUserModel? {
        // 动态获得queryBuilder，即UserModel.queryBuilder()
        val query = userModel.modelOrmMeta.queryBuilder(false, true)

        // 根据用户名查找用户
        val user = query.where(sessionConfig["usernameField"]!!, "=", username).find(transform = userModel.recordTranformer) as IAuthUserModel?;
        if(user == null)
            return null;

        //　检查密码
        val p1 = user.hash(password)
        val p2 = user.getString(sessionConfig["passwordField"]!!)
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