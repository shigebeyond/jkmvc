package com.jkmvc.orm

import com.jkmvc.db.Db
import kotlin.reflect.KClass

/**
 * orm的元数据
 */
open class MetaData(public val clazz: KClass<*> /* 模型类 */, public val dbName:String = "database" /* 数据库名 */, public var table:String = "" /* 表名 */, public val primaryKey:String = "id" /* 主键 */, public val relations:Map<String, MetaRelation>? = null /* 关联关系 */ ){
    init {
        // 假定model类名, 都是以"Model"作为后缀
        if(table == "")
            table = clazz.simpleName!!.removeSuffix("Model");

    }

    /**
     * 数据库
     */
    public val db:Db
        get() = Db.getDb(dbName)

    /**
     * 表字段
     */
    public val columns:List<String>
        get() = db.listColumns(table)

    /**
     * 是否有某个关联关系
     */
    public fun hasRelation(name:String):Boolean{
        return if(relations == null)
                    false
                else
                    relations.containsKey(name);
    }

    /**
     * 获得某个关联关系
     */
    public fun getRelation(name:String):MetaRelation?{
        return relations?.get(name);
    }

    /**
     * 获得orm查询构建器
     */
    public fun queryBuilder(): OrmQueryBuilder {
        return OrmQueryBuilder(this);
    }

}
