package net.jkcode.jkmvc.orm

import com.thoughtworks.xstream.XStream
import net.jkcode.jkmvc.db.DbColumn
import net.jkcode.jkmvc.db.DbTable
import net.jkcode.jkmvc.db.IDb
import net.jkcode.jkmvc.orm.relation.HasNThroughRelation
import net.jkcode.jkmvc.orm.relation.ICbRelation
import net.jkcode.jkmvc.orm.relation.IRelation
import net.jkcode.jkutil.collection.FixedKeyMapFactory
import net.jkcode.jkutil.common.any
import net.jkcode.jkutil.validator.IValidator
import net.jkcode.jkutil.validator.ModelValidateResult
import net.jkcode.jkutil.validator.RuleValidator
import net.jkcode.jkutil.validator.ValidateLambda
import kotlin.reflect.KClass

/**
 * orm的元数据
 * 　模型映射表的映射元数据，如模型类/数据库/表名
 *
 *   关于 cascadeDeleted:
 *      只对 hasOne/hasMany 有效, 对 belongsTo/hasOneThrough/hasManyThrough 无效
 *      对 belongsTo, 你敢删除 belongsTo 关系的主对象？
 *      对 hasOneThrough/hasManyThrough, 都通过中间表来关联了, 两者之间肯定是独立维护的, 只删除关联关系就好, 不删除关联对象
 */
interface IOrmMeta {

    /**
     * 模型类
     */
    val model: KClass<out IOrm>

    /**
     * 模型名
     */
    val name:String

    /**
     * 模型中文名
     */
    val label:String

    /**
     * 数据库名
     */
    val dbName:String

    /**
     * 表名
     */
    var table:String

    /**
     * 主键
     */
    val primaryKey:DbKeyNames

    /**
     * 主键属性
     */
    val primaryProp:DbKeyNames

    /**
     * 默认外键
     */
    val defaultForeignKey:DbKeyNames

    /**
     * 关联关系
     */
    val relations: Map<String, IRelation>

    /**
     * 每个字段的规则
     */
    val rules: MutableMap<String, IValidator>

    /**
     * 获得实体类: 模型类实现 IEntitiableOrm 接口时, 指定的泛型类型
     */
    val entityClass: Class<*>?

    /**
     * 数据库
     */
    val db: IDb

    /**
     * db表
     */
    val dbTable: DbTable

    /**
     * db列
     */
    val dbColumns: Map<String, DbColumn>

    /**
     * 检查主键为空的规则
     */
    val pkEmptyRule: PkEmptyRule

    /**
     * 表字段
     */
    val columns: Collection<String>

    /**
     * 对象属性名
     */
    val props: List<String>

    /**
     * 对象属性名+关系名
     */
    val propsAndRelations: List<String>

    /**
     * 要序列化的对象属性
     *   写时序列化, 读时反序列化
     */
    val serializingProps: List<String>


    /**
     * 如果是空字符串转为null的外键属性
     */
    val emptyToNullForeignProps: List<String>

    /**
     * 创建时间
     */
    val createdDateProp: String?

    /**
     * 创建人id
     */
    val createdByProp: String?

    /**
     * 创建人名
     */
    val createdByNameProp: String?

    /**
     * 修改时间
     */
    val modifiedDateProp: String?

    /**
     * 修改人id
     */
    val modifiedByProp: String?

    /**
     * 修改人名
     */
    val modifiedByNameProp: String?

    /**
     * 数据的工厂
     */
    val dataFactory: FixedKeyMapFactory

    /**
     * 检查主键值是否为空
     */
    fun isPkEmpty(pk: Any?): Boolean {
        return pkEmptyRule.isEmpty(pk)
    }

    /**
     * 熟悉是否需要序列化
     * @param prop
     * @return
     */
    fun isSerializingProp(prop: String): Boolean {
        return serializingProps.contains(prop)
    }

    /**
     * 根据对象属性名，获得db字段名 -- 单个字段
     *    可根据实际需要在 model 类中重写
     *
     * @param prop 对象属性名
     * @return db字段名
     */
    fun prop2Column(prop:String): String {
        return db.prop2Column(prop)
    }

    /**
     * 根据db字段名，获得对象属性名 -- 单个字段
     *
     * @param column db字段名
     * @return 对象属性名
     */
    fun column2Prop(column:String): String {
       return db.column2Prop(column)
    }

