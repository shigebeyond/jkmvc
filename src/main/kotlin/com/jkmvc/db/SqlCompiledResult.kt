package com.jkmvc.db

import java.util.*


/**
 * sql编译结果
 *
 * @author shijianhang
 * @date 2017-6-10 下午8:02:47
 */
class SqlCompiledResult : Cloneable, ISqlCompiledResult {

    /**
     * 编译好的sql
     */
    override var sql:String = ""

    /**
     * 编译后的sql参数 / 静态参数
     */
    public var staticParams: LinkedList<Any?> = LinkedList<Any?>()

    /**
     * 动态参数
     */
    public var dynamicParams: Array<out Any?>? = null

    /**
     *  实际参数 = 静态参数 + 动态参数
     */
    override val params:List<Any?>
        get(){
            if(dynamicParams == null)
                return staticParams;

            val param = ArrayList<Any?>(staticParams.size)
            var i = 0; // 动态变量的迭代索引
            for (v in staticParams){
                if(v is String && v == "?") // 如果参数值是?，则认为是动态参数
                    param.add(dynamicParams!![i++])
                else // 静态参数
                    param.add(v)
            }
            return param;
        }

    /**
     * 判断是否为空
     */
    override fun isEmpty():Boolean{
        return sql == ""
    }

    /**
     * 清空编译结果
     */
    override fun clear(): SqlCompiledResult {
        sql = "";
        staticParams.clear();
        dynamicParams = null;
        return this;
    }

    /**
     * 克隆对象
     * @return o
     */
    public override fun clone(): Any {
        val o = super.clone() as SqlCompiledResult
        o.staticParams = staticParams.clone() as LinkedList<Any?>
        return o
    }

    /**
     * 预览sql
     * @return
     */
    override fun previewSql(useDynParams:Boolean): String {
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