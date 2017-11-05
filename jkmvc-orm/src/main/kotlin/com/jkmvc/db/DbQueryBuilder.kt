package com.jkmvc.db

import java.math.BigDecimal
import kotlin.reflect.KClass

/**
 * sql构建器
 *   依次继承 DbQueryBuilderAction 处理动作子句 + DbQueryBuilderDecoration 处理修饰子句
 *  提供select/where等类sql的方法, 但是调用方法时, 不直接拼接sql, 而是在compile()时才延迟拼接sql, 因为调用方法时元素可以无序, 但生成sql时元素必须有序
 *
 * @author shijianhang
 * @date 2016-10-13
 */
open class DbQueryBuilder(db:IDb = Db.getDb(), table:Pair<String, String?> /*表名*/ = emptyTable) :DbQueryBuilderDecoration(db, table)
{
    /**
     * 缓存编译好的sql
     */
    protected var compiledSql: CompiledSql = CompiledSql();

    /**
     * 构造函数
     * @param db 数据库连接
     * @param table 表名
     */
    public constructor(db: IDb, table: String):this(db, if(table == "") emptyTable else Pair(table, null)){
    }

    /**
     * 构造函数
     * @param dbName 数据库名
     * @param table 表名
     */
    public constructor(dbName:String, table:String = ""):this(Db.getDb(dbName), table){
    }

    /**
     * 清空条件
     * @return
     */
    public override fun clear(): IDbQueryBuilder {
        compiledSql.clear();
        return this;
    }

    /**
     * 克隆对象
     * @return o
     */
    public override fun clone(): Any {
        val o = super.clone() as DbQueryBuilder
        // 复制编译结果
        o.compiledSql = compiledSql.clone() as CompiledSql
        return o;
    }

    /**
     * 改写转义值的方法，搜集sql参数
     *
     * @param value
     * @return
     */
    public override fun quote(value: Any?): String {
        // 1 将参数值直接拼接到sql
        //return db.quote(value);

        // 2 sql参数化: 将参数名拼接到sql, 独立出参数值, 以便执行时绑定参数值
        // 2.1 多值
        if(value is Array<*>){
            return value.joinToString(", ", "(", ")") {
                // 单值
                quote(it)
            }
        }
        if(value is Collection<*>){
            return value.joinToString(", ", "(", ")") {
                // 单值
                quote(it)
            }
        }

        // 2.2 单值
        compiledSql.staticParams.add(value);
        return "?";
    }

    /**
     * 获得记录转换器
     * @param clazz 要转换的类
     * @return 转换的匿名函数
     */
    public override fun <T:Any> getRecordTranformer(clazz: KClass<T>): ((MutableMap<String, Any?>) -> T) {
        return clazz.recordTranformer
    }

    /**
     * 编译sql
     *
     * @param action sql动作：select/insert/update/delete
     * @return 编译好的sql
     */
    public override fun compile(action:ActionType): CompiledSql
    {
        // 清空编译结果
        compiledSql.clear();

        // 设置动作
        this.action = action;

        // 编译动作子句 + 修饰子句
        // 其中，sql收集编译好的语句，compiledSql.staticParams收集静态参数
        val sql: StringBuilder = StringBuilder();
        this.compileAction(sql).compileDecoration(sql);

        // 收集编译好的sql
        compiledSql.sql = sql.toString()

        return compiledSql
    }

    /**
     * 编译select语句
     * @return 编译好的sql
     */
    public override fun compileSelect(): CompiledSql{
        return compile(ActionType.SELECT)
    }

    /**
     * 编译select ... limit 1语句
     * @return 编译好的sql
     */
    public override fun compileSelectOne(): CompiledSql{
        if(db.dbType == DbType.Oracle) { // oracle
            where("rownum", "<=", 1)
        }else{
            limit(1)
        }
        return compile(ActionType.SELECT)
    }

    /**
     * 编译select count() 语句
     * @return 编译好的sql
     */
    public override fun compileCount(): CompiledSql{
        return select(Pair("count(1)", "num")).compile(ActionType.SELECT);
    }

