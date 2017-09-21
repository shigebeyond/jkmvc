package com.jkmvc.session

import com.jkmvc.common.Config
import com.jkmvc.http.Request
import com.jkmvc.orm.Orm
import com.jkmvc.orm.modelMetaData
import org.apache.commons.codec.digest.DigestUtils
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

    /**
     * 会话配置
     */
    val sessionConfig:Config = Config.instance("session")!!;

    /**
     * 用户模型的类
     */
    val userModel: KClass<out Orm> = Class.forName(sessionConfig["userModel"]).kotlin as KClass<out Orm>;

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
    public override fun getUser(): Orm?{
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
    public override fun login(username:String, password:String): Orm? {
        // 动态获得queryBuilder，即UserModel.queryBuilder()
        val query = userModel.modelMetaData.queryBuilder()

        // 根据用户名查找用户
        val user:Orm = query.where(sessionConfig["usernameField"]!!, "=", username).find(){
            userModel.java.newInstance().original(it)
        } as Orm;
        if(user == null)
            return null;

        //　检查密码
        if(hash(password) != user.getString(sessionConfig["passwordField"]!!))
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

    /**
     * 加密字符串，用于加密密码
     *
     * @param str
     * @return
     */
    public override fun hash(str: String): String {
        return DigestUtils.md5Hex(str + sessionConfig["salt"]);
    }

}