    /**
     * 智能转换字段值
     *    在不知字段类型的情况下，将string赋值给属性
     *    => 需要将string转换为属性类型
     *    => 需要显式声明属性
     *
     * @param column
     * @param value 字符串
     */
    fun convertIntelligent(column:String, value:String):Any?

    /**
     * 创建实例
     *    使用无参数构造函数/可变参数构造参数来实例化
     * @return
     */
    fun newInstance(): IOrm

    /************************************ 缓存 *************************************/
    /**
     * 缓存配置
     */
    val cacheMeta: OrmCacheMeta?

    /**
     * 读缓存, 无则读db
     * @param pk 要查询的主键
     * @param item 要赋值的对象
     * @param expires 缓存过期时间
     * @return
     */
    fun <T:IOrm> getOrPutCache(pk: DbKeyValues, item: T? = null, expires:Long = 5 * 3600): T?

    /**
     * 根据主键值来删除缓存
     * @param item
     */
    fun removeCache(item: IOrm)

    /************************************ 事件 *************************************/
    /**
     * 校验orm对象数据
     * @param item
     * @return
     */
    fun validate(item: IOrmEntity): ModelValidateResult

    /**
     * 添加规则
     * @param field
     * @param label
     * @param rule
     * @return
     */
    fun addRule(field: String, label:String, rule: String = "", otherRule: ValidateLambda? = null): OrmMeta{
        return addRule(field, RuleValidator(label, rule).combile(otherRule))
    }

    /**
     * 添加规则
     * @param field
     * @param rule
     * @return
     */
    fun addRule(field: String, rule: IValidator): OrmMeta;

    /************************************ query builder *************************************/
    /**
     * 获得orm查询构建器
     *
     * @param convertingValue 查询时是否智能转换字段值
     * @param convertingColumn 查询时是否智能转换字段名
     * @param withSelect with()联查时自动select关联表的字段
     * @return
     */
    fun queryBuilder(convertingValue: Boolean = false, convertingColumn: Boolean = false, withSelect: Boolean = true): OrmQueryBuilder;

    /**
     * 获得orm查询构建器
     *
     *
     * @param sort 排序字段
     * @param desc 是否降序
     * @param start 偏移
     * @param rows 查询行数
     * @return
     */
    fun queryBuilder(sort: String?, desc: Boolean? = null, start: Int? = null, rows: Int? = null): OrmQueryBuilder {
        val query = queryBuilder()

        if (sort != null && sort != "")
            query.orderBy(sort, desc)

        if (rows != null && rows > 0)
            query.limit(rows, start ?: 0)

        return query
    }

    /**
     * 获得orm查询构建器
     *
     *
     * @param condition 条件
     * @param params 条件参数
     * @param sort 排序字段
     * @param desc 是否降序
     * @param start 偏移
     * @param rows 查询行数
     * @return
     */
    fun queryBuilder(condition: String, params: List<*> = emptyList<Any>(), sort: String? = null, desc: Boolean? = null, start: Int? = null, rows: Int? = null): OrmQueryBuilder {
        val query = queryBuilder(sort, desc, start, rows)
        query.whereCondition(condition, params)
        return query
    }

    /**
     * 根据主键值来加载数据
     * @param pks 要查询的主键
     * @param item 要赋值的对象
     */
    public fun loadByPk(vararg pks: Any, item: IOrm){
        if(pks.isNotEmpty())
            loadByPk(DbKeyValues(*pks), item)
    }

    /**
     * 根据主键值来加载数据
     * @param pk 要查询的主键
     * @param item 要赋值的对象
     */
    public fun loadByPk(pk: DbKeyValues, item: IOrm)

    /**
     * 根据主键值来查找数据
     * @param pks 要查询的主键
     * @return
     */
    public fun <T: IOrm> findByPk(vararg pks: Any): T?{
        if(pks.isEmpty())
            return null

        return findByPk(DbKeyValues(*pks))
    }

    /**
     * 根据主键值来查找数据
     * @param pk 要查询的主键
     * @return
     */
    public fun <T: IOrm> findByPk(pk: DbKeyValues): T?

    /**
     * 检查是否存在主键值对应的数据
     * @param pks 要检查的主键
     * @return
     */
    public fun existByPk(vararg pks: Any): Boolean {
        if(pks.isEmpty())
            return false

        return existByPk(DbKeyValues(*pks))
    }

