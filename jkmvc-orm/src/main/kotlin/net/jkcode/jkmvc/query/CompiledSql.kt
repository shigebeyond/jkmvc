package net.jkcode.jkmvc.query

import net.jkcode.jkutil.common.cloneProperties
import net.jkcode.jkutil.common.dbLogger
import net.jkcode.jkmvc.db.Db
import net.jkcode.jkmvc.db.DbConfig
import net.jkcode.jkmvc.db.DbResultSet
import net.jkcode.jkmvc.db.IDb
import java.util.*
import kotlin.collections.ArrayList


/**
 * 编译好的sql
 *   为了避免多次编译，可以缓存该编译好的sql，其属性 sql/staticParams/dynamicParams 方法 previewSql() 有可能会被调用多次
 *   动态参数，是指静态参数中为?(DbExpr.question)的参数
 *
 * @author shijianhang
 * @date 2017-6-10 下午8:02:47
 */
class CompiledSql : ICompiledSql() {

    /**
     * 默认db
     *   CompiledSql 有可能被多个请求持有, 因此每次都读最新的db
     *   不能直接赋值, 否则持有的db被上一个请求释放了, 下一个请求再用就报错
     */
    public override val defaultDb: IDb
        get() = Db.instance()

    /****************************** 构建参数/sql *******************************/
    /**
     * 编译好的sql
     */
    public override var sql:String = ""
        // 不在这里而在Db中预览
        /*set(sql:String){
            field = sql;

            // 预览sql
            if(DbConfig.debug && sql != "")
                dbLogger.debug("Preview sql: {}", previewSql())
        }*/

    /**
     * 编译后的sql参数 / 静态参数
     */
    public override val staticParams: LinkedList<Any?> = LinkedList<Any?>()

    /**
     * 动态参数的个数 = 静态参数中?的个数
     */
    public override val dynamicParamsSize:Int
        get(){
            return staticParams.count { param ->
                param == DbExpr.question
            }
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
        val o = super.clone()
        // 复制复杂属性: 静态参数
        o.cloneProperties("staticParams")
        return o
    }

    /**
     * 构建实际参数 = 静态参数 + 动态参数
     *
     * @param dynamicParamses 动态参数
     * @return
     */
    public override fun buildParams(dynamicParams: List<*>):List<Any?>{
        if(dynamicParams.isEmpty())
            return staticParams;

        // 检查动态参数个数
        val size = dynamicParamsSize;
        if(dynamicParams.size != size)
            //throw IllegalArgumentException("动态参数个数不对：需要 $size 个，传递 ${dynamicParams.size}")
            throw IllegalArgumentException("Mismatch dynamic parameters size：It needs [$size], but you pass [${dynamicParams.size}]")

        // 全都是动态参数
        if(staticParams.size == size)
            return dynamicParams

        // 构建实际参数：将静态参数中?，替换为动态参数
        return collectParams(ArrayList<Any?>(staticParams.size), dynamicParams)
    }

    /**
     * 收集实际参数 = 静态参数 + 动态参数
     *
     * @param result 实际参数
     * @param dynamicParamses 动态参数
     * @param fromIndex 动态参数的开始索引
     * @return
     */
    protected fun collectParams(result: ArrayList<Any?>, dynamicParams: List<*>, fromIndex:Int = 0): ArrayList<Any?> {
        // 构建实际参数：将静态参数中?，替换为动态参数
        var i = 0; // 动态变量的迭代索引
        for (v in staticParams) {
            if (v == DbExpr.question) // 如果参数值是?，则认为是动态参数
                result.add(dynamicParams[fromIndex + (i++)])
            else // 静态参数
                result.add(v)
        }

        // 预览sql
//        if(DbConfig.debug)
//            dbLogger.debug("Preview sql: {}", previewSql(dynamicParams, fromIndex))

        return result;
    }

    /**
     * 构建实际参数 = 静态参数 + 动态参数
     *
     * @param dynamicParamses 多次处理的动态参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @return
     */
    public override fun buildBatchParamses(dynamicParamses: List<Any?>):List<Any?>{
        // 检查动态参数个数, 即一次处理的参数个数
        val size = dynamicParamsSize;

        // 检查批处理的次数
        if(dynamicParamses.size % size > 0)
            throw IllegalArgumentException("Mismatch batch parameter size: Parameter `dynamicParamses`'s size [${dynamicParamses.size}] is not an integral multiple of [$size]");

        // 全都是动态参数
        if(staticParams.size == size)
            return dynamicParamses;

        // 批处理的次数
        val batchNum:Int = dynamicParamses.size / size

        // 构建实际参数：将静态参数中?，替换为动态参数
        val realParams = ArrayList<Any?>(staticParams.size * batchNum)
        for(i in 0..(batchNum - 1)){
            collectParams(realParams, dynamicParamses, i * size)
        }

        return realParams;
    }

    /**
     * 预览sql
     *
     * @param params 动态参数
     * @param fromIndex 动态参数的开始索引
     * @param db 数据库连接
     * @return
     */
    public override fun previewSql(dynamicParams: List<*>, fromIndex: Int, db: IDb): String {
        // 替换实参
        var i = 0 // 静态变量的迭代索引
        var j = fromIndex // 动态变量的迭代索引
        return sql.replace("\\?".toRegex()) { matches: MatchResult ->
            var param = staticParams[i++] // 静态参数
            if(param == DbExpr.question && dynamicParams.isNotEmpty())// 如果参数值是?，则认为是动态参数
                param = dynamicParams[j++]
            if(param is String && param.contains("\n"))
                param = param.replace("\n", "\t") + "\n\t"
            db.quote(param)
        }
    }

    /****************************** 执行sql *******************************/
    /**
     * 查找结果： select 语句
     *
     * @param params 动态参数
     * @param single 是否查一条
     * @param transform 行转换函数
     * @return 列表
     */
    public override fun <T> findResult(params: List<*>, single: Boolean, db: IDb, transform: (DbResultSet) -> T): T{
        // 执行 select
        return db.queryResult(sql, buildParams(params), transform)
    }

    /**
     * 编译 + 执行
     *
     * @param params 动态参数
     * @param generatedColumn 返回的自动生成的主键名
     * @return 影响行数|新增id
     */
    public override fun execute(params: List<*>, generatedColumn:String?, db: IDb): Long {
        return db.execute(sql, buildParams(params), generatedColumn);
    }

    /**
     * 批量更新有参数的sql
     *
     * @param action sql动作：select/insert/update/delete
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @return
     */
    public override fun batchExecute(paramses: List<Any?>, db: IDb): IntArray {
        // 批量执行有参数sql
        return db.batchExecute(sql, buildBatchParamses(paramses), staticParams.size);
    }

}