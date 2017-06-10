package com.jkmvc.db

import com.jkmvc.common.findConstructor
import com.jkmvc.common.forceClone
import kotlin.reflect.KClass

/**
 * sql构建器
 *   依次继承 DbQueryBuilderAction 处理动作子句 + DbQueryBuilderDecoration 处理修饰子句
 *  提供select/where等类sql的方法, 但是调用方法时, 不直接拼接sql, 而是在compile()时才延迟拼接sql, 因为调用方法时元素可以无序, 但生成sql时元素必须有序
 *
 * @author shijianhang
 * @date 2016-10-13
 */
open class DbQueryBuilder(db:IDb = Db.getDb(), table:String = "" /*表名*/) :DbQueryBuilderDecoration(db, table)
{
    companion object{
        // 是否调试
        val debug = true;
    }

    /**
     * 缓存sql编译结果
     */
    protected var compiledResult: SqlCompiledResult = SqlCompiledResult();

    /**
     * 预编译参数化的sql
     *
     * 控制是否构建参数化的查询，如果是，则这个带参数的查询会预编译并缓存sql，下次构建同一个查询时，直接使用缓存的sql，不用重复编译
     * controlls whether to build a parameterized query, if true it will compile and cache sql, so next time when you build the same query, it will use the cached sql, no need to compile again
     *
     * 查询中需要进行转义的值，如果是?占位符的话，会被当成是参数，在sql只执行时赋以实参
     * value in query which is quote, if value is '?', it will be treat as parameters, and assign a actual value when executing sql
     */
    protected var prepared:Boolean = false;

    public constructor(dbName:String /* db名 */, table:String = "" /*表名*/):this(Db.getDb(dbName), table){
    }

    /**
     * 清空条件
     * @return
     */
    public override fun clear(): IDbQueryBuilder {
        compiledResult.clear();
        return this;
    }

    /**
     * 克隆对象
     * @return o
     */
    public override fun clone(): Any {
        val o = super.clone() as DbQueryBuilder
        // 复制编译结果
        o.compiledResult = compiledResult.clone() as SqlCompiledResult
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
        compiledResult.staticParams.add(value);
        return "?";
    }

    /**
     * 获得记录转换器
     * @param clazz 要转换的类
     * @return 转换的匿名函数
     */
    public override fun <T:Any> getRecordTranformer(clazz: KClass<T>): ((MutableMap<String, Any?>) -> T) {
        // 1 如果是map类，则直接返回
        if(Map::class.java.isAssignableFrom(clazz.java)){
            return {
                it as T;
            }
        }
        // 2 否则，调用其构造函数
        // 获得类的构造函数
        val construtor = clazz.findConstructor(listOf(MutableMap::class.java))
        if(construtor == null)
            throw RuntimeException("类${clazz}没有构造函数constructor(MutableMap)");

        // 调用构造函数
        return {
            construtor.call(it) as T; // 转换一行数据: 直接调用构造函数
        }
    }

    /**
     * 设置是否预编译参数化的sql
     *
     * @param prepared 是否预编译sql
     * @return IDbQueryBuilder
     */
    public override fun prepare(prepared:Boolean):IDbQueryBuilder{
        this.prepared = prepared
        return this;
    }

    /**
     * 编译sql
     *
     * @param action sql动作：select/insert/update/delete
     * @return Pair(sql, 参数)
     */
    public override fun compile(action:ActionType): SqlCompiledResult
    {
        // 1 度缓存
        // 如果是预编译sql，则直接返回上一次缓存的编译结果
        if(prepared && !compiledResult.isEmpty())
            return compiledResult;

        // 2 编译
        // 清空编译结果
        compiledResult.clear();

        // 设置动作
        this.action = action;

        // 编译动作子句 + 修饰子句
        // 其中，sql收集编译好的语句，compiledResult.staticParams收集静态参数
        val sql:StringBuilder = StringBuilder();
        this.compileAction(sql).compileDecoration(sql);

        // 收集编译好的sql
        compiledResult.sql = sql.toString()

        // 预览sql
        if(debug)
            println(compiledResult.previewSql())

        return compiledResult
    }

    /**
     * 设置动态参数
     * @param params 动态参数
     * @return IDbQueryBuilder
     */
    public override fun setParameters(vararg params: Any?):IDbQueryBuilder{
        compiledResult.dynamicParams = params;
        return this;
    }

    /**
     * 查找多个： select 语句
     *
     * @param transform 转换函数
     * @return 列表
     */
    public override fun <T:Any> findAll(transform:(MutableMap<String, Any?>) -> T): List<T>{
        // 1 编译
        val result = compile(ActionType.SELECT);

        // 2 执行 select
        return db.queryRows<T>(result.sql, result.params, transform)
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param transform 转换函数
     * @return 单个数据
     */
    public override fun <T:Any> find(transform:(MutableMap<String, Any?>) -> T): T?{
        // 1 编译
        val result = compile(ActionType.SELECT);

        // 2 执行 select
        return db.queryRow<T>(result.sql, result.params, transform);
    }

    /**
     * 编译 + 执行
     *
     * @param action sql动作：select/insert/update/delete
     * @param returnGeneratedKey
     * @return 影响行数|新增id
     */
    protected fun execute(action:ActionType, returnGeneratedKey:Boolean = false):Int
    {
        // 1 编译
        val result = compile(action);

        // 2 执行 insert/update/delete
        return db.execute(result.sql, result.params, returnGeneratedKey);
    }

    /**
     * 统计行数： count语句
     * @return
     */
    public override fun count():Long
    {
        // 1 编译
        val result = select(Pair("count(1)", "num")).compile(ActionType.SELECT);

        // 2 执行 select
        val (hasNext, count) = db.queryCell(result.sql, result.params);
        return if(hasNext)
                    count as Long;
                else
                    0
    }

    /**
     * 插入：insert语句
     *
     *  @param returnGeneratedKey 是否返回自动生成的主键
     * @return 新增的id
     */
    public override fun insert(returnGeneratedKey:Boolean):Int
    {
        return execute(ActionType.INSERT, returnGeneratedKey);
    }

    /**
     *	更新：update语句
     *	@return	bool
     */
    public override fun update():Boolean
    {
        return execute(ActionType.UPDATE) > 0;
    }

    /**
     *	删除
     *	@return	bool
     */
    public override fun delete():Boolean
    {
        return execute(ActionType.DELETE) > 0;
    }
}