    /**
     * 根据主键值来查找数据
     * @param pk 要查询的主键
     * @return
     */
    public fun existByPk(pk: DbKeyValues): Boolean{
        return findByPk<IOrm>(pk) != null
    }

    /**
     * 根据主键值来删除数据
     * @param pks 要删除的主键
     * @param withHasRelations 是否连带保存 hasOne/hasMany 的关联关系
     * @return
     */
    public fun deleteByPk(vararg pks: Any, withHasRelations: Boolean = false): Boolean{
        return deleteByPk(DbKeyValues(*pks), withHasRelations)
    }

    /**
     * 根据主键值来删除数据
     * @param pk 要删除的主键
     * @param withHasRelations 是否连带保存 hasOne/hasMany 的关联关系
     * @return
     */
    public fun deleteByPk(pk: DbKeyValues, withHasRelations: Boolean = false): Boolean

    /**
     * 批量插入
     *   一般用于批量插入 OrmEntity 对象, 而不是 Orm 对象, 因此也不会触发 Orm 中的前置后置事件
     *
     * @param items
     * @return
     */
    fun batchInsert(items: List<IOrmEntity>): IntArray

    /**
     * 批量更新
     *   一般用于批量更新 OrmEntity 对象, 而不是 Orm 对象, 因此也不会触发 Orm 中的前置后置事件
     *
     * @param items
     * @return
     */
    fun batchUpdate(items: List<IOrmEntity>): IntArray

    /**
     * 单个删除
     *
     * @param pk 主键值, 可能是单主键(Any), 也可能是多主键(DbKey)
     * @return
     */
    fun delete(pk: Any): Boolean

    /**
     * 批量删除
     *
     * @param pks 主键值列表, 主键值可能是单主键(Any), 也可能是多主键(DbKey)
     * @return
     */
    fun batchDelete(vararg pks: Any): IntArray

    /**
     * 批量删除
     *
     * @param pks 主键值列表, 主键值可能是单主键(Any), 也可能是多主键(DbKey)
     * @return
     */
    fun batchDelete(vararg pks: DbKeyValues): IntArray{
        return batchDelete(*(pks as Array<Any>))
    }

    /**
     * 联查关联表
     *
     * @param query 查询构建器
     * @param name 关联关系名, 类型 String|DbExpr(关系名+别名)
     * @param select 是否select关联字段
     * @param columns 关联字段列表
     * @param lastName 上一级关系名, 类型 String|DbExpr(关系名+别名)
     * @param path 列名父路径
     * @param queryAction 查询对象的回调函数, 只针对 hasMany 关系
     * @return 关联关系
     */
    fun joinRelated(query: OrmQueryBuilder, name: CharSequence, select: Boolean, columns: SelectColumnList?, lastName:CharSequence = this.name, path:String = "", queryAction: ((OrmQueryBuilder)->Unit)? = null): IRelation

    /************************************ 事件 *************************************/
    /**
     * 能处理的事件(只是增删改, 不包含查)
     */
    val processableEvents: List<String>

    /**
     * 能否处理任一事件(只是增删改, 不包含查)
     * @param events 多个事件名，以|分隔，如 beforeCreate|afterCreate
     * @return
     */
    fun canHandleAnyEvent(events:String): Boolean{
        return processableEvents.any { event ->
            events.contains(event)
        }
    }

    /**
     * 如果有要处理的事件(只是增删改, 不包含查)，则开启事务
     *
     * @param events 多个事件名，以|分隔，如 beforeCreate|afterCreate
     * @param withHasRelations 是否连带保存 hasOne/hasMany 的关联关系
     * @param statement
     * @return
     */
    fun <T> transactionWhenHandlingEvent(events:String, withHasRelations: Boolean, statement: () -> T): T

    /************************************ 关联关系 *************************************/
    /**
     * hasN关联关系
     */
    public val hasNOrThroughRelations: List<IRelation>

    /**
     * 是否有某个关联关系
     * @param name
     * @return
     */
    fun hasRelation(name:String):Boolean;

    /**
     * 获得某个关联关系
     * @param name
     * @return
     */
    fun getRelation(name:String): IRelation?;

