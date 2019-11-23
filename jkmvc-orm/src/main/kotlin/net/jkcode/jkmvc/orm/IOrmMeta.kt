package net.jkcode.jkmvc.orm

import net.jkcode.jkutil.common.FixedKeyMapFactory
import net.jkcode.jkmvc.db.IDb
import net.jkcode.jkutil.validator.IValidator
import net.jkcode.jkutil.validator.RuleValidator
import net.jkcode.jkutil.validator.ValidateLambda
import kotlin.reflect.KClass

/**
 * orm的元数据
 * 　模型映射表的映射元数据，如模型类/数据库/表名
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
    val relations: MutableMap<String, IRelationMeta>

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
     * 表字段
     */
    val columns:List<String>

    /**
     * 对象属性
     */
    val props: List<String>

    /**
     * 要序列化的对象属性
     *   写时序列化, 读时反序列化
     */
    val serializingProps: List<String>

    /**
     * 默认要设置的字段名
     */
    val defaultExpectedProps: List<String>

    /**
     * 数据的工厂
     */
    val dataFactory: FixedKeyMapFactory

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

    /************************************ 事件 *************************************/
    /**
     * 校验orm对象数据
     * @param item
     * @return
     */
    fun validate(item: IOrmEntity): Boolean

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
     * @param name 关联关系名
     * @param select 是否select关联字段
     * @param columns 关联字段列表
     * @param lastName 上一级关系名
     * @param path 列名父路径
     * @return 关联关系
     */
    fun joinRelated(query: OrmQueryBuilder, name: String, select: Boolean, columns: SelectColumnList?, lastName:String = this.name, path:String = ""): IRelationMeta


    /************************************ 事件 *************************************/
    /**
     * 能处理的事件
     */
    val processableEvents: List<String>

    /**
     * 能否处理任一事件
     * @param events 多个事件名，以|分隔，如 beforeCreate|afterCreate
     * @return
     */
    fun canHandleAnyEvent(events:String): Boolean{
        return processableEvents.any { event ->
            events.contains(event)
        }
    }

    /**
     * 如果有要处理的事件，则开启事务
     *
     * @param events 多个事件名，以|分隔，如 beforeCreate|afterCreate
     * @param statement
     * @return
     */
    fun <T> transactionWhenHandlingEvent(events:String, statement: () -> T): T

    /************************************ 关联关系 *************************************/
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
    fun getRelation(name:String): IRelationMeta?;

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
     * @return
     */
    fun hasOne(name: String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyNames, primaryKey:DbKeyNames, conditions: Map<String, Any?> = emptyMap()): IOrmMeta

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
     * @return
     */
    fun hasOne(name:String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyNames = this.defaultForeignKey /* 主表_主键 = 本表_主键 */, conditions:Map<String, Any?> = emptyMap()): IOrmMeta{
        return hasOne(name, relatedModel, foreignKey, this.primaryKey /* 本表的主键 */, conditions)
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
     * @return
     */
    fun hasMany(name: String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyNames, primaryKey:DbKeyNames, conditions: Map<String, Any?> = emptyMap()): IOrmMeta

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
     * @return
     */
    fun hasMany(name:String, relatedModel: KClass<out IOrm>, foreignKey:DbKeyNames = this.defaultForeignKey /* 主表_主键 = 本表_主键 */, conditions:Map<String, Any?> = emptyMap()): IOrmMeta{
        return hasMany(name, relatedModel, foreignKey, this.primaryKey /* 本表的主键 */, conditions)
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
                      foreignKey:DbKeyNames = this.defaultForeignKey /* 主表_主键 = 本表_主键 */,
                      primaryKey:DbKeyNames = this.primaryKey /* 本表的主键 */,
                      middleTable:String = table + '_' + relatedModel.modelOrmMeta.table /* 主表_从表 */,
                      farForeignKey:DbKeyNames = relatedModel.modelOrmMeta.defaultForeignKey /* 远端主表_主键 = 从表_主键 */,
                      farPrimaryKey:DbKeyNames = relatedModel.modelOrmMeta.primaryKey /* 从表的主键 */,
                      conditions: Map<String, Any?> = emptyMap()
    ): IOrmMeta

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
                       foreignKey:DbKeyNames = this.defaultForeignKey /* 主表_主键 = 本表_主键 */,
                       primaryKey:DbKeyNames = this.primaryKey /* 本表的主键 */,
                       middleTable:String = table + '_' + relatedModel.modelOrmMeta.table /* 主表_从表 */,
                       farForeignKey:DbKeyNames = relatedModel.modelOrmMeta.defaultForeignKey /* 远端主表_主键 = 从表_主键 */,
                       farPrimaryKey:DbKeyNames = relatedModel.modelOrmMeta.primaryKey /* 从表的主键 */,
                       conditions: Map<String, Any?> = emptyMap()
    ): IOrmMeta

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
     * @return
     */
    fun hasOne(name: String, relatedModel: KClass<out IOrm>, foreignKey:String, primaryKey:String, conditions: Map<String, Any?> = emptyMap()): IOrmMeta{
        return hasOne(name, relatedModel, DbKeyNames(foreignKey), DbKeyNames(primaryKey), conditions)
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
     * @return
     */
    fun hasOne(name:String, relatedModel: KClass<out IOrm>, foreignKey:String, conditions:Map<String, Any?> = emptyMap()): IOrmMeta{
        return hasOne(name, relatedModel, foreignKey, this.primaryKey.first() /* 本表的主键 */, conditions)
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
     * @return
     */
    fun hasMany(name: String, relatedModel: KClass<out IOrm>, foreignKey:String, primaryKey:String, conditions: Map<String, Any?> = emptyMap()): IOrmMeta{
        return hasMany(name, relatedModel, DbKeyNames(foreignKey), DbKeyNames(primaryKey), conditions)
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
     * @return
     */
    fun hasMany(name:String, relatedModel: KClass<out IOrm>, foreignKey:String, conditions:Map<String, Any?> = emptyMap()): IOrmMeta{
        return hasMany(name, relatedModel, foreignKey, this.primaryKey.first() /* 本表的主键 */, conditions)
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
                      foreignKey:String = this.defaultForeignKey.first() /* 主表_主键 = 本表_主键 */,
                      primaryKey:String = this.primaryKey.first() /* 本表的主键 */,
                      middleTable:String = table + '_' + relatedModel.modelOrmMeta.table /* 主表_从表 */,
                      farForeignKey:String = relatedModel.modelOrmMeta.defaultForeignKey.first() /* 远端主表_主键 = 从表_主键 */,
                      farPrimaryKey:String = relatedModel.modelOrmMeta.primaryKey.first() /* 从表的主键 */,
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
                       foreignKey:String = this.defaultForeignKey.first() /* 主表_主键 = 本表_主键 */,
                       primaryKey:String = this.primaryKey.first() /* 本表的主键 */,
                       middleTable:String = table + '_' + relatedModel.modelOrmMeta.table /* 主表_从表 */,
                       farForeignKey:String = relatedModel.modelOrmMeta.defaultForeignKey.first() /* 远端主表_主键 = 从表_主键 */,
                       farPrimaryKey:String = relatedModel.modelOrmMeta.primaryKey.first() /* 从表的主键 */,
                       conditions: Map<String, Any?> = emptyMap()
    ): IOrmMeta{
        return hasManyThrough(name, relatedModel, DbKeyNames(foreignKey), DbKeyNames(primaryKey), middleTable, DbKeyNames(farForeignKey), DbKeyNames(farPrimaryKey), conditions)
    }
}
