package com.jkmvc.http

/**
 * Cookie工具类
 * 	TODO: 添加cookie的加密
 * 
 * @Package packagename 
 * @category 
 * @author shijianhang
 * @date 2016-10-8 上午12:52:35 
 *
 */
class Cookie 
{

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
	public static fun get(key = null, default = null)
	{
		if(key === null)
			return COOKIE;
		
		return Arr::get(COOKIE, key, default);
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
	public static fun set(name, value = null, expiration = null)
	{
		// 多个值，则遍历递归调用
		if(isarray(name)) 
		{
			foreach (name as key => value)
			{
				// 递归调用
				static::set(key, value["value"], value["expiration"]);
			}
			return;
		}
		
		// 获得配置
		cookieConfig = Config::load("cookie");
		
		// 取默认期限
		if (expiration === null)
			expiration = cookieConfig["expiration"];

		// 转为时间戳
		if (expiration !== 0)
			expiration += time();

		// 写内存的cookie
		 COOKIE[name] = value;
		
		// 写客户端的cookie
		setcookie(name, value, expiration, cookieConfig["path"], cookieConfig["domain"], cookieConfig["secure"], cookieConfig["httponly"]);
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
	public static fun delete(name)
	{
		// 获得配置
		cookieConfig = Config::load("cookie");
		
		// 删除内存的cookie
		unset(COOKIE[name]);

		// 删除客户端的cookie： 让他过期
		return setcookie(name, null, -86400, cookieConfig["path"], cookieConfig["domain"], cookieConfig["secure"], cookieConfig["httponly"]);
	}

}