    /**
     * 编译insert语句
     * @return 编译好的sql
     */
    public override fun compileInsert(): CompiledSql{
        return compile(ActionType.INSERT)
    }

    /**
     * 编译update语句
     * @return 编译好的sql
     */
    public override fun compileUpdate(): CompiledSql{
        return compile(ActionType.UPDATE)
    }

    /**
     * 编译delete语句
     * @return 编译好的sql
     */
    public override fun compileDelete(): CompiledSql{
        return compile(ActionType.DELETE)
    }

    /**
     * 查找多个： select 语句
     *
     * @param params 动态参数
     * @param transform 转换函数
     * @return 列表
     */
    public override fun <T:Any> findAll(vararg params: Any?, transform:(MutableMap<String, Any?>) -> T): List<T>{
        // 1 编译
        val result = compile(ActionType.SELECT);

        // 2 执行 select
        return db.queryRows<T>(result.sql, result.buildParams(params), transform)
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param params 动态参数
     * @param transform 转换函数
     * @return 单个数据
     */
    public override fun <T:Any> find(vararg params: Any?, transform:(MutableMap<String, Any?>) -> T): T?{
        // 1 编译
        val result = compileSelectOne()

        // 2 执行 select
        return db.queryRow<T>(result.sql, result.buildParams(params), transform);
    }

    /**
     * 查询一列（多行）
     *
     * @param params 动态参数
     * @return
     */
    public override fun findColumn(vararg params: Any?): List<Any?>{
        // 1 编译
        val result = compile(ActionType.SELECT);

        // 2 执行 select
        return db.queryColumn(result.sql, result.buildParams(params))
    }

    /**
     * 统计行数： count语句
     *
     * @param params 动态参数
     * @return
     */
    public override fun count(vararg params: Any?):Long
    {
        // 1 编译
        val result = select(Pair("count(1)", "num")).compile(ActionType.SELECT);

        // 2 执行 select
        val (hasNext, count) = db.queryCell(result.sql, result.buildParams(params));
        if(!hasNext)
            return 0

        // oracle 是 BigDecimal
        if(count is BigDecimal)
            return count.toLong()

        // mysql
        return count as Long;
    }

    /**
     * 编译 + 执行
     *
     * @param action sql动作：select/insert/update/delete
     * @param params 动态参数
     * @param returnGeneratedKey 是否返回自动生成的主键
     * @return 影响行数|新增id
     */
    public override fun execute(action:ActionType, params:Array<out Any?>, returnGeneratedKey:Boolean):Int
    {
        // 1 编译
        val result = compile(action);

        // 2 执行sql
        return db.execute(result.sql, result.buildParams(params), returnGeneratedKey);
    }

    /**
     * 批量更新有参数的sql
     *
     * @param action sql动作：select/insert/update/delete
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @return
     */
    public override fun batchExecute(action:ActionType, paramses: List<Any?>, paramSize:Int): IntArray {
        // 1 编译
        val result = compile(action);

        // 2 批量执行有参数sql
        return db.batchExecute(result.sql, result.buildBatchParamses(paramses, paramSize), result.staticParams.size);
    }

    /**
     * 批量插入
     *
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @return
     */
    public override fun batchInsert(paramses: List<Any?>, paramSize:Int): IntArray {
        return batchExecute(ActionType.INSERT, paramses, paramSize)
    }

    /**
     * 插入：insert语句
     *
     *  @param returnGeneratedKey 是否返回自动生成的主键
     *  @param params 动态参数
     * @return 新增的id
     */
    public override fun insert(returnGeneratedKey:Boolean, vararg params: Any?):Int
    {
        return execute(ActionType.INSERT, params, returnGeneratedKey);
    }

    /**
     * 更新：update语句
     *
     * @param params 动态参数
     * @return
     */
    public override fun update(vararg params: Any?):Boolean
    {
        return execute(ActionType.UPDATE, params) > 0;
    }

    /**
     * 删除
     *
     * @param params 动态参数
     * @return
     */
    public override fun delete(vararg params: Any?):Boolean
    {
        return execute(ActionType.DELETE, params) > 0;
    }
}