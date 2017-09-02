package com.jkmvc.orm

/**
 * ORM之关联对象操作
 *
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
interface IOrmRelated : IOrmPersistent
{
	/**
	 * 设置原始的字段值
	 *
	 * @param data
	 * @return
	 */
	public fun original(data: Map<String, Any?>): IOrm;

	/**
	 * 获得关联对象
	 *
	 * @param name 关联对象名
	 * @param newed 是否创建新对象：在查询db后设置原始字段值data()时使用
	 * @param columns 字段名数组: Array(column1, column2, alias to column3),
	 * 													如 Array("name", "age", "birt" to "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
	 * @return
	 */
	public fun related(name:String, newed:Boolean, vararg columns:String): Any?;
}
