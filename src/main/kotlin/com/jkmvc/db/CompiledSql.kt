package com.jkmvc.db

import java.util.*
import kotlin.collections.ArrayList


/**
 * 编译好的sql
 *   在预编译的场景下，该编译好的sql会被缓存，其属性 sql/staticParams/dynamicParams 方法debugSql() 有可能会被调用多次
 *   动态参数，是指静态参数中为?的参数
 *   如果你的实际参数指就是?，请使用静态参数，不要使用动态参数（就是buildParams()中参数为空），因为这样动态参数的个数是对不上的
 *
 * @author shijianhang
 * @date 2017-6-10 下午8:02:47
 */
class CompiledSql : Cloneable, ICompiledSql {

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
     * 动态参数的个数 = 静态参数中?的个数
     */
    public override val dynamicParamsSize:Int
        get(){
            var size = 0;
            for(param in staticParams)
                if(param is String && param == "?")
                    size++;
            return size
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
    override fun clear(): ICompiledSql {
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
        val o = super.clone() as CompiledSql
        o.staticParams = staticParams.clone() as LinkedList<Any?>
        return o
    }

    /**
     * 构建实际参数 = 静态参数 + 动态参数
     *
     * @param dynamicParamses 动态参数
     * @return
     */
    public override fun buildParams(dynamicParams:List<Any?>):List<Any?>{
        if(dynamicParams.isEmpty())
            return staticParams;

        // 检查动态参数个数
        val size = dynamicParamsSize;
        if(dynamicParams.size != size)
            throw DbException("动态参数个数不对：需要 $size 个，传递 ${dynamicParams.size} ");

        // 构建实际参数：将静态参数中?，替换为动态参数
        return collectParams(ArrayList<Any?>(staticParams.size), dynamicParams)
    }

    /**
     * 收集实际参数 = 静态参数 + 动态参数
     *
     * @param dynamicParamses 动态参数
     * @param fromIndex 动态参数的开始索引
     * @return
     */
    protected fun collectParams(result: ArrayList<Any?>, dynamicParams: List<Any?>, fromIndex:Int = 0): ArrayList<Any?> {
        // 构建实际参数：将静态参数中?，替换为动态参数
        var i = 0; // 动态变量的迭代索引
        for (v in staticParams) {
            if (v is String && v == "?") // 如果参数值是?，则认为是动态参数
                result.add(dynamicParams[fromIndex + (i++)])
            else // 静态参数
                result.add(v)
        }

        // 预览sql
        if(Db.debug)
            println("实际的sql：" + previewSql(dynamicParams, fromIndex))

        return result;
    }

    /**
     * 构建实际参数 = 静态参数 + 动态参数
     *
     * @param dynamicParamses 多次处理的动态参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @return
     */
    public override fun buildBatchParamses(dynamicParamses: List<Any?>, paramSize: Int):List<Any?>{
        // 检查动态参数个数
        val size = dynamicParamsSize;
        if(paramSize != size)
            throw DbException("动态参数个数不对：需要 $size 个，传递 $paramSize ");

        // 计算批处理的次数
        if(paramSize <= 0)
            throw Exception("paramSize 只能为正整数");
        if(dynamicParamses.size % paramSize > 0)
            throw Exception("paramses 的大小必须是 paramSize 的整数倍");
        val batchNum:Int = dynamicParamses.size / paramSize

        // 构建实际参数：将静态参数中?，替换为动态参数
        val realParams = ArrayList<Any?>(staticParams.size * batchNum)
        for(i in 0..(batchNum - 1)){
            collectParams(realParams, dynamicParamses, i * paramSize)
        }

        return realParams;
    }

    /**
     * 预览sql
     *
     * @param params 动态参数
     * @param fromIndex 动态参数的开始索引
     * @return
     */
    public override fun previewSql(dynamicParams: List<Any?>, fromIndex: Int): String {
        // 替换实参
        var i = 0 // 静态变量的迭代索引
        var j = fromIndex // 动态变量的迭代索引
        val realSql = sql.replace("\\?".toRegex()) { matches: MatchResult ->
            val param = staticParams[i++] // 静态参数
            if(param is String){
                if(param == "?" && dynamicParams.isNotEmpty())// 如果参数值是?，则认为是动态参数
                    dynamicParams[j++].toString()
                else
                    "\"$param\""
            } else
                param.toString()
        }
        return realSql
    }
}