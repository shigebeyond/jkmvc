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
	 * 判断当前记录是否存在于db: 有原始数据就认为它是存在的
	 */
	var loaded:Boolean;

	/**
	 * 获得主键值
	 */
	val pk:DbKeyValues

	/**
	 * 获得原始主键值
	 *   update()时用到，因为主键可能被修改
	 */
	val oldPk:DbKeyValues

	/**
	 * 获得sql构建器
	 * @return
	 */
	fun queryBuilder(): OrmQueryBuilder

	/**
	 * 根据主键值来加载数据
	 *   如果是联合主键, 则参数按 ormMeta.primaryKey 中定义的字段的属性来传值
	 *
	 * @param pk
	 * @return
	 */
	fun loadByPk(vararg pk: Any): IOrm

	/**
	 * 保存数据
	 *
	 * @return
	 */
	fun save(): Boolean {
		if(loaded)
			return update();

		return create() > 0;
	}

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
