package com.jkmvc.orm

/**
 * ORM之持久化，主要是负责数据库的增删改查
 *
 * @Package packagename
 * @category
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
	 * @return int|string
	 */
	public fun pk(): Int;

	/**
	 * 获得sql构建器
	 * @return Orm_Query_Builder
	 */
	public fun queryBuilder(): OrmQueryBuilder;

	/**
	 * 保存数据
	 *
	 * @return int 对insert返回新增数据的主键，对update返回影响行数
	 */
	public fun save(): Boolean;

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
	 * @return int 新增数据的主键
	 */
	public fun create(): Int;

	/**
	 * 更新数据: update sql
	 *
	 * <code>
	 *    user = ModelUser.queryBuilder().where("id", 1).find();
	 *    user.name = "li";
	 *    user.update();
	 * </code>
	 * 
	 * @return int 影响行数
	 */
	public fun update(): Boolean;

	/**
	 * 删除数据: delete sql
	 *
	 *　<code>
	 *    user = ModelUser.queryBuilder().where("id", "=", 1).find();
	 *    user.delete();
	 *　</code>
	 *
	 * @return int 影响行数
	 */
	public fun delete(): Boolean;
}
