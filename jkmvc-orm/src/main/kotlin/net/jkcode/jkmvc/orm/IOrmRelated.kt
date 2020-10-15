package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.db.DbResultRow

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
	 */
	fun setOriginal(orgn: DbResultRow)

	/**
	 * 获得关联对象, 如果没有则查询
	 *
	 * @param name 关联对象名
	 * @param newed 是否创建新对象：在查询db后设置原始字段值data()时使用
	 * @param columns 字段名数组: Array(column1, column2, alias to column3),
	 * 						如 Array("name", "age", "birt" to "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
	 * @return
	 */
	fun getRelatedOrQuery(name:String, vararg columns:String): Any?;

	/**
	 * 获得关联对象, 如果没有则创建新对象
	 *
	 * @param name 关联对象名
	 * @param loaded 是否标记为已加载, 仅在查询db后设置原始字段值时调用
	 * @return
	 */
	fun getRelatedOrNew(name:String, loaded: Boolean = false): Any?;

	/**
	 * 获得回调的关联对象
	 *
	 * @param name 关联对象名
	 * @return
	 */
	fun cbRelated(name: String): Any?

	/**
	 * 检查是否有关联对象
	 *    一般只用于一对多 hasMany 的关系
	 *    一对一关系，你还统计个数干啥？
	 *
	 * @param name 关联对象名
	 * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则删除所有关系, 否则删除单个关系
	 * @return
	 */
	fun hasRelation(name:String, fkInMany: Any? = null): Boolean {
		return countRelation(name, fkInMany) > 0
	}

	/**
	 * 统计关联对象个数
	 *    一般只用于一对多 hasMany 的关系
	 *    一对一关系，你还统计个数干啥？
	 *
	 * @param name 关联对象名
	 * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则删除所有关系, 否则删除单个关系
	 * @return
	 */
	fun countRelation(name:String, fkInMany: Any? = null): Int

	/**
	 * 删除关联对象
	 *    一般用于删除 hasOne/hasMany 关系的从对象
	 *    你敢删除 belongsTo 关系的主对象？
	 *
	 * @param name 关系名
	 * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则删除所有关系, 否则删除单个关系
	 * @return
	 */
	fun deleteRelated(name: String, fkInMany: Any? = null): Boolean

	/**
	 * 删除关联对象
	 *    一般用于删除 hasOne/hasMany 关系的从对象
	 *    你敢删除 belongsTo 关系的主对象？
	 *
	 * @param names 关系名数组
	 */
	fun deleteRelateds(vararg names: String){
		for(name in names)
		 	deleteRelated(name)
	}

	/**
	 * 添加关系（添加从表的外键值）
	 *     一般用于添加 hasOne/hasMany 关系的从对象的外键值
	 *     至于 belongsTo 关系的主对象中只要主键，没有外键，你只能添加本对象的外键咯
	 *
	 * @param name 关系名
	 * @param value 外键值Any |关联对象IOrm
	 * @return
	 */
	fun addRelation(name:String, value: Any): Boolean

	/**
	 * 删除关系，不删除关联对象，只是将关联的外键给清空
	 *     一般用于清空 hasOne/hasMany 关系的从对象的外键值
	 *     至于 belongsTo 关系的主对象中只要主键，没有外键，你只能清空本对象的外键咯
	 *     关于外键的空值, 是外键字段的默认值(DbColumn.default), 如果没有设置字段默认值, 则为null
	 *
	 * @param name 关系名
	 * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则删除所有关系, 否则删除单个关系
	 * @return
	 */
	fun removeRelations(name:String, fkInMany: Any? = null): Boolean
}
