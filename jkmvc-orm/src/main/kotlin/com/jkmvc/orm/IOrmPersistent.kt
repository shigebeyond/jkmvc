package com.jkmvc.orm

/**
 * ORM之持久化，主要是负责数据库的增删改查
 *
 * @author shijianhang
 * @date 2016-10-10
 *
 */
interface IOrmPersistent : IOrmValid {

	/**
	 * 元数据
	 */
	val ormMeta: IOrmMeta;

	/**
	 * 获得主键值
	 */
	val pk:DbKeyValue

	/**
	 * 获得原始主键值
	 *   update()时用到，因为主键可能被修改
	 */
	val oldPk:DbKeyValue

	/**
	 * 获得sql构建器
	 * @return
	 */
	fun queryBuilder(): OrmQueryBuilder;

	/**
	 * 保存数据
	 *
	 * @return
	 */
	fun save(): Boolean;

	/**
	 * 插入数据: insert sql
	 *
	 * <code>
	 *    val user = ModelUser();
	 *    user.name = "shi";
	 *    user.age = 24;
	 *    user.create();
	 * </code>
	 * 
	 * @return 新增数据的主键
	 */
	fun create(): Int;

	/**
	 * 更新数据: update sql
	 *
	 * <code>
	 *    val user = ModelUser.queryBuilder().where("id", 1).find();
	 *    user.name = "li";
	 *    user.update();
	 * </code>
	 * 
	 * @return 
	 */
	fun update(): Boolean;

	/**
	 * 删除数据: delete sql
	 *
	 *　<code>
	 *    val user = ModelUser.queryBuilder().where("id", "=", 1).find();
	 *    user.delete();
	 *　</code>
	 *
	 * @return 
	 */
	fun delete(): Boolean;

	/**
	 * 字段值自增: update t1 set col1 = col1 + 1
	 *
	 * <code>
	 *    val user = ModelUser.queryBuilder().where("id", 1).find();
	 *    user.incr("age", 1);
	 * </code>
	 *
	 * @return
	 */
	fun incr(prop: String, step: Int): Boolean;

	/**
	 * 触发事件
	 *
	 * @param event 事件名
	 */
	fun fireEvent(event:String)
}
