package com.jkmvc.orm

import com.jkmvc.db.Db
import com.jkmvc.db.IDbQueryBuilder
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

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
    val relations:ConcurrentHashMap<String, MetaRelation>

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
    fun getRelation(name:String):MetaRelation?;

    /**
     * 获得orm查询构建器
     */
    fun queryBuilder(): OrmQueryBuilder;

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
