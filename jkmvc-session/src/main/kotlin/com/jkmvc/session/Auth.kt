package com.jkmvc.session

import com.jkmvc.common.Config
import com.jkmvc.http.Request
import com.jkmvc.orm.Orm
import com.jkmvc.orm.modelMetaData
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import javax.servlet.http.HttpSession
import kotlin.reflect.KClass

/**
 * 认证用户
 *   1 登录
 *   2 注销
 *   3 密码加密
 *
 * @author shijianhang
 * @create 2017-09-19 下午11:35
 **/
object Auth:IAuth {

    // 日志
    val logger = LoggerFactory.getLogger(Auth::class.java)

    /**
     * 会话配置
     */
    val sessionConfig:Config = Config.instance("session")

    /**
     * 用户模型的类
     */
    val userModel: KClass<out Orm> by lazy {
        // 获得配置的用户模型类
        val className: String = sessionConfig["userModel"]!!
        val clazz = Class.forName(className).kotlin as KClass<out Orm>
        // 检查是否实现了 IAuthUserModel 接口
        if(IAuthUserModel::class.java.isAssignableFrom(clazz.java))
            throw IllegalArgumentException("无效用户模型的类[$className]，必须是实现[com.jkmvc.session.IAuthUserModel]接口");
        clazz
    }

    init{
        logger.error(sessionConfig.getString("userModel"))
    }

    /**
     * 获得当前会话
     *
     * @param create 是否创建新的会话
     * @return
     */
    private fun getSession(create:Boolean = true): HttpSession {
        return Request.current().getSession(create);
    }

    /**
     * 获得当前登录用户
     * @return
     */
    public override fun getUser(): IAuthUserModel?{
        // 从session中读取登录用户
        return getSession(false).getAttribute("user") as Orm?;
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
        val query = userModel.modelMetaData.queryBuilder()

        // 根据用户名查找用户
        val user = query.where(sessionConfig["usernameField"]!!, "=", username).find(){
            userModel.java.newInstance().original(it)
        } as IAuthUserModel?;
        if(user == null)
            return null;

        //　检查密码
        if(user.hash(password) != user.getString(sessionConfig["passwordField"]!!))
            return null;

        // 保存登录用户到session中
        getSession(false).setAttribute("user", user);
        return user;
    }

    /**
     * 注销登录
     */
    public override fun logout(){
        getSession().invalidate();
    }

}