    /************************************ 添加关联关系: 复合主键版本, 主外键类型为 DbKeyNames *************************************/
    /**
     * 生成属性代理 + 设置关联关系(belongs to)
     *
     *    公式：从表.外键 = 主表.主键
     *             外键默认值 = 主表_主键 （= 关联表_主键）
     *             主键默认值 = 主表的主键 （= 关联表的主键）
     *
     *    其中本表从属于关联表，因此 本表是从表，关联表是主表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param conditions 关联查询条件
     * @return
     */
    fun belongsTo(name:String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyNames, primaryKey:DbKeyNames, conditions:Map<String, Any?> = emptyMap()): IOrmMeta;

    /**
     * 生成属性代理 + 设置关联关系(belongs to)
     *
     *    公式：从表.外键 = 主表.主键
     *             外键默认值 = 主表_主键 （= 关联表_主键）
     *             主键默认值 = 主表的主键 （= 关联表的主键）
     *
     *    其中本表从属于关联表，因此 本表是从表，关联表是主表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param conditions 关联查询条件
     * @return
     */
    fun belongsTo(name:String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyNames = relatedModel.modelOrmMeta.defaultForeignKey /* 主表_主键 = 关联表_主键 */, conditions:Map<String, Any?> = emptyMap()): IOrmMeta{
        return belongsTo(name, relatedModel, foreignKey, relatedModel.modelOrmMeta.primaryKey /* 关联表的主键 */, conditions)
    }

    /**
     * 设置关联关系(has one)
     *
     * 公式：从表.外键 = 主表.主键
     *          外键默认值 = 主表_主键（= 本表_主键）
     *          主键默认值 = 主表的主键（= 本表的主键）
     *
     *    其中本表有一个关联表，因此 本表是主表，关联表是从表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param conditions 关联查询条件
     * @param cascadeDeleted 是否级联删除
     * @return
     */
    fun hasOne(name: String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyNames, primaryKey:DbKeyNames, conditions: Map<String, Any?> = emptyMap(), cascadeDeleted: Boolean = false): IOrmMeta

    /**
     * 设置关联关系(has one)
     *
     * 公式：从表.外键 = 主表.主键
     *          外键默认值 = 主表_主键（= 本表_主键）
     *          主键默认值 = 主表的主键（= 本表的主键）
     *
     *    其中本表有一个关联表，因此 本表是主表，关联表是从表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param conditions 关联查询条件
     * @param cascadeDeleted 是否级联删除
     * @return
     */
    fun hasOne(name:String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyNames = this.defaultForeignKey /* 主表_主键 = 本表_主键 */, conditions:Map<String, Any?> = emptyMap(), cascadeDeleted: Boolean = false): IOrmMeta{
        return hasOne(name, relatedModel, foreignKey, this.primaryKey /* 本表的主键 */, conditions, cascadeDeleted)
    }

    /**
     * 设置关联关系(has many)
     *
     * 公式：从表.外键 = 主表.主键
     *          外键默认值 = 主表_主键（= 本表_主键）
     *          主键默认值 = 主表的主键（= 本表的主键）
     *
     *    其中本表有一个关联表，因此 本表是主表，关联表是从表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param conditions 关联查询条件
     * @param cascadeDeleted 是否级联删除
     * @return
     */
    fun hasMany(name: String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyNames, primaryKey:DbKeyNames, conditions: Map<String, Any?> = emptyMap(), cascadeDeleted: Boolean = false): IOrmMeta

    /**
     * 设置关联关系(has many)
     *
     * 公式：从表.外键 = 主表.主键
     *          外键默认值 = 主表_主键（= 本表_主键）
     *          主键默认值 = 主表的主键（= 本表的主键）
     *
     *    其中本表有一个关联表，因此 本表是主表，关联表是从表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param conditions 关联查询条件
     * @param cascadeDeleted 是否级联删除
     * @return
     */
    fun hasMany(name:String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyNames = this.defaultForeignKey /* 主表_主键 = 本表_主键 */, conditions:Map<String, Any?> = emptyMap(), cascadeDeleted: Boolean = false): IOrmMeta{
        return hasMany(name, relatedModel, foreignKey, this.primaryKey /* 本表的主键 */, conditions, cascadeDeleted)
    }

