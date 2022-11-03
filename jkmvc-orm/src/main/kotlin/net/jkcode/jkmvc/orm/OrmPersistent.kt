package net.jkcode.jkmvc.orm

import net.jkcode.jkutil.common.associate
import net.jkcode.jkutil.common.dbLogger
import java.util.*

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
	public override var loaded:Boolean = false
		set(value) {
			field = value
			/* 清理_dirty
			因为有缓存的加载数据, 即 OrmMeta.getOrPutCache() 会调用 item.fromMap((cacheItem as Orm).getData()) + item.loaded = true
			而fromMap()会调用set()进而设置_dirty, 这会导致后续更新字段错乱
			因此在loaded的setter中修正_dirty
			*/
			if(value)
				_dirty.clear()
		}

	/**
	 * 元数据
	 *   伴随对象就是元数据
	 */
	public override val ormMeta: OrmMeta
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
			if(_dirty.containsAllKeys(pp))
				return pp.map {
					_dirty[it]
				}

			return pk
		}

	/**
	 * 获得sql构建器
	 *   注意是复用的
	 * @return
	 */
	public override fun queryBuilder(): OrmQueryBuilder {
		return ormMeta.queryBuilder(reused = true);
	}

	/**
	 * 根据主键值来加载数据
	 *   如果是复合主键, 则参数按 ormMeta.primaryKey 中定义的字段的属性来传值
	 *
	 * @param pks
	 * @param useCache 是否使用缓存
	 */
	public override fun loadByPk(pk: DbKeyValues, useCache: Boolean) {
		if (!isPkEmpty(pk))
			ormMeta.loadByPk(pk, this, useCache = useCache)
	}

	/**
	 * 重新加载
	 */
	public override fun reload(){
		this.clear()
		loadByPk(this.pk, useCache = false)
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
	 * @param withHasRelations 是否连带保存 hasOne/hasMany 的关联关系
	 * @param checkPkExists 是否检查主键存在
	 * @return 新增数据的主键
	 */
	public override fun create(withHasRelations: Boolean, checkPkExists: Boolean): Long {
		if(_dirty.isEmpty())
			throw OrmException("No data to create"); // 没有要创建的数据

		// 触发校验
		triggerValidate()

		// 有主键则检查主键是否存在
		if(checkPkExists) {
			val pk = this.pk
			if (!ormMeta.isPkEmpty(pk) && ormMeta.existByPk(pk, useCache = false))
				throw OrmException("Fail to create [${ormMeta.name}]: Primary key $pk exists")
		}

		// 事务
		return ormMeta.transactionWhenHandlingEvent("beforeCreate|afterCreate|beforeSave|afterSave", withHasRelations) {
			// 触发create前置事件
			triggerBeforeCreate()

			// 修正外键属性, 如果是空字符串转为null
			setForeignPropEmptyToNull()

			// 是否需要生成主键
			val needPk = !ormMeta.primaryProp.isAllEmpty() // 有主键字段
					&& !_data.containsAllKeys(ormMeta.primaryProp) // 但无主键值

			// 删除缓存 -- 可能创建前先查了一下, 检查是否有重复数据, 因此要删除缓存
			if(!needPk)
				removeCache()

			// 插入数据库
			val generatedColumn = if (needPk) ormMeta.primaryKey.first() else null // 主键名
			val pk:Long
			if(ormMeta.precompileSql) { // 优化性能: 使用编译好的sql
				val sql = ormMeta.getInsertSql(_dirty.keys)
				val params = buildDirtyValues(sql.paramNames) // 不是 _dirty.keys, 而是有序的 sql.paramNames
				pk = sql.execute(params, generatedColumn, ormMeta.db)
			}else
				pk = queryBuilder().value(buildDirtyData()).insert(generatedColumn);

			// 更新内部数据
			if (needPk)
				_data[ormMeta.primaryProp.first()] = pk; // 主键

			// 触发create后置事件
			triggerAfterCreate()

			// 添加 _data 中的 hasOne/hasMany 的关联关系
			if(withHasRelations)
				addHasNRelations()

			// 更新内部数据
			loaded = true; // save事件据此来判定是新增与修改
			_dirty.clear(); // create事件据此来获得变化的字段

			pk;
		}
	}

	/**
	* 构建要改变的数据(字段名+字段值)
	 *   在往db中写数据时调用，将本对象的变化属性值保存到db中
	 *   要做字段名转换：对象属性名 -> db字段名
	 *   要做字段值转换: 序列化
	 *
	 * @return
	 */
	protected fun buildDirtyData(): MutableMap<String, Any?> {
		// 挑出变化的属性
		return _dirty.associate { prop, oldValue ->
			// 字段名
			val column = ormMeta.prop2Column(prop)
			// 字段值
			var value = _data[prop]
			var value2 = if(value != null && ormMeta.isSerializingProp(prop))
				serializer.serialize(value)
			else
				value
			// 字段名 => 字段值
			column to value2
		}
	}

	/**
	* 构建要改变的字段值
	 *   在往db中写数据时调用，将本对象的变化属性值保存到db中
	 *   要做字段值转换: 序列化
	 *
	 * @return
	 */
	protected fun buildDirtyValues(dirtyProps: Collection<String>): List<Any?> {
		// 挑出变化的属性
		return dirtyProps.map { prop ->
			// 字段值
			var value = _data[prop]
			if(value != null && ormMeta.isSerializingProp(prop))
				serializer.serialize(value)
			else
				value
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
	protected fun setOriginal(column: String, value: Any?) {
		// 属性名
		val prop = ormMeta.column2Prop(column)
		// 属性值: 需要反序列化
		var value2 = if(value is ByteArray && ormMeta.isSerializingProp(prop))
						serializer.unserialize(value)
					else
						value
		// 设置属性值
		_data[prop] = value2;
	}

	/**
	 * 更新数据: update sql
	 *
	 * <code>
	 *    val user = UserModel.queryBuilder().where("id", 1).findRow<UserModel>();
	 *    user.name = "li";
	 *    user.update();
	 * </code>
	 *
	 * @param withHasRelations0 是否连带保存 hasOne/hasMany 的关联关系
	 * @return
	 */
	public override fun update(withHasRelations0: Boolean): Boolean {
		if(!loaded)
			throw OrmException("Load before updating object[$this]"); // 更新对象[$this]前先检查是否存在

		// 如果没有修改，则不执行sql，不抛异常，直接返回true
		if (_dirty.isEmpty()){
			dbLogger.debug("No data to update") // 没有要更新的数据
			return true;
		}

		// 触发校验
		triggerValidate()

		// 事务
		val withHasRelations = withHasRelations0 && ormMeta.hasNOrThroughRelations.isNotEmpty()
		return ormMeta.transactionWhenHandlingEvent("beforeUpdate|afterUpdate|beforeSave|afterSave", withHasRelations) {
			// 触发update前置事件
			triggerBeforeUpdate()

			// 删除缓存
			removeCache()

			// 更新数据库
			val result:Boolean
			if(ormMeta.precompileSql) { // 优化性能: 使用编译好的sql
				val sql = ormMeta.getUpdateSql(_dirty.keys)
				val params = buildDirtyValues(sql.paramNames) as MutableList // 不是 _dirty.keys, 而是有序的 sql.paramNames
				params.addAll(oldPk.columns)// 原始主键，因为主键可能被修改
				result = sql.execute(params, null, ormMeta.db) > 0
			}else
				result = queryBuilder().sets(buildDirtyData()).where(ormMeta.primaryKey, oldPk /* 原始主键，因为主键可能被修改 */).update()

			// 触发后置事件
			triggerAfterUpdate()

			// 修改(先删后加) _data 中的 hasOne/hasMany 的关联关系
			if(withHasRelations) {
				removeHasNRelationsByUpdate() // 删除旧的关系
				addHasNRelations() // 添加新的关系
			}

			// 更新内部数据
			_dirty.clear() // update事件据此来获得变化的字段
			result
		};
	}

	/**
	 * 删除数据: delete sql
	 *
	 *　<code>
	 *    val user = UserModel.queryBuilder().where("id", "=", 1).findRow<UserModel>();
	 *    user.delete();
	 *　</code>
	 *
	 * @param withHasRelations0 是否连带删除 hasOne/hasMany 的关联关系
	 * @return
	 */
	public override fun delete(withHasRelations0: Boolean): Boolean {
		if(!loaded)
			throw OrmException("Must load before deleting object[$this]"); // 删除对象[$this]前先检查是否存在

		// 事务
		val withHasRelations = withHasRelations0 && ormMeta.hasNOrThroughRelations.isNotEmpty()
		return ormMeta.transactionWhenHandlingEvent("beforeDelete|afterDelete", withHasRelations) {
			// 触发前置事件
			beforeDelete()

			// 删除缓存
			removeCache()

			// 先删除 hasOne/hasMany 的关联关系, 否则本记录可能因仍有外键约束, 而导致本记录删除失败, 报错: Cannot delete or update a parent row: a foreign key constraint fails
			if(withHasRelations)
				removeHasNRelationsByDelete()

			// 删除数据
			val result: Boolean
			if(ormMeta.precompileSql) // 优化性能: 使用编译好的sql
				result = ormMeta.deleteSqlByPk.execute(pk.toList(), null, ormMeta.db) > 0
			else
				result = queryBuilder().where(ormMeta.primaryKey, pk).delete();

			// 触发后置事件
			afterDelete()

			// 更新内部数据
			_data.clear() // delete事件据此来获得删除前的数据
			_dirty.clear()

			result;
		}
	}

	/**
	 * 删除缓存
	 */
	protected fun removeCache() {
		ormMeta.removeCache(this)
	}

	/**
	 * 字段值自增: update t1 set col1 = col1 + 1
	 *   一般用于计数字段更新
	 *   注：没更新内存
	 *
	 * <code>
	 *    val user = UserModel.queryBuilder().where("id", 1).findRow<UserModel>();
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

	/**
	 * 添加 _data 中的 hasOne/hasMany 的关联关系
	 *   仅用在 create/update() 方法中
	 *
	 */
	internal abstract fun addHasNRelations()

	/**
	 * 删除 hasOne/hasMany 的关联关系, 由 delete() 触发
	 */
	internal abstract fun removeHasNRelationsByDelete()

	/**
	 * 删除 hasOne/hasMany 的关联关系, 由 update() 触发
	 */
	internal abstract fun removeHasNRelationsByUpdate()

	/************************************ 前置后置事件+触发 *************************************/
	/**
	 * 触发校验
	 */
	internal fun triggerValidate() {
		// 触发前置事件
		beforeValidate()

		// 校验
		validateOrThrow();
	}

	/**
	 * 触发create前置事件
	 */
	internal fun triggerBeforeCreate() {
		// 触发前置事件
		beforeCreate()
		beforeSave()

		// 设置创建时间/人的字段
		setCreatedProps()
		setUpdateProps()
	}

	/**
	 * 触发create后置事件
	 */
	internal fun triggerAfterCreate() {
		afterCreate()
		afterSave()
	}

	/**
	 * 触发update前置事件
	 */
	internal fun triggerBeforeUpdate() {
		beforeUpdate()
		beforeSave()

		// 设置更新时间/人的字段
		setUpdateProps()

		// 修正外键属性, 如果是空字符串转为null
		setForeignPropEmptyToNull()
	}


	/**
	 * 触发update后置事件
	 */
	internal fun triggerAfterUpdate() {
		afterUpdate()
		afterSave()
	}

	/**
	 * 修正外键属性, 如果是空字符串转为null
	 */
	protected fun setForeignPropEmptyToNull(){
		for(prop in ormMeta.emptyToNullForeignProps){
			if(!isDirty(prop))
				continue

			val value = _data[prop]
			// 如果是空字符串转为null
			if(value is String && value.isBlank())
				_data[prop] = null
		}
	}

	/**
	 * 获得当前登录在用户的id与名字
	 * @return
	 */
	protected open fun getCurrentUserIdAndName(): Pair<Any, Any>? {
		return null
	}

	/**
	 * 设置创建时间/人的字段
	 */
	protected fun setCreatedProps() {
		// 创建时间
		if (ormMeta.createdDateProp != null)
			this[ormMeta.createdDateProp!!] = Date()

		val user = getCurrentUserIdAndName()
		if (user != null) {
			// 创建人id
			val (uid, uname) = user
			if (ormMeta.createdByProp != null)
				this[ormMeta.createdByProp!!] = uid
			// 创建人名
			if (ormMeta.createdByNameProp != null)
				this[ormMeta.createdByNameProp!!] = uname
		}
	}

	/**
	 * 设置更新时间/人的字段
	 */
	protected fun setUpdateProps() {
		// 修改时间
		if (ormMeta.modifiedDateProp != null)
			this[ormMeta.modifiedDateProp!!] = Date()

		val user = getCurrentUserIdAndName()
		if (user != null) {
			// 修改人id
			val (uid, uname) = user
			if (ormMeta.modifiedByProp != null)
				this[ormMeta.modifiedByProp!!] = uid
			// 修改人名
			if (ormMeta.modifiedByNameProp != null)
				this[ormMeta.modifiedByNameProp!!] = uname
		}
	}

}
