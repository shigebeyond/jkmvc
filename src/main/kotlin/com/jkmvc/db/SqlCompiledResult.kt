package com.jkmvc.db

import java.util.*


interface ICompiledResult {
    /**
     * 编译好的sql
     */
    var sql: String
    /**
     *  实际参数 = 静态参数 + 动态参数
     */
    val params: List<Any?>

    /**
     * 判断是否为空
     */
    fun isEmpty(): Boolean

    /**
     * 清空编译结果
     */
    fun clear(): CompiledResult

    /**
     * 预览sql
     * @return
     */
    fun previewSql(): String
}

/**
 * sql编译结果
 */
class CompiledResult: Cloneable, ICompiledResult {

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
    public var dynamicParams: List<Any?>? = null

    /**
     * 判断是否为空
     */
    override fun isEmpty():Boolean{
        return sql == ""
    }

    /**
     *  实际参数 = 静态参数 + 动态参数
     */
    override val params:List<Any?>
        get(){
            if(dynamicParams == null)
                return staticParams;

            val param = ArrayList<Any?>(staticParams.size)
            var i = 0;
            for (v in staticParams){
                if(v == "?") // 如果参数值是?，则认为是动态参数
                    param.add(dynamicParams!![i++])
                else // 静态参数
                    param.add(v)
            }
            return param;
        }

    /**
     * 清空编译结果
     */
    override fun clear(): CompiledResult {
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
        val o = super.clone() as CompiledResult
        o.staticParams = staticParams.clone() as LinkedList<Any?>
        return o
    }

    /**
     * 预览sql
     * @return
     */
    override fun previewSql(): String {
        // 替换实参
        var i = 0
        val realSql = sql.replace("\\?".toRegex()) { matches: MatchResult ->
            val param = staticParams[i++]
            if(param is String)
                "\"$param\""
            else
                param.toString()
        }
        return realSql
    }
}