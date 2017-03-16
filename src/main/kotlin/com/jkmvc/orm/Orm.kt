package com.jkmvc.orm

import java.util.*

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

    init{
        if(id != null)
            queryBuilder().where(metadata.primaryKey, id).find(){
                this.original(it);
            }
    }

    public override fun toString(): String {
        return "${this.javaClass}: " + data.toString()
    }
}