    /**
     * 设置关联关系(has one)
     *
     * 公式：中间表.外键 = 主表.主键
     *      中间表.远端外键 = 远端主表.远端主键
     *           外键默认值 = 主表_主键（= 本表_主键）
     *           主键默认值 = 主表的主键（= 本表的主键）
     *           中间表默认值 = 主表_从表
     *           远端外键默认值 = 远端主表_主键（= 从表_主键）
     *           远端主键默认值 = 远端主表的主键（= 从表的主键）
     *
     *
     *    其中本表有一个关联表，因此 本表是主表，中间表与关联表是从表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param middleTable 中间表
     * @param farForeignKey 远端外键
     * @param farPrimaryKey 远端主键
     * @param conditions 关联查询条件
     * @return
     */
    fun hasOneThrough(name: String,
                      relatedModel: KClass<out IOrm>,
                      foreignKey:DbKeyNames = this.defaultForeignKey, // 主表_主键 = 本表_主键 
                      primaryKey:DbKeyNames = this.primaryKey, // 本表的主键 
                      middleTable:String = table + '_' + relatedModel.modelOrmMeta.table, // 主表_从表 
                      farForeignKey:DbKeyNames = relatedModel.modelOrmMeta.defaultForeignKey, // 远端主表_主键 = 从表_主键 
                      farPrimaryKey:DbKeyNames = relatedModel.modelOrmMeta.primaryKey, // 从表的主键 
                      conditions: Map<String, Any?> = emptyMap()
    ): IOrmMeta

    /**
     * 通过2层关系来设置关联关系(has one)
     *
     * 公式：中间表.外键 = 主表.主键
     *      中间表.远端外键 = 远端主表.远端主键
     *           外键默认值 = 主表_主键（= 本表_主键）
     *           主键默认值 = 主表的主键（= 本表的主键）
     *           中间表默认值 = 主表_从表
     *           远端外键默认值 = 远端主表_主键（= 从表_主键）
     *           远端主键默认值 = 远端主表的主键（= 从表的主键）
     *
     *
     *    其中本表有一个关联表，因此 本表是主表，中间表与关联表是从表
     *
     *    如 hasOneThrough2LevelRelations("reportWorkflowPackage", ReportWorkflowPackage::class, "ReportProcess", "reportWorkflowPackage") // 通过2层关系来关联
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param relationName1 第1层关系
     * @param relationName2 第2层关系
     * @return
     */
    fun hasOneThrough2LevelRelations(name: String,
                      relatedModel: KClass<out IOrm>,
                      relationName1:String, // 第1层关系
                      relationName2:String, // 第2层关系
                      conditions: Map<String, Any?> = emptyMap()
    ): IOrmMeta{
        // 第1层关系
        val relation1 = getRelation(relationName1)
        if(relation1 == null)
            throw IllegalArgumentException("Model [${this.name}] has no relation: $relationName1")

        // 第2层关系
        val meta1 = relation1.model.modelOrmMeta
        val relation2 = meta1.getRelation(relationName2)
        if(relation2 == null)
            throw IllegalArgumentException("Model [${meta1.name}] has no relation: $relationName2")

        // 目标表对中间表是hasOne/hasMany关系的话, 则名称映射相同, 否则相反
        var foreignKey:DbKeyNames // 外键
        var primaryKey:DbKeyNames // 主键
        if(relation1.isBelongsTo){ // 从属于, 名称映射相反
            foreignKey = relation1.primaryKey
            primaryKey = relation1.foreignKey
        }else{ // 有一个
            foreignKey = relation1.foreignKey
            primaryKey = relation1.primaryKey
        }

        var farForeignKey:DbKeyNames // 远端主表_主键 = 从表_主键
        var farPrimaryKey:DbKeyNames // 从表的主键
        if(relation2.isBelongsTo){ // 从属于, 名称映射相同
            farForeignKey = relation2.foreignKey
            farPrimaryKey = relation2.primaryKey
        }else{ // 有一个
            farForeignKey = relation2.primaryKey
            farPrimaryKey = relation2.foreignKey
        }

        return hasOneThrough(name, relatedModel, foreignKey, primaryKey, meta1.table, farForeignKey, farPrimaryKey, conditions)
    }

