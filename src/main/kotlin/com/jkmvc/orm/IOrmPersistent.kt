package com.jkmvc.orm

/**
 * ORM之持久化，主要是负责数据库的增删改查
 *
 * @author shijianhang
 * @date 2016-10-10
 *
 */
interface IOrmPersistent :IOrmValid {

	/**
	 * 元数据
	 */
	val metadata: IMetaData;

	/**
	 * 获得主键值
	 * @return|string
	 */
	val pk: Int;

	/**
	 * 获得sql构建器
	 * @return
	 */
	fun queryBuilder(): OrmQueryBuilder;

	/**
	 * 保存数据
	 *
	 * @return 对insert返回新增数据的主键，对update返回影响行数
	 */
	fun save(): Boolean;

	/**
	 * 插入数据: insert sql
	 *
	 * <code>
	 *    user = ModelUser();
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
	 *    user = ModelUser.queryBuilder().where("id", 1).find();
	 *    user.name = "li";
	 *    user.update();
	 * </code>
	 * 
	 * @return 影响行数
	 */
	fun update(): Boolean;

	/**
	 * 删除数据: delete sql
	 *
	 *　<code>
	 *    user = ModelUser.queryBuilder().where("id", "=", 1).find();
	 *    user.delete();
	 *　</code>
	 *
	 * @return 影响行数
	 */
	fun delete(): Boolean;
}
