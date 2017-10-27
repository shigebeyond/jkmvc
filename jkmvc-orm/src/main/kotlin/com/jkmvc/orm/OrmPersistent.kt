package com.jkmvc.orm

import com.jkmvc.db.dbLogger
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
	public override val ormMeta: IOrmMeta
		get() = this::class.modelOrmMeta

	/**
	 * 获得主键值
	 * @return|string
	 */
	public override val pk:Any?
		get() = this[ormMeta.primaryProp];

	/**
	 * 获得sql构建器
	 * @return
	 */
	public override fun queryBuilder(): OrmQueryBuilder {
		return ormMeta.queryBuilder();
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

		// 触发前置事件
		fireEvent("beforeCreate")
		fireEvent("beforeSave")

		// 插入数据库
		val needPk = !data.containsKey(ormMeta.primaryProp) // 是否需要生成主键
		val pk = queryBuilder().value(buildDirtyData()).insert(needPk);

		// 更新内部数据
		if(needPk)
			data[ormMeta.primaryProp] = pk; // 主键

		// 触发后置事件
		fireEvent("afterCreate")
		fireEvent("afterSave")

		// beforeSave 与 afterCreate 事件根据这个来判定是新增与修改 + 变化的字段
		loaded = true;
		dirty.clear();

		return pk;
	}

	/**
	* 构建要改变的数据
	 *   在往db中写数据时调用，将本对象的变化属性值保存到db中
	 *   要做字段转换：对象属性名 -> db字段名
	 *
	 * @return
	 */
	protected fun buildDirtyData(): MutableMap<String, Any?> {
		val result:MutableMap<String, Any?> = HashMap<String, Any?>();
		// 挑出变化的属性
		for((prop, oldValue) in dirty) {
			val column = ormMeta.prop2Column(prop)
			result[column] = data[prop];
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
	 * @return 影响行数
	 */
	public override fun update(): Boolean {
		if(!loaded)
			throw OrmException("更新对象[$this]前先检查是否存在");

		// 如果没有修改，则不执行sql，不抛异常，直接返回true
		if (dirty.isEmpty()){
			//throw OrmException("没有要更新的数据");
			dbLogger.debug("执行${javaClass}.update()成功：没有要更新的数据")
			return true;
		}

		// 校验
		check();

		// 触发前置事件
		fireEvent("beforeUpdate")
		fireEvent("beforeSave")

		// 更新数据库
		val result = queryBuilder().sets(buildDirtyData()).where(ormMeta.primaryKey, pk).update();

		// 触发后置事件
		fireEvent("afterUpdate")
		fireEvent("afterSave")

		// beforeSave 与 afterCreate 事件根据这个来判定变化的字段
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

		// 触发前置事件
		fireEvent("beforeDelete")

		// 删除数据
		val result = queryBuilder().where(ormMeta.primaryKey, pk).delete();

		// 更新内部数据
		data.clear()
		dirty.clear()

		// 触发后置事件
		fireEvent("afterDelete")

		return result;
	}

	/**
	 * 触发事件
	 *   通过反射来调用事件处理函数，纯属装逼
	 *   其实可以显式调用，但是需要在Orm类中事先声明各个事件的处理函数
	 *
	 * @param event 事件名
	 */
	public override fun fireEvent(event:String){
		ormMeta.getEventHandler(event)?.call(this)
	}
}
