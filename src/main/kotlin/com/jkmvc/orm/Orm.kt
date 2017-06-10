package com.jkmvc.orm

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

/**
 * ORM

 * @Package package_name
 * *
 * @category
 * *
 * @author shijianhang
 * *
 * @date 2016-10-10 上午12:52:34
 */
abstract class Orm(id:Int? = null): OrmRelated() {

    companion object{
        /**
         * 缓存id查询
         */
        protected val idQueries: ConcurrentHashMap<Class<*>, OrmQueryBuilder> = ConcurrentHashMap()
    }

    init{
        if(id != null){
            val query = idQueries.getOrPut(this.javaClass){
                queryBuilder().prepare() /* 预编译sql */.where(metadata.primaryKey, id) as OrmQueryBuilder
            }
            query.find(){
                this.original(it);
            }
        }
    }

    public override fun toString(): String {
        return "${this.javaClass}: " + data.toString()
    }
}
