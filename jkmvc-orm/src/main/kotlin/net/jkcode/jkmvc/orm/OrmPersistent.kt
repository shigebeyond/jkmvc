package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.common.associate
import net.jkcode.jkmvc.db.MutableRow
import net.jkcode.jkmvc.common.dbLogger

/**
 * ORM之持久化，主要是负责数据库的增删改查
 *
 * @author shijianhang
 * @date 2016-10-10
 *
 */
abstract class OrmPersistent : OrmValid() {

	/**
	 * 判断当前记录是否存在于db: 有原始数据就认为它是存在的
	 */
	public override var loaded:Boolean = false;

	/**
	 * 元数据
	 *   伴随对象就是元数据
	 */
	public override val ormMeta: IOrmMeta
		get() = this::class.modelOrmMeta

	/**
	 * 获得主键值
	 */
	public override val pk:DbKeyValues
		get() = gets(ormMeta.primaryProp)

	/**
	 * 获得原始主键值
	 *   update()时用到，因为主键可能被修改
	 */
	public override val oldPk:DbKeyValues
		get(){
			val pp = ormMeta.primaryProp
			if(dirty.containsAllKeys(pp))
				return pp.map {
					dirty[it]
				}

			return pk
		}

	/**
	 * 获得sql构建器
	 * @return
	 */
	public override fun queryBuilder(): OrmQueryBuilder {
		return ormMeta.queryBuilder();
	}

	/**
	 * 根据主键值来加载数据
	 *   如果是复合主键, 则参数按 ormMeta.primaryKey 中定义的字段的属性来传值
	 *
	 * @param pk
	 */
	public override fun loadByPk(vararg pk: Any): Unit {
		if(pk.isNotEmpty())
			queryBuilder().where(ormMeta.primaryKey, DbKeyValues(pk)).find(){
				this.setOriginal(it)
				this
			}
	}

	/**
	 * 插入数据: insert sql
	 *
	 * <code>
	 *    val user = UserModel();
	 *    user.name = "shi";
	 *    user.age = 24;
	 *    user.create();
	 * </code>
	 * 
	 * @return 新增数据的主键
	 */
	public override fun create(): Long {
		if(dirty.isEmpty())
			throw OrmException("No data to create"); // 没有要创建的数据

		// 校验
		validate();

		// 事务
		return ormMeta.transactionWhenHandlingEvent("beforeCreate|afterCreate|beforeSave|afterSave") {
			// 触发前置事件
			beforeCreate()
			beforeSave()

			// 插入数据库
			val needPk = !ormMeta.primaryProp.isAllEmpty() && !data.containsAllKeys(ormMeta.primaryProp) // 是否需要生成主键
			val generatedColumn = if (needPk) ormMeta.primaryKey.first() else null // 主键名
			val pk = queryBuilder().value(buildDirtyData()).insert(generatedColumn);

			// 更新内部数据
			if (needPk)
				data[ormMeta.primaryProp.first()] = pk; // 主键

			// 触发后置事件
			afterCreate()
			afterSave()

			// 更新内部数据
			loaded = true; // save事件据此来判定是新增与修改
			dirty.clear(); // create事件据此来获得变化的字段

			pk;
		}
	}

	/**
	* 构建要改变的数据
	 *   在往db中写数据时调用，将本对象的变化属性值保存到db中
	 *   要做字段名转换：对象属性名 -> db字段名
	 *   要做字段值转换: 序列化
	 *
	 * @return
	 */
	protected fun buildDirtyData(): MutableRow {
		// 挑出变化的属性
		return dirty.associate { prop, oldValue ->
			// 字段名
			val column = ormMeta.prop2Column(prop)
			// 字段值
			var value = data[prop]
			var value2 = if(value != null && ormMeta.serializingProps.contains(prop))
				serializer.serialize(value)
			else
				value
			// 字段名 => 字段值
			column to value2
		}
	}

	/**
	 * 设置原始的单个字段值
	 *    在从db中读数据时调用，来赋值给本对象属性
	 *    要做字段名转换：db字段名 -> 对象属性名
	 *    要做字段值转换: 反序列化
	 *    被 OrmRelated::setOriginal(orgn: Row) 逐个字段调用
	 *
	 * @param column 字段名
	 * @param value 字段值
	 */
	protected fun setOriginal(column: String, value: Any?): Unit{
		// 属性名
		val prop = ormMeta.column2Prop(column)
		// 属性值: 需要反序列化
		var value2 = if(value is ByteArray && ormMeta.serializingProps.contains(prop))
						serializer.unserialize(value)
					else
						value
		// 设置属性值
		data[prop] = value2;
	}

	/**
	 * 更新数据: update sql
	 *
	 * <code>
	 *    val user = UserModel.queryBuilder().where("id", 1).find<UserModel>();
	 *    user.name = "li";
	 *    user.update();
	 * </code>
	 * 
	 * @return
	 */
	public override fun update(): Boolean {
		if(!loaded)
			throw OrmException("Load before updating object[$this]"); // 更新对象[$this]前先检查是否存在

		// 如果没有修改，则不执行sql，不抛异常，直接返回true
		if (dirty.isEmpty()){
			dbLogger.debug("No data to update") // 没有要更新的数据
			return true;
		}

		// 校验
		validate();

		// 事务
		return ormMeta.transactionWhenHandlingEvent("beforeUpdate|afterUpdate|beforeSave|afterSave") {
			// 触发前置事件
			beforeUpdate()
			beforeSave()

			// 更新数据库
			val result = queryBuilder().sets(buildDirtyData()).where(ormMeta.primaryKey, oldPk /* 原始主键，因为主键可能被修改 */).update();

			// 触发后置事件
			afterUpdate()
			afterSave()

			// 更新内部数据
			dirty.clear() // update事件据此来获得变化的字段
			result
		};
	}

	/**
	 * 删除数据: delete sql
	 *
	 *　<code>
	 *    val user = UserModel.queryBuilder().where("id", "=", 1).find<UserModel>();
	 *    user.delete();
	 *　</code>
	 *
	 * @return
	 */
	public override fun delete(): Boolean {
		if(!loaded)
			throw OrmException("Load before deleting object[$this]"); // 删除对象[$this]前先检查是否存在

		// 事务
		return ormMeta.transactionWhenHandlingEvent("beforeDelete|afterDelete") {
			// 触发前置事件
			beforeDelete()

			// 删除数据
			val result = queryBuilder().where(ormMeta.primaryKey, "=", pk).delete();

			// 触发后置事件
			afterDelete()

			// 更新内部数据
			data.clear() // delete事件据此来获得删除前的数据
			dirty.clear()

			result;
		}
	}

	/**
	 * 字段值自增: update t1 set col1 = col1 + 1
	 *   一般用于计数字段更新
	 *   注：没更新内存
	 *
	 * <code>
	 *    val user = UserModel.queryBuilder().where("id", 1).find<UserModel>();
	 *    user.incr("age", 1);
	 * </code>
	 *
	 * @return
	 */
	public override fun incr(prop: String, step: Int): Boolean {
		if(step == 0)
			return false

		val column = ormMeta.prop2Column(prop)
		return queryBuilder().set(column, "$column + $step", true).where(ormMeta.primaryKey, pk).update();
	}

}
