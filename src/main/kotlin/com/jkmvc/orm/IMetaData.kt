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
     */
    fun hasRelation(name:String):Boolean;

    /**
     * 获得某个关联关系
     */
    fun getRelation(name:String):IMetaRelation?;

    /**
     * 获得orm查询构建器
     */
    fun queryBuilder(): OrmQueryBuilder;

    /**
     * 添加规则
     */
    fun addRule(name: String, rule: String): MetaData;

    /**
     * 添加标签
     */
    fun addLabel(name: String, label: String): MetaData;

    /**
     * 生成属性代理 + 设置关联关系(belongs to)
     */
    fun belongsTo(name:String, relatedModel: KClass<out IOrm>, foreignKey:String = "", conditions:((IDbQueryBuilder) -> Unit)? = null): IMetaData;

    /**
     * 设置关联关系(has one)
     */
    fun hasOne(name:String, relatedModel: KClass<out IOrm>, foreignKey:String = "", conditions:((IDbQueryBuilder) -> Unit)? = null): IMetaData;

    /**
     * 设置关联关系(has many)
     */
    fun hasMany(name:String, relatedModel: KClass<out IOrm>, foreignKey:String = "", conditions:((IDbQueryBuilder) -> Unit)? = null): IMetaData;

}