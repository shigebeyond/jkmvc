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
abstract class Orm(): OrmRelated() {

    public constructor(id:Int):this(){
        queryBuilder().where(metadata.primaryKey, id).find(){
            this.original(it);
        }
    }

    public override fun toString(): String {
        return "${this.javaClass}: " + data.toString()
    }
}
