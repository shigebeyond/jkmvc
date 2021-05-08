package net.jkcode.jkmvc.orm

import com.thoughtworks.xstream.XStream
import net.jkcode.jkmvc.db.DbColumn
import net.jkcode.jkmvc.db.DbTable
import net.jkcode.jkmvc.db.IDb
import net.jkcode.jkmvc.orm.relation.ICbRelation
import net.jkcode.jkmvc.orm.relation.IRelation
import net.jkcode.jkutil.collection.FixedKeyMapFactory
import net.jkcode.jkutil.validator.IValidator
import net.jkcode.jkutil.validator.ModelValidateResult
import net.jkcode.jkutil.validator.RuleValidator
import net.jkcode.jkutil.validator.ValidateLambda
import kotlin.reflect.KClass

/**
 * orm的元数据
 *   扩展IOrmMeta, 兼容java调用:
 *   1. KClass 参数 要改为 Class
 *   2. 要使用 @JvmOverloads 来使得有参数默认值的函数重载
 *   但问题是 IOrmMeta 是接口, 而 @JvmOverloads 不能用于接口方法
 *
 */
public abstract class IJavaOrmMeta : IOrmMeta  {

    /************************************ query builder *************************************/
    /**
     * 获得orm查询构建器
     *    子类要重写该方法, 只是因为@JvmOverloads不能用在抽象方法中, 才给了一个默认的实现
     *
     * @param convertingValue 查询时是否智能转换字段值
     * @param convertingColumn 查询时是否智能转换字段名
     * @param withSelect with()联查时自动select关联表的字段
     * @param reused 是否复用的
     * @return
     */
    @JvmOverloads
    open fun queryBuilder(convertingValue: Boolean = false, convertingColumn: Boolean = false, withSelect: Boolean = true, reused: Boolean = false): OrmQueryBuilder{
        throw UnsupportedOperationException("子类要重写该方法, 只是因为@JvmOverloads不能用在抽象方法中, 才给了一个默认的实现")
    }

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
    @JvmOverloads
    fun queryBuilder(sort: String?, desc: Boolean? = null, start: Int? = null, rows: Int? = null): OrmQueryBuilder {
        val query = queryBuilder()

        if (sort != null && sort != "")
            query.orderBy(sort, desc)

        if (rows != null && rows > 0)
            query.limit(rows, start ?: 0)

        return query
    }

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
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun belongsTo(name:String, relatedModel: Class<out IOrm>, foreignKey:DbKeyNames, primaryKey:DbKeyNames, conditions:Map<String, Any?> = emptyMap(), queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null): IOrmMeta{
        return belongsTo(name, relatedModel.kotlin, foreignKey, primaryKey, conditions, queryAction)
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
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun belongsTo(name:String, relatedModel: Class<out IOrm>, foreignKey:DbKeyNames = relatedModel.kotlin.modelOrmMeta.defaultForeignKey /* 主表_主键 = 关联表_主键 */, conditions:Map<String, Any?> = emptyMap(), queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null): IOrmMeta{
        return belongsTo(name, relatedModel.kotlin, foreignKey, relatedModel.kotlin.modelOrmMeta.primaryKey /* 关联表的主键 */, conditions, queryAction)
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
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun hasOne(name: String, relatedModel: Class<out IOrm>, foreignKey:DbKeyNames, primaryKey:DbKeyNames, conditions: Map<String, Any?> = emptyMap(), cascadeDeleted: Boolean = false, queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null): IOrmMeta{
        return hasOne(name, relatedModel.kotlin, foreignKey, primaryKey, conditions, cascadeDeleted, queryAction)
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
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun hasOne(name:String, relatedModel: Class<out IOrm>, foreignKey:DbKeyNames = this.defaultForeignKey /* 主表_主键 = 本表_主键 */, conditions:Map<String, Any?> = emptyMap(), cascadeDeleted: Boolean = false, queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null): IOrmMeta{
        return hasOne(name, relatedModel.kotlin, foreignKey, this.primaryKey /* 本表的主键 */, conditions, cascadeDeleted, queryAction)
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
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun hasMany(name: String, relatedModel: Class<out IOrm>, foreignKey:DbKeyNames, primaryKey:DbKeyNames, conditions: Map<String, Any?> = emptyMap(), cascadeDeleted: Boolean = false, queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null): IOrmMeta{
        return hasMany(name, relatedModel.kotlin, foreignKey, primaryKey, conditions, cascadeDeleted, queryAction)
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
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun hasMany(name:String, relatedModel: Class<out IOrm>, foreignKey:DbKeyNames = this.defaultForeignKey /* 主表_主键 = 本表_主键 */, conditions:Map<String, Any?> = emptyMap(), cascadeDeleted: Boolean = false, queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null): IOrmMeta{
        return hasMany(name, relatedModel.kotlin, foreignKey, this.primaryKey /* 本表的主键 */, conditions, cascadeDeleted, queryAction)
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
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun hasOneThrough(name: String,
                      relatedModel: Class<out IOrm>,
                      foreignKey:DbKeyNames = this.defaultForeignKey, // 主表_主键 = 本表_主键 
                      primaryKey:DbKeyNames = this.primaryKey, // 本表的主键 
                      middleTable:String = table + '_' + relatedModel.kotlin.modelOrmMeta.table, // 主表_从表 
                      farForeignKey:DbKeyNames = relatedModel.kotlin.modelOrmMeta.defaultForeignKey, // 远端主表_主键 = 从表_主键 
                      farPrimaryKey:DbKeyNames = relatedModel.kotlin.modelOrmMeta.primaryKey, // 从表的主键 
                      conditions: Map<String, Any?> = emptyMap(),
                      queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null
    ): IOrmMeta{
        return hasOneThrough(name, relatedModel.kotlin, foreignKey, primaryKey, middleTable, farForeignKey, farPrimaryKey, conditions, queryAction)
    }

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
     * @param conditions 关联查询条件
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun hasOneThrough2LevelRelations(name: String,
                                     relatedModel: Class<out IOrm>,
                                     relationName1:String, // 第1层关系
                                     relationName2:String, // 第2层关系
                                     conditions: Map<String, Any?> = emptyMap(),
                                     queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null
    ): IOrmMeta{
        return hasOneThrough2LevelRelations(name, relatedModel.kotlin, relationName1, relationName2, conditions, queryAction)
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
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun hasManyThrough(name: String,
                       relatedModel: Class<out IOrm>,
                       foreignKey:DbKeyNames = this.defaultForeignKey, // 主表_主键 = 本表_主键 
                       primaryKey:DbKeyNames = this.primaryKey, // 本表的主键 
                       middleTable:String = table + '_' + relatedModel.kotlin.modelOrmMeta.table, // 主表_从表 
                       farForeignKey:DbKeyNames = relatedModel.kotlin.modelOrmMeta.defaultForeignKey, // 远端主表_主键 = 从表_主键 
                       farPrimaryKey:DbKeyNames = relatedModel.kotlin.modelOrmMeta.primaryKey, // 从表的主键 
                       conditions: Map<String, Any?> = emptyMap(),
                       queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null
    ): IOrmMeta{
        return hasManyThrough(name, relatedModel.kotlin, foreignKey, primaryKey, middleTable, farForeignKey, farPrimaryKey, conditions, queryAction)
    }

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
     * @param conditions 关联查询条件
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun hasManyThrough2LevelRelations(name: String,
                                      relatedModel: Class<out IOrm>,
                                      relationName1:String, // 第1层关系
                                      relationName2:String, // 第2层关系
                                      conditions: Map<String, Any?> = emptyMap(),
                                      queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null
    ): IOrmMeta{
        return hasManyThrough2LevelRelations(name, relatedModel.kotlin, relationName1, relationName2, conditions, queryAction)
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
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun belongsTo(name:String, relatedModel: Class<out IOrm>, foreignKey:String, primaryKey:String, conditions:Map<String, Any?> = emptyMap(), queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null): IOrmMeta{
        return belongsTo(name, relatedModel.kotlin, DbKeyNames(foreignKey), DbKeyNames(primaryKey), conditions, queryAction)
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
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun belongsTo(name:String, relatedModel: Class<out IOrm>, foreignKey:String, conditions:Map<String, Any?> = emptyMap(), queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null): IOrmMeta{
        return belongsTo(name, relatedModel.kotlin, foreignKey, relatedModel.kotlin.modelOrmMeta.primaryKey.first() /* 关联表的主键 */, conditions, queryAction)
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
     * @param cascadeDeleted
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun hasOne(name: String, relatedModel: Class<out IOrm>, foreignKey:String, primaryKey:String, conditions: Map<String, Any?> = emptyMap(), cascadeDeleted: Boolean = false, queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null): IOrmMeta{
        return hasOne(name, relatedModel.kotlin, DbKeyNames(foreignKey), DbKeyNames(primaryKey), conditions, cascadeDeleted, queryAction)
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
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun hasOne(name:String, relatedModel: Class<out IOrm>, foreignKey:String, conditions:Map<String, Any?> = emptyMap(), cascadeDeleted: Boolean = false, queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null): IOrmMeta{
        return hasOne(name, relatedModel.kotlin, foreignKey, this.primaryKey.first() /* 本表的主键 */, conditions, cascadeDeleted, queryAction)
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
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun hasMany(name: String, relatedModel: Class<out IOrm>, foreignKey:String, primaryKey:String, conditions: Map<String, Any?> = emptyMap(), cascadeDeleted: Boolean = false, queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null): IOrmMeta{
        return hasMany(name, relatedModel.kotlin, DbKeyNames(foreignKey), DbKeyNames(primaryKey), conditions, cascadeDeleted, queryAction)
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
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun hasMany(name:String, relatedModel: Class<out IOrm>, foreignKey:String, conditions:Map<String, Any?> = emptyMap(), cascadeDeleted: Boolean = false, queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null): IOrmMeta{
        return hasMany(name, relatedModel.kotlin, foreignKey, this.primaryKey.first() /* 本表的主键 */, conditions, cascadeDeleted, queryAction)
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
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun hasOneThrough(name: String,
                      relatedModel: Class<out IOrm>,
                      foreignKey:String, // 主表_主键 = 本表_主键
                      primaryKey:String = this.primaryKey.first(), // 本表的主键
                      middleTable:String = table + '_' + relatedModel.kotlin.modelOrmMeta.table, // 主表_从表
                      farForeignKey:String = relatedModel.kotlin.modelOrmMeta.defaultForeignKey.first(), // 远端主表_主键 = 从表_主键
                      farPrimaryKey:String = relatedModel.kotlin.modelOrmMeta.primaryKey.first(), // 从表的主键
                      conditions: Map<String, Any?> = emptyMap(),
                      queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null
    ): IOrmMeta{
        return hasOneThrough(name, relatedModel.kotlin, DbKeyNames(foreignKey), DbKeyNames(primaryKey), middleTable, DbKeyNames(farForeignKey), DbKeyNames(farPrimaryKey), conditions, queryAction)
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
     * @param queryAction 查询对象的回调函数
     * @return
     */
    @JvmOverloads
    fun hasManyThrough(name: String,
                       relatedModel: Class<out IOrm>,
                       foreignKey:String, // 主表_主键 = 本表_主键
                       primaryKey:String = this.primaryKey.first(), // 本表的主键 
                       middleTable:String = table + '_' + relatedModel.kotlin.modelOrmMeta.table, // 主表_从表 
                       farForeignKey:String = relatedModel.kotlin.modelOrmMeta.defaultForeignKey.first(), // 远端主表_主键 = 从表_主键 
                       farPrimaryKey:String = relatedModel.kotlin.modelOrmMeta.primaryKey.first(), // 从表的主键 
                       conditions: Map<String, Any?> = emptyMap(),
                       queryAction: ((OrmQueryBuilder, Boolean)->Unit)? = null
    ): IOrmMeta{
        return hasManyThrough(name, relatedModel.kotlin, DbKeyNames(foreignKey), DbKeyNames(primaryKey), middleTable, DbKeyNames(farForeignKey), DbKeyNames(farPrimaryKey), conditions, queryAction)
    }

}
