package com.jkmvc.orm

import com.jkmvc.db.Db
import com.jkmvc.db.IDbQueryBuilder
import kotlin.reflect.KClass

/**
 * orm的元数据
 * 　模型映射表的映射元数据，如模型类/数据库/表名
 */
interface IMetaData{

    /**
     * 模型类
     */
    val model: KClass<out IOrm>

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
    val primaryKey:String

    /**
     * 默认外键
     */
    val defaultForeignKey:String

    /**
     * 关联关系
     */
    val relations: MutableMap<String, IMetaRelation>

    /**
     * 每个字段的规则
     */
    val rules: MutableMap<String, String>

    /**
     * 每个字段的标签
     */
    val labels: MutableMap<String, String>

    /**
     * 数据库
     */
    val db:Db

    /**
     * 表字段
     */
    val columns:List<String>

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
    fun getRelation(name:String):IMetaRelation?;

    /**
     * 获得orm查询构建器
     * @return
     */
    fun queryBuilder(): OrmQueryBuilder;

    /**
     * 添加规则
     * @param name
     * @param rule
     * @return
     */
    fun addRule(name: String, rule: String): MetaData;

    /**
     * 添加标签
     * @param name
     * @param label
     * @return
     */
    fun addLabel(name: String, label: String): MetaData;

    /**
     * 生成属性代理 + 设置关联关系(belongs to)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param conditions 关联查询条件
     * @return
     */
    fun belongsTo(name:String, relatedModel: KClass<out IOrm>, foreignKey:String = "", conditions:((IDbQueryBuilder) -> Unit)? = null): IMetaData;

    /**
     * 设置关联关系(has one)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param conditions 关联查询条件
     */
    fun hasOne(name:String, relatedModel: KClass<out IOrm>, foreignKey:String = "", conditions:((IDbQueryBuilder) -> Unit)? = null): IMetaData;

    /**
     * 设置关联关系(has many)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param conditions 关联查询条件
     */
    fun hasMany(name:String, relatedModel: KClass<out IOrm>, foreignKey:String = "", conditions:((IDbQueryBuilder) -> Unit)? = null): IMetaData;

    /**
     * 获得上传目录
     * @return string
     */
    fun prepareUploadDir(): String
}