    /**
     * 设置关联关系(has many)
     *
     * 公式：中间表.外键 = 主表.主键
     *      中间表.远端外键 = 远端主表.远端主键
     *           外键默认值 = 主表_主键（= 本表_主键）
     *           主键默认值 = 主表的主键（= 本表的主键）
     *           中间表默认值 = 主表_从表
     *           远端外键默认值 = 远端主表_主键（= 从表_主键）
     *           远端主键默认值 = 远端主表的主键（= 从表的主键）
     *
     *
     *    其中本表有一个关联表，因此 本表是主表，中间表与关联表是从表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param middleTable 中间表
     * @param farForeignKey 远端外键
     * @param farPrimaryKey 远端主键
     * @param conditions 关联查询条件
     * @return
     */
    fun hasManyThrough(name: String,
                       relatedModel: KClass<out IOrm>,
                       foreignKey:DbKeyNames = this.defaultForeignKey, // 主表_主键 = 本表_主键 
                       primaryKey:DbKeyNames = this.primaryKey, // 本表的主键 
                       middleTable:String = table + '_' + relatedModel.modelOrmMeta.table, // 主表_从表 
                       farForeignKey:DbKeyNames = relatedModel.modelOrmMeta.defaultForeignKey, // 远端主表_主键 = 从表_主键 
                       farPrimaryKey:DbKeyNames = relatedModel.modelOrmMeta.primaryKey, // 从表的主键 
                       conditions: Map<String, Any?> = emptyMap()
    ): IOrmMeta

    /**
     * 通过2层关系来设置关联关系(has many)
     *
     * 公式：中间表.外键 = 主表.主键
     *      中间表.远端外键 = 远端主表.远端主键
     *           外键默认值 = 主表_主键（= 本表_主键）
     *           主键默认值 = 主表的主键（= 本表的主键）
     *           中间表默认值 = 主表_从表
     *           远端外键默认值 = 远端主表_主键（= 从表_主键）
     *           远端主键默认值 = 远端主表的主键（= 从表的主键）
     *
     *
     *    其中本表有一个关联表，因此 本表是主表，中间表与关联表是从表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param relationName1 第1层关系
     * @param relationName2 第2层关系
     * @return
     */
    fun hasManyThrough2LevelRelations(name: String,
                      relatedModel: KClass<out IOrm>,
                      relationName1:String, // 第1层关系
                      relationName2:String, // 第2层关系
                      conditions: Map<String, Any?> = emptyMap()
    ): IOrmMeta{
        // 第1层关系
        val relation1 = getRelation(relationName1)
        if(relation1 == null)
            throw IllegalArgumentException("Model [$name] has no relation: $relationName1")

        // 第2层关系
        val meta1 = relation1.model.modelOrmMeta
        val relation2 = meta1.getRelation(relationName2)
        if(relation2 == null)
            throw IllegalArgumentException("Model [${meta1.name}] has no relation: $relationName2")

        // 目标表对中间表是hasOne/hasMany关系的话, 则名称映射相同, 否则相反

        var foreignKey:DbKeyNames // 外键
        var primaryKey:DbKeyNames // 主键
        if(relation1.isBelongsTo){ // 从属于, 名称映射相反
            foreignKey = relation1.primaryKey
            primaryKey = relation1.foreignKey
        }else{ // 有一个
            foreignKey = relation1.foreignKey
            primaryKey = relation1.primaryKey
        }

        var farForeignKey:DbKeyNames // 远端主表_主键 = 从表_主键
        var farPrimaryKey:DbKeyNames // 从表的主键
        if(relation2.isBelongsTo){ // 从属于, 名称映射相同
            farForeignKey = relation2.foreignKey
            farPrimaryKey = relation2.primaryKey
        }else{ // 有一个
            farForeignKey = relation2.primaryKey
            farPrimaryKey = relation2.foreignKey
        }

        return hasManyThrough(name, relatedModel, foreignKey, primaryKey, meta1.table, farForeignKey, farPrimaryKey, conditions)
    }

    /************************************ 添加关联关系: 单主键版本, 主外键类型为 String *************************************/
    /**
     * 生成属性代理 + 设置关联关系(belongs to)
     *
     *    公式：从表.外键 = 主表.主键
     *             外键默认值 = 主表_主键 （= 关联表_主键）
     *             主键默认值 = 主表的主键 （= 关联表的主键）
     *
     *    其中本表从属于关联表，因此 本表是从表，关联表是主表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param conditions 关联查询条件
     * @return
     */
    fun belongsTo(name:String, relatedModel: KClass<out IOrm>, foreignKey:String, primaryKey:String, conditions:Map<String, Any?> = emptyMap()): IOrmMeta{
        return  belongsTo(name, relatedModel, DbKeyNames(foreignKey), DbKeyNames(primaryKey), conditions)
    }

