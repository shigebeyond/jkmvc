package com.jkmvc.test

import com.jkmvc.db.getDruidDataSource
import com.jkmvc.db.Db
import com.jkmvc.db.DbQueryBuilder
import java.sql.Connection

fun main(vararg args:String){
    val dataSource = getDruidDataSource();
    val db:Db = Db.connect(dataSource);
    val builder = DbQueryBuilder(db).table("user").where("id", "=", 1)
    val record = builder.find();
    println(record)
    val count = builder.count();
    println(count)
}


