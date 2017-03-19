package com.jkmvc.http

import com.jkmvc.common.Config
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

// 获得cookie配置
val cookieConfig = Config.instance("cookie");

/**
 * 获得cookie值
 *
 * <code>
 *     theme = Cookie::get("theme", "blue");
 * </code>
 *
 * @param   string  key        cookie名
 * @param   mixed   default    默认值
 * @return  string
 */
public fun HttpServletRequest.getCookie(name:String): Cookie {
    return this.cookies.first(){
        it.name == name
    }
}

/**
 * 设置cookie值
 *
 * <code>
 *     static::set("theme", "red");
 * </code>
 *
 * @param   string  name       cookie名
 * @param   string  value      cookie值
 * @param   integer expiration 期限
 */
fun HttpServletResponse.setCookie(name:String, value:String, expiry:Int? = null){
    val cookie:Cookie = Cookie(name, value);
    // expiry
    val maxAage:Int? = cookieConfig?.getInt("expiry", expiry);
    if(maxAage != null)
        cookie.maxAge = maxAage
    // path
    val path:String? = cookieConfig?.get("path");
    if(path != null)
        cookie.path = path
    // domain
    val domain:String? = cookieConfig?.get("domain");
    if(domain != null)
        cookie.domain = domain
    // secure
    val secure:Boolean? = cookieConfig?.getBoolean("secure");
    if(secure != null)
        cookie.secure = secure
    // httponly
    val httponly:Boolean? = cookieConfig?.getBoolean("httponly");
    if(httponly != null)
        cookie.isHttpOnly = httponly
    addCookie(cookie);
}

/**
 * 设置cookie值
 *
 * <code>
 *     static::set("theme", "red");
 * </code>
 *
 * @param   string  name       cookie名
 * @param   string  value      cookie值
 * @param   integer expiration 期限
 */
public fun HttpServletResponse.setCookies(data:Map<String, String>, expiry:Int? = null){
    for((name, value) in data){
        setCookie(name, value, expiry);
    }
}

/**
 * 删除cookie
 *
 * <code>
 *     static::delete("theme");
 * </code>
 *
 * @param   string  name   cookie名
 * @return  boolean
 */
fun HttpServletResponse.deleteCookie(name:String){
    setCookie(name, "", -86400) // 让他过期
}