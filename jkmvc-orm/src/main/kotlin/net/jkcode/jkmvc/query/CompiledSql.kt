package net.jkcode.jkmvc.query

import net.jkcode.jkmvc.db.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KClass


/**
 * 编译好的sql
 *   为了避免多次编译，可以缓存该编译好的sql，其属性 sql/staticParams/dynamicParams 方法 previewSql() 有可能会被调用多次
 *   动态参数，是指静态参数中为?(DbExpr.question)的参数
 *
 * @author shijianhang
 * @date 2017-6-10 下午8:02:47
 */
class CompiledSql : Cloneable, ICompiledSql() {

    /**
     * 默认db
     */
    public override val defaultDb: IDb = Db.instance()

    /****************************** 构建参数/sql *******************************/
    /**
     * 编译好的sql
     */
    public override var sql:String = ""
        set(sql:String){
            field = sql;

            // 预览sql
            if(Db.debug && sql != "")
                dbLogger.debug("编译好的sql：{}", previewSql())
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
                if(param == DbExpr.question)
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
    public override fun buildParams(dynamicParams: List<Any?>):List<Any?>{
        if(dynamicParams.isEmpty())
            return staticParams;

        // 检查动态参数个数
        val size = dynamicParamsSize;
        if(dynamicParams.size != size)
            throw IllegalArgumentException("动态参数个数不对：需要 $size 个，传递 ${dynamicParams.size} ");

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
    protected fun collectParams(result: ArrayList<Any?>, dynamicParams: List<Any?>, fromIndex:Int = 0): ArrayList<Any?> {
        // 构建实际参数：将静态参数中?，替换为动态参数
        var i = 0; // 动态变量的迭代索引
        for (v in staticParams) {
            if (v == DbExpr.question) // 如果参数值是?，则认为是动态参数
                result.add(dynamicParams[fromIndex + (i++)])
            else // 静态参数
                result.add(v)
        }

        // 预览sql
        if(Db.debug)
            dbLogger.debug("实际的sql：{}", previewSql(dynamicParams, fromIndex))

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
            throw IllegalArgumentException("动态参数个数不对：需要 $size 个，传递 $paramSize ");

        // 检查批处理的次数
        if(paramSize <= 0)
            throw IllegalArgumentException("参数个数只能为正整数，但实际为 $paramSize");
        if(dynamicParamses.size % paramSize > 0)
            throw IllegalArgumentException("paramses 的大小必须是指定参数个数 $paramSize 的整数倍");

        // 全都是动态参数
        if(staticParams.size == size)
            return dynamicParamses;

        // 批处理的次数
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
     * @param db 数据库连接
     * @return
     */
    public override fun previewSql(dynamicParams: List<Any?>, fromIndex: Int, db: IDb): String {
        // 替换实参
        var i = 0 // 静态变量的迭代索引
        var j = fromIndex // 动态变量的迭代索引
        return sql.replace("\\?".toRegex()) { matches: MatchResult ->
            var param = staticParams[i++] // 静态参数
            if(param == DbExpr.question && dynamicParams.isNotEmpty())// 如果参数值是?，则认为是动态参数
                param = dynamicParams[j++]
            db.quote(param)
        }
    }

    /****************************** 执行sql *******************************/
    /**
     * 查找多个： select 语句
     *
     * @param params 动态参数
     * @param transform 转换函数
     * @return 列表
     */
    public override fun <T:Any> findAll(params: List<Any?>, db: IDb, transform: (Row) -> T): List<T>{
        // 执行 select
        return db.queryRows(sql, buildParams(params), transform)
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param params 动态参数
     * @param transform 转换函数
     * @return 单个数据
     */
    public override fun <T:Any> find(params: List<Any?>, db: IDb, transform: (Row) -> T): T?{
        return db.queryRow(sql, buildParams(params), transform);
    }

    /**
     * 查询一列（多行）
     *
     * @param params 动态参数
     * @return
     */
    public override fun <T:Any> findColumn(params: List<Any?>, clazz: KClass<T>?, db: IDb): List<T> {
        // 执行 select
        return db.queryColumn(sql, buildParams(params), clazz)
    }

    /**
     * 查询一个值（单行单列）
     *
     * @param params 动态参数
     * @return
     */
    public override fun <T:Any> findCell(params: List<Any?>, clazz: KClass<T>?, db: IDb): Cell<T> {
        // 执行 select
        return db.queryCell(sql, buildParams(params), clazz);
    }

    /**
     * 编译 + 执行
     *
     * @param params 动态参数
     * @param generatedColumn 返回的自动生成的主键名
     * @return 影响行数|新增id
     */
    public override fun execute(params: List<Any?>, generatedColumn:String?, db: IDb):Int {
        return db.execute(sql, buildParams(params), generatedColumn);
    }

    /**
     * 批量更新有参数的sql
     *
     * @param action sql动作：select/insert/update/delete
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @return
     */
    public override fun batchExecute(paramses: List<Any?>, paramSize:Int, db: IDb): IntArray {
        // 批量执行有参数sql
        return db.batchExecute(sql, buildBatchParamses(paramses, paramSize), staticParams.size);
    }

}