    /**
     * 生成属性代理 + 设置关联关系(belongs to)
     *
     *    公式：从表.外键 = 主表.主键
     *             外键默认值 = 主表_主键 （= 关联表_主键）
     *             主键默认值 = 主表的主键 （= 关联表的主键）
     *
     *    其中本表从属于关联表，因此 本表是从表，关联表是主表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param conditions 关联查询条件
     * @return
     */
    fun belongsTo(name:String, relatedModel: KClass<out IOrm>, foreignKey:String, conditions:Map<String, Any?> = emptyMap()): IOrmMeta{
        return belongsTo(name, relatedModel, foreignKey, relatedModel.modelOrmMeta.primaryKey.first() /* 关联表的主键 */, conditions)
    }

    /**
     * 设置关联关系(has one)
     *
     * 公式：从表.外键 = 主表.主键
     *          外键默认值 = 主表_主键（= 本表_主键）
     *          主键默认值 = 主表的主键（= 本表的主键）
     *
     *    其中本表有一个关联表，因此 本表是主表，关联表是从表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param conditions 关联查询条件
     * @param cascadeDeleted 是否级联删除
     * @return
     */
    fun hasOne(name: String, relatedModel: KClass<out IOrm>, foreignKey:String, primaryKey:String, conditions: Map<String, Any?> = emptyMap(), cascadeDeleted: Boolean = false): IOrmMeta{
        return hasOne(name, relatedModel, DbKeyNames(foreignKey), DbKeyNames(primaryKey), conditions, cascadeDeleted)
    }

    /**
     * 设置关联关系(has one)
     *
     * 公式：从表.外键 = 主表.主键
     *          外键默认值 = 主表_主键（= 本表_主键）
     *          主键默认值 = 主表的主键（= 本表的主键）
     *
     *    其中本表有一个关联表，因此 本表是主表，关联表是从表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param conditions 关联查询条件
     * @param cascadeDeleted 是否级联删除
     * @return
     */
    fun hasOne(name:String, relatedModel: KClass<out IOrm>, foreignKey:String, conditions:Map<String, Any?> = emptyMap(), cascadeDeleted: Boolean = false): IOrmMeta{
        return hasOne(name, relatedModel, foreignKey, this.primaryKey.first() /* 本表的主键 */, conditions, cascadeDeleted)
    }

    /**
     * 设置关联关系(has many)
     *
     * 公式：从表.外键 = 主表.主键
     *          外键默认值 = 主表_主键（= 本表_主键）
     *          主键默认值 = 主表的主键（= 本表的主键）
     *
     *    其中本表有一个关联表，因此 本表是主表，关联表是从表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param conditions 关联查询条件
     * @param cascadeDeleted 是否级联删除
     * @return
     */
    fun hasMany(name: String, relatedModel: KClass<out IOrm>, foreignKey:String, primaryKey:String, conditions: Map<String, Any?> = emptyMap(), cascadeDeleted: Boolean = false): IOrmMeta{
        return hasMany(name, relatedModel, DbKeyNames(foreignKey), DbKeyNames(primaryKey), conditions, cascadeDeleted)
    }

    /**
     * 设置关联关系(has many)
     *
     * 公式：从表.外键 = 主表.主键
     *          外键默认值 = 主表_主键（= 本表_主键）
     *          主键默认值 = 主表的主键（= 本表的主键）
     *
     *    其中本表有一个关联表，因此 本表是主表，关联表是从表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param conditions 关联查询条件
     * @param cascadeDeleted 是否级联删除
     * @return
     */
    fun hasMany(name:String, relatedModel: KClass<out IOrm>, foreignKey:String, conditions:Map<String, Any?> = emptyMap(), cascadeDeleted: Boolean = false): IOrmMeta{
        return hasMany(name, relatedModel, foreignKey, this.primaryKey.first() /* 本表的主键 */, conditions, cascadeDeleted)
    }

