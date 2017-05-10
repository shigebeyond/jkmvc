package com.jkmvc.orm

import java.util.*

/**
 * ORM之持久化，主要是负责数据库的增删改查
 *
 * @author shijianhang
 * @date 2016-10-10
 *
 */
abstract class OrmPersistent: OrmValid() {

	/**
	 * 判断当前记录是否存在于db: 有原始数据就认为它是存在的
	 */
	var loaded:Boolean = false;

	/**
	 * 元数据
	 *   伴随对象就是元数据
	 */
	public override val metadata:IMetaData
		get() = this::class.modelMetaData

	/**
	 * 获得主键值
	 * @return|string
	 */
	public override val pk:Int
		get() = this[metadata.primaryKey];

	/**
	 * 获得sql构建器
	 * @return
	 */
	public override fun queryBuilder(): OrmQueryBuilder {
		return metadata.queryBuilder();
	}

	/**
	 * 保存数据
	 *
	 * @return 对insert返回新增数据的主键，对update返回影响行数
	 */
	public override fun save(): Boolean {
		if(loaded)
			return update();

		return create() > 0;
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
	 * @return 新增数据的主键
	 */
	public override fun create(): Int {
		if(dirty.isEmpty())
			throw OrmException("没有要创建的数据");

		// 校验
		check();

		// 插入数据库
		val pk = queryBuilder().value(buildDirtyData()).insert();

		// 更新内部数据
		data[metadata.primaryKey] = pk; // 主键
		dirty.clear(); // 变化的字段值
		loaded = true;
		
		return pk;
	}

	/**
	* 构建要改变的数据
	 * @return
	 */
	protected fun buildDirtyData(): MutableMap<String, Any?> {
		val result:MutableMap<String, Any?> = HashMap<String, Any?>();
		// 挑出变化的属性
		for(column in dirty)
			result[column] = data[column];
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
	 * @return 影响行数
	 */
	public override fun update(): Boolean {
		if(!loaded)
			throw OrmException("更新对象[$this]前先检查是否存在");

		if (dirty.isEmpty())
			throw OrmException("没有要更新的数据");

		// 校验
		check();

		// 更新数据库
		val result = queryBuilder().sets(buildDirtyData()).where(metadata.primaryKey, pk).update();

		// 更新内部数据
		dirty.clear()

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
	 * @return 影响行数
	 */
	public override fun delete(): Boolean {
		if(!loaded)
			throw OrmException("删除对象[$this]前先检查是否存在");

		//　校验
		check();

		// 删除数据
		val result = queryBuilder().where(metadata.primaryKey, pk).delete();

		// 更新内部数据
		data.clear()
		dirty.clear()

		return result;
	}
}
