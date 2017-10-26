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
	 * @param orgn
	 * @return
	 */
	fun original(orgn: Map<String, Any?>): IOrm;

	/**
	 * 获得关联对象
	 *
	 * @param name 关联对象名
	 * @param newed 是否创建新对象：在查询db后设置原始字段值data()时使用
	 * @param columns 字段名数组: Array(column1, column2, alias to column3),
	 * 													如 Array("name", "age", "birt" to "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
	 * @return
	 */
	fun related(name:String, newed:Boolean = false, vararg columns:String): Any?;

	/**
	 * 统计关联对象个数
	 *    一般只用于一对多 hasMany 的关系
	 *    一对一关系，你还统计个数干啥？
	 *
	 * @param name 关联对象名
	 * @return
	 */
	fun countRelated(name:String): Long

	/**
	 * 删除关联对象
	 *    一般用于删除 hasOne/hasMany 关系的从对象
	 *    你敢删除 belongsTo 关系的主对象？
	 *
	 * @param name 关系名
	 * @return
	 */
	fun deleteRelated(name: String): Boolean

	/**
	 * 删除关系，不删除关联对象，只是将关联的外键给清空
	 *     一般用于清空 hasOne/hasMany 关系的从对象的外键值
	 *     至于 belongsTo 关系的主对象中只要主键，没有外键，你只能清空本对象的外键咯
	 *
	 * @param name 关系名
	 * @param nullValue 外键的空值
	 * @return
	 */
	fun removeRelations(name:String, nullValue: Any? = null): Boolean
}
