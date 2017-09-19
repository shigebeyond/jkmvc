package com.jkmvc.session

import com.jkmvc.common.Config
import com.jkmvc.http.Request
import org.apache.commons.codec.digest.DigestUtils
import javax.servlet.http.HttpSession

/**
 * dd
 * @author shijianhang
 * @create 2017-09-19 下午11:35
 **/
object Auth {

    /**
     * 会话配置
     */
    private val sessionConfig:Config = Config.instance("session")!!;

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
    public fun getUser():IUserModel?{
        // 从session中读取登录用户
        return getSession(false).getAttribute("user") as IUserModel?;
    }

    /**
     * 登录验证
     *
     * @param username  用户名
     * @param password  密码
     * @param remember  记录登录状态：自动登录
     * @return  boolean
     */
    public fun login(username:String, password:String, remember:Boolean = false): Boolean {
        // 根据用户名查找用户
        val user = UserModel.queryBuilder().where("username", "=", username).find<UserModel>();
        if(user == null)
            return false;

        //　检查密码
        if(hash(password) != user.getString("password"))
            return false;

        // 保存登录用户到session中
        getSession(false).setAttribute("user", user);
        return true;
    }

    /**
     * 注销登录
     */
    public fun logout(){
        getSession().invalidate();
    }

    /**
     * 加密字符串，用于加密密码
     *
     * @param str
     * @return
     */
    public fun hash(str: String): String {
        return DigestUtils.md5Hex(str + sessionConfig["salt"]);
    }

}