package com.jkmvc.orm

import com.jkmvc.db.CompiledSql
import java.util.concurrent.ConcurrentHashMap

/**
 * ORM

 * @Package package_name
 * @category
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 */
abstract class Orm(id:Int? = null): OrmRelated() {

    companion object{
        /**
         * 缓存根据主键查询的sql
         */
        protected val pkSqls: ConcurrentHashMap<Class<*>, CompiledSql> = ConcurrentHashMap()
    }

    init{
        if(id != null){
            val csql = pkSqls.getOrPut(this.javaClass){
                queryBuilder().where(metadata.primaryKey, id).compileSelectOne() // 构建根据主键来查询的sql
            }
            csql.find(){ // 查询
                this.original(it); // 读取查询数据
            }
        }
    }

    public override fun toString(): String {
        return "${this.javaClass}: " + data.toString()
    }
}
