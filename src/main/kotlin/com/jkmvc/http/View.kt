package com.jkmvc.http

/**
 * 视图
 *
 * @Package packagename 
 * @category 
 * @author shijianhang
 * @date 2016-10-21 下午3:14:54  
 *
 */
class View 
{
	/**
	 * 全局变量
	 * @var array
	 */
	protected static globaldata = array();
	
	/**
	 * 设置全局变量
	 * @param string key
	 * @param mixed value
	 * @return View
	 */
	public fun setglobal(key, value)
	{
		static::globaldata[key] = value;
		return this;
	}
	
	/**
	 * 视图文件
	 * @var string
	 */
	protected file;
	
	/**
	 * 局部变量
	 * @var array
	 */
	protected data = array();
	
	public constructor(file, data = null)
	{
		this.file = file;
		if(data !== null)
			this.data = data;
	}
	
	/**
	 * 设置局部变量
	 * @param string key
	 * @param mixed value
	 * @return View
	 */
	public fun set(key, value)
	{
			this.data[key] = value;
			return this;
	}
	
	/**
	 * 渲染视图
	 * 
	 * @return string
	 */
	public fun render():String
	{
		// 释放变量
		extract(this.data, EXTRREFS | EXTRIP);
		
		// 开输出缓冲
		obstart();
		
		// 找到视图
		view = Loader::findfile("views", this.file);
		if(!view)
			throw new ViewException("视图文件[this.file]不存在");
			
		try {
			// 加载视图, 并输出
			include view;
			
			// 获得输出缓存
			return obgetcontents();
		} 
		catch (Exception e) 
		{
			throw new ViewException("视图[this.file]渲染出错", 500, e);
		}
		finally 
		{
			// 结束输出缓存
			obendclean();
		}
	}
	
	// 由于php中约定toString()不能抛出异常, 因此不能调用render()
	/* public fun toString()
	{
		return this.render();
	} */
}