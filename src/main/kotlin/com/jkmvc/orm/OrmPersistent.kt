package com.jkmvc.orm

import java.util.*

/**
 * ORM之持久化，主要是负责数据库的增删改查
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-10
 *
 */
abstract class OrmPersistent(data: MutableMap<String, Any?> = LinkedHashMap<String, Any?>()): OrmMetaData(data) {

	var loaded:Boolean = false;

	/**
	 * 判断当前记录是否存在于db: 有原始数据就认为它是存在的
	 */
	public fun exists(): Boolean {
		return loaded;
	}

	/**
	 * 获得sql构建器
	 * @return Orm_Query_Builder
	 */
	public override fun queryBuilder(): OrmQueryBuilder {
		return metadata.queryBuilder();
	}
	/**
	 * 保存数据
	 *
	 * @return int 对insert返回新增数据的主键，对update返回影响行数
	 */
	public override fun save(): Boolean {
		if(this.exists())
			return this.update();

		return this.create() > 0;
	}

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
	public override fun create(): Int {
		if(this.dirty.isEmpty())
			throw OrmException("没有要创建的数据");

		// 校验
		this.check();

		// 插入数据库
		val pk = queryBuilder().value(buildDirtyData()).insert();

		// 更新内部数据
		this.data[metadata.primaryKey] = pk; // 主键
		this.dirty.clear(); // 变化的字段值

		return pk;
	}

	public fun buildDirtyData(): MutableMap<String, Any?> {
		val result:MutableMap<String, Any?> = HashMap<String, Any?>();
		for(column in dirty){
			result[column] = data[column];
		}
		return result;
	}

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
	public override fun update(): Boolean {
		if(!this.exists())
			throw OrmException("更新对象[$this]前先检查是否存在");

		if (this.dirty.isEmpty())
			throw OrmException("没有要更新的数据");

		// 校验
		this.check();

		// 更新数据库
		val result = queryBuilder().sets(buildDirtyData()).where(metadata.primaryKey, this.pk()).update();

		// 更新内部数据
		this.dirty.clear()

		return result;
	}

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
	public override fun delete(): Boolean {
		if(!this.exists())
			throw OrmException("删除对象[$this]前先检查是否存在");

		//　校验
		if(!this.check())
			return false;

		// 删除数据
		val result = queryBuilder().where(metadata.primaryKey, this.pk()).delete();

		// 更新内部数据
		this.data.clear()
		this.dirty.clear()

		return result;
	}
}
