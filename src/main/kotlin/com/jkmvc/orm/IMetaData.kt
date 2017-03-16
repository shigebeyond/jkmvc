package com.jkmvc.orm

import com.jkmvc.db.Db
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass

/**
 * orm的元数据
 * 　两重职责
 *  1 模型映射表的映射元数据，如模型类/数据库/表名
 *  2 代理模型的属性读写
 */
interface IMetaData{

    /**
     * 模型类
     */
    public val clazz: KClass<*>

    /**
     * 数据库名
     */
    public val dbName:String

    /**
     * 表名
     */
    public var table:String

    /**
     * 主键
     */
    public val primaryKey:String

    /**
     * 关联关系
     */
    public val relations:Map<String, MetaRelation>?

    /**
     * 数据库
     */
    public val db:Db

    /**
     * 表字段
     */
    public val columns:List<String>

    /**
     * 是否有某个关联关系
     */
    public fun hasRelation(name:String):Boolean;

    /**
     * 获得某个关联关系
     */
    public fun getRelation(name:String):MetaRelation?;

    /**
     * 获得orm查询构建器
     */
    public fun queryBuilder(): OrmQueryBuilder;

    /**
     * 获得属性代理
     *   代理模型的属性读写
     */
    public fun <T> property(): ReadWriteProperty<IOrm, T>;

}
