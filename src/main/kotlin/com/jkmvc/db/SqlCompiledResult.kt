package com.jkmvc.db

import java.util.*
import kotlin.collections.ArrayList


/**
 * sql编译结果
 *   在预编译的场景下，该sql编译结果会被缓存，其属性 sql/params/dynamicParams 方法debugSql() 有可能会被调用多次
 *
 * @author shijianhang
 * @date 2017-6-10 下午8:02:47
 */
class SqlCompiledResult : Cloneable, ISqlCompiledResult {

    /**
     * 编译好的sql
     */
    public override var sql:String = ""
        set(sql:String){
            field = sql;

            // 预览sql
            if(Db.debug && sql != "")
                println("编译好的sql：" + previewSql())
        }

    /**
     * 编译后的sql参数 / 静态参数
     */
    public override var staticParams: LinkedList<Any?> = LinkedList<Any?>()

    /**
     * 构建实际参数 = 静态参数 + 动态参数
     *
     * @param params 动态参数
     * @return
     */
    public override fun buildParams(dynamicParams:Array<out Any?>):List<Any?>{
        if(dynamicParams.isEmpty())
            return staticParams;

        // 构建实际参数：将静态参数中?，替换为动态参数
        val realParams = ArrayList<Any?>()
        var i = 0; // 动态变量的迭代索引
        for (v in staticParams){
            if(v is String && v == "?") // 如果参数值是?，则认为是动态参数
                realParams.add(dynamicParams!![i++])
            else // 静态参数
                realParams.add(v)
        }

        // 预览sql
        if(Db.debug)
            println("实际的sql：" + previewSql(dynamicParams))

        return realParams;
    }

    /**
     * 判断是否为空
     * @return
     */
    override fun isEmpty():Boolean{
        return sql == ""
    }

    /**
     * 清空编译结果
     * @return
     */
    override fun clear(): SqlCompiledResult {
        sql = "";
        staticParams.clear();
        // dynamicParams = null;
        return this;
    }

    /**
     * 克隆对象
     * @return
     */
    public override fun clone(): Any {
        val o = super.clone() as SqlCompiledResult
        o.staticParams = staticParams.clone() as LinkedList<Any?>
        return o
    }

    /**
     * 预览sql
     *
     * @param params 动态参数
     * @return
     */
    public override fun previewSql(dynamicParams:Array<out Any?>?): String {
        // 替换实参
        var i = 0 // 静态变量的迭代索引
        var j = 0 // 动态变量的迭代索引
        val realSql = sql.replace("\\?".toRegex()) { matches: MatchResult ->
            val param = staticParams[i++] // 静态参数
            if(param is String){
                if(param == "?" && dynamicParams != null)// 如果参数值是?，则认为是动态参数
                    dynamicParams!![j++].toString()
                else
                    "\"$param\""
            } else
                param.toString()
        }
        return realSql
    }
}