    /**
     * 设置关联关系(has one)
     *
     * 公式：中间表.外键 = 主表.主键
     *      中间表.远端外键 = 远端主表.远端主键
     *           外键默认值 = 主表_主键（= 本表_主键）
     *           主键默认值 = 主表的主键（= 本表的主键）
     *           中间表默认值 = 主表_从表
     *           远端外键默认值 = 远端主表_主键（= 从表_主键）
     *           远端主键默认值 = 远端主表的主键（= 从表的主键）
     *
     *
     *    其中本表有一个关联表，因此 本表是主表，中间表与关联表是从表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param middleTable 中间表
     * @param farForeignKey 远端外键
     * @param farPrimaryKey 远端主键
     * @return
     */
    fun hasOneThrough(name: String,
                      relatedModel: KClass<out IOrm>,
                      foreignKey:String = this.defaultForeignKey.first(), // 主表_主键 = 本表_主键 
                      primaryKey:String = this.primaryKey.first(), // 本表的主键 
                      middleTable:String = table + '_' + relatedModel.modelOrmMeta.table, // 主表_从表 
                      farForeignKey:String = relatedModel.modelOrmMeta.defaultForeignKey.first(), // 远端主表_主键 = 从表_主键 
                      farPrimaryKey:String = relatedModel.modelOrmMeta.primaryKey.first(), // 从表的主键 
                      conditions: Map<String, Any?> = emptyMap()
    ): IOrmMeta{
        return hasOneThrough(name, relatedModel, DbKeyNames(foreignKey), DbKeyNames(primaryKey), middleTable, DbKeyNames(farForeignKey), DbKeyNames(farPrimaryKey), conditions)
    }

    /**
     * 设置关联关系(has many)
     *
     * 公式：中间表.外键 = 主表.主键
     *      中间表.远端外键 = 远端主表.远端主键
     *           外键默认值 = 主表_主键（= 本表_主键）
     *           主键默认值 = 主表的主键（= 本表的主键）
     *           中间表默认值 = 主表_从表
     *           远端外键默认值 = 远端主表_主键（= 从表_主键）
     *           远端主键默认值 = 远端主表的主键（= 从表的主键）
     *
     *
     *    其中本表有一个关联表，因此 本表是主表，中间表与关联表是从表
     *
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param middleTable 中间表
     * @param farForeignKey 远端外键
     * @param farPrimaryKey 远端主键
     * @return
     */
    fun hasManyThrough(name: String,
                       relatedModel: KClass<out IOrm>,
                       foreignKey:String = this.defaultForeignKey.first(), // 主表_主键 = 本表_主键 
                       primaryKey:String = this.primaryKey.first(), // 本表的主键 
                       middleTable:String = table + '_' + relatedModel.modelOrmMeta.table, // 主表_从表 
                       farForeignKey:String = relatedModel.modelOrmMeta.defaultForeignKey.first(), // 远端主表_主键 = 从表_主键 
                       farPrimaryKey:String = relatedModel.modelOrmMeta.primaryKey.first(), // 从表的主键 
                       conditions: Map<String, Any?> = emptyMap()
    ): IOrmMeta{
        return hasManyThrough(name, relatedModel, DbKeyNames(foreignKey), DbKeyNames(primaryKey), middleTable, DbKeyNames(farForeignKey), DbKeyNames(farPrimaryKey), conditions)
    }

    /********************************* 通过回调动态获得对象的关联关系 **************************************/
    /**
     * 是否有某个回调的关联关系
     * @param name
     * @return
     */
    fun hasCbRelation(name: String): Boolean

    /**
     * 获得某个回调的关联关系
     * @param name
     * @return
     */
    fun getCbRelation(name: String): ICbRelation<out IOrm, *, *>?

    /**
     * 设置通过回调动态获得对象的关联关系(has one)
     * @param name 字段名
     * @param pkGetter 当前模型的主键的getter
     * @param fkGetter 关联对象的外键的getter
     * @param relatedSupplier 批量获取关联对象的回调
     * @return
     */
    fun <M:IOrm, K, R> cbHasOne(name: String, pkGetter: (M)->K, fkGetter: (R)->K, relatedSupplier:(List<K>) -> List<R>): IOrmMeta

    /**
     * 设置通过回调动态获得对象的关联关系(has many)
     * @param name 字段名
     * @param pkGetter 当前模型的主键的getter
     * @param fkGetter 关联对象的外键的getter
     * @param relatedSupplier 批量获取关联对象的回调
     * @return
     */
    fun <M:IOrm, K, R> cbHasMany(name: String, pkGetter: (M)->K, fkGetter: (R)->K, relatedSupplier:(List<K>) -> List<R>): IOrmMeta

    /********************************* xstream **************************************/
    /**
     * 初始化xstream, 入口
     * @param modelNameAsAlias 模型名作为别名
     * @return
     */
    fun initXStream(modelNameAsAlias: Boolean = true): XStream
}
