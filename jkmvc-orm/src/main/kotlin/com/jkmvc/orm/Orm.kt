package com.jkmvc.orm

/**
 * ORM

 * @Package package_name
 * @category
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 */
abstract class Orm(id:Any? = null): OrmRelated() {

    init{
        if(id != null){
            // 构建根据主键来查询的sql
            queryBuilder().where(ormMeta.primaryKey, id).find(){
                this.setOriginal(it); // 读取查询数据
            }
        }
    }

    public override fun toString(): String {
        return "${this.javaClass}: " + data.toString()
    }
}
