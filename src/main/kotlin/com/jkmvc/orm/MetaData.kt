package com.jkmvc.orm

import com.jkmvc.db.Db
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * orm的元数据
 * 　两重职责
 *  1 模型映射表的映射元数据，如模型类/数据库/表名
 *  2 代理模型的属性读写
 */
open class MetaData(public override val clazz: KClass<*> /* 模型类 */, public override val dbName:String = "database" /* 数据库名 */, public override var table:String = "" /* 表名 */, public override val primaryKey:String = "id" /* 主键 */, public final override val relations:Map<String, MetaRelation>? = null /* 关联关系 */ ):IMetaData{

    init {
        // 假定model类名, 都是以"Model"作为后缀
        if(table == "")
            table = clazz.simpleName!!.removeSuffix("Model").toLowerCase();

    }

    /**
     * 数据库
     */
    public override val db:Db
        get() = Db.getDb(dbName)

    /**
     * 表字段
     */
    public override val columns:List<String>
        get() = db.listColumns(table)

    /**
     * 是否有某个关联关系
     */
    public override fun hasRelation(name:String):Boolean{
        return relations != null && relations.containsKey(name);

    }

    /**
     * 获得某个关联关系
     */
    public override fun getRelation(name:String):MetaRelation?{
        return relations?.get(name);
    }

    /**
     * 获得orm查询构建器
     */
    public override fun queryBuilder(): OrmQueryBuilder {
        return OrmQueryBuilder(this);
    }

    /**
     * 获得属性代理
     *   代理模型的属性读写
     */
    public override fun <T> property(): ReadWriteProperty<IOrm, T>{
        return object : ReadWriteProperty<IOrm, T> {
            /**
             * 获得属性
             */
            public override operator fun getValue(thisRef: IOrm, property: KProperty<*>): T {
                return thisRef[property.name]
            }

            /**
             * 设置属性
             */
            public override operator fun setValue(thisRef: IOrm, property: KProperty<*>, value: T) {
                thisRef[property.name] = value
            }
        }
    }
}

