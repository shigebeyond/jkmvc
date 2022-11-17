package net.jkcode.jkmvc.orm.jphp

import net.jkcode.jkmvc.orm.*
import net.jkcode.jkmvc.query.DbExpr
import net.jkcode.jkmvc.query.IDbQueryBuilder
import net.jkcode.jkutil.common.mapSelf
import net.jkcode.jkutil.common.mapToArray
import net.jkcode.jphp.ext.toPureArray
import net.jkcode.jphp.ext.toPureList
import net.jkcode.jphp.ext.toPureMap
import php.runtime.annotation.Reflection
import php.runtime.env.Environment
import php.runtime.ext.java.JavaObject
import php.runtime.lang.BaseWrapper
import php.runtime.memory.ArrayMemory
import php.runtime.memory.ObjectMemory
import php.runtime.reflection.ClassEntity

/**
 * 包装db
 *    java中的实例化： val qb = PQueryBuilder.of(env, xxx)
 *    php中的实例化: $qb = Db::instance("default")->queryBuilder();
 *    php中的方法调用: $qb->table("test");
 */
@Reflection.Name("QueryBuilder")
@Reflection.Namespace(JkmvcOrmExtension.NS)
open class PQueryBuilder(env: Environment, clazz: ClassEntity) : BaseWrapper<JavaObject>(env, clazz) {

    companion object {
        // 创建 PQueryBuilder 实例
        fun of(env: Environment, qb: IDbQueryBuilder): PQueryBuilder {
            val obj = PQueryBuilder(env, env.fetchClass(JkmvcOrmExtension.NS + "\\QueryBuilder"))
            obj.query = qb
            return obj
        }
    }

    // 代理的query builder
    lateinit var query: IDbQueryBuilder

    // 代理的orm query builder
    inline val ormQuery: OrmQueryBuilder
        get() = query as OrmQueryBuilder

    // 缓存php内存对象，因为链式调用多，一般会创建很多内存对象，因此要复用内存对象
    val objMem = ObjectMemory()
    init {
        objMem.value = this
    }

    /**
     * 设置表名
     *
     * @param table 表名
     * @param alias 别名
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun table(table:String, alias:String? = null): ObjectMemory {
        return from(table, alias)
    }

    /**
     * 设置表名
     *
     * @param table 表名
     * @param alias 别名
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun from(table:String, alias:String? = null): ObjectMemory{
        query.from(table, alias)
        return objMem
    }


    /**
     * 设置插入的列, insert时用
     *
     * @param column
     * @return
     */
    @Reflection.Signature
    fun insertColumns(vararg colums:String): ObjectMemory{
        query.insertColumns(*colums)
        return objMem
    }

    /**
     * 设置插入的单行值, insert时用
     *    插入的值的数目必须登录插入的列的数目
     * @param row
     * @return
     */
    @Reflection.Signature
    fun value(row: ArrayMemory): ObjectMemory{
        if(row.isMap)
            query.value(row.toPureMap())
        else
            query.value(*row.toPureArray())
        return objMem
    }

    /**
     * 设置更新的单个值, update时用
     *
     * @param column
     * @param value
     * @return
     */
    @Reflection.Signature
    fun set(column:String, value:Any?): ObjectMemory{
        query.set(column, value)
        return objMem
    }

    /**
     * 设置更新的多个值, update时用
     *
     * @param row
     * @return
     */
    @Reflection.Signature
    fun sets(row: ArrayMemory): ObjectMemory{
        query.sets(row.toPureMap() as Map<String, Any?>)
        return objMem
    }

    /**
     * 设置查询的字段, select时用
     *
     * @param columns 字段名数组，其元素类型是 String 或 Pair
     * @return
     */
    @Reflection.Signature
    fun select(columns:ArrayMemory): ObjectMemory{
        query.select(*buildSelectColumns(columns))
        return objMem
    }

    /**
     * 构建查询字段
     */
    private fun buildSelectColumns(columns: ArrayMemory): Array<CharSequence> {
        val cols: Array<CharSequence>
        if (columns.isMap) {
            cols = columns.toPureMap().entries.mapToArray { (k, v) ->
                if (k is Int)
                    v!!.toString()
                else
                    DbExpr(k.toString(), v.toString())
            }
        } else {
            cols = columns.toPureArray() as Array<CharSequence>
        }
        return cols
    }

    /**
     * 设置查询结果是否去重唯一
     * @returnAction
     */
    @Reflection.Signature
    fun distinct(): ObjectMemory{
        query.distinct()
        return objMem
    }

    /**
     * 设置查询的字段, select时用
     *
     * @param columns 字段名数组，其元素类型是 String 或 Pair
     * @return
     */
    @Reflection.Signature
    fun selectDistinct(vararg columns:String): ObjectMemory{
        query.distinct().select(*columns)
        return objMem
    }

    /**
     * Adds addition tables to "JOIN ...".
     *
     * @param   table  table name | DbExpr | subquery
     * @param   type   joinClause type (LEFT, RIGHT, INNER, etc)
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun join(table: CharSequence, type: String = "INNER"): ObjectMemory{
        val type = if(type.isBlank()) "INNER" else type
        query.join(table, type)
        return objMem
    }

    /**
     * Adds "ON ..." conditions for the last created JOIN statement.
     *
     * @param   c1  column name or DbExpr
     * @param   op  logic operator
     * @param   c2  column name or DbExpr or value
     * @param   isCol whether is column name, or value
     * @return
     */
    @Reflection.Signature
    fun on(c1: String, op: String, c2: Any?): ObjectMemory{
        query.on(c1, op, c2)
        return objMem
    }

    /**
     * 清空条件
     * @return
     */
    @Reflection.Signature
    fun clear(): ObjectMemory{
        query.clear()
        return objMem
    }


    /**
     * 多个having条件
     * @param conditions
     * @return
     */
    @Reflection.Signature
    fun havings(conditions: ArrayMemory): ObjectMemory{
        query.havings(conditions.toPureMap() as Map<String, Any?>)
        return objMem
    }

    /**
     * Alias of andWhere()
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    @Reflection.Signature
    fun where(column: String, op: String, value: Any?): ObjectMemory{
        return andWhere(column, op, value);
    }

    /**
     * Creates a new "WHERE BETWEEN" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   from   column value
     * @param   to   column value
     * @return
     */
    @Reflection.Signature
    fun whereBetween(column: String, from: Any, to: Any): ObjectMemory{
        return where(column, "BETWEEN", Pair(from, to))
    }

    /**
     * Creates a new "OR WHERE BETWEEN" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   from   column value
     * @param   to   column value
     * @return
     */
    @Reflection.Signature
    fun orWhereBetween(column: String, from: Any, to: Any): ObjectMemory{
        return orWhere(column, "BETWEEN", Pair(from, to))
    }

    /**
     * Creates a new "WHERE LIKE" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   value   column value
     * @return
     */
    @Reflection.Signature
    fun whereLike(column: String, value: String): ObjectMemory{
        val exp = if (value.contains('%')) value else "%$value%"
        return where(column, "LIKE", exp)
    }

    /**
     * Creates a new "OR WHERE LIKE" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   value   column value
     * @return
     */
    @Reflection.Signature
    fun orWhereLike(column: String, value: String): ObjectMemory{
        val exp = if (value.contains('%')) value else "%$value%"
        return orWhere(column, "LIKE", exp)
    }

    /**
     * Multiple Where
     *
     * @param   conditions
     * @return
     */
    @Reflection.Signature
    fun wheres(conditions: ArrayMemory): ObjectMemory{
        query.wheres(conditions.toPureMap() as Map<String, Any?>)
        return objMem
    }

    /**
     * Creates a new "AND WHERE" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    @Reflection.Signature
    fun andWhere(column: String, op: String, value: Any?): ObjectMemory{
        query.andWhere(column, op, value)
        return objMem
    }

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    @Reflection.Signature
    fun orWhere(column: String, op: String, value: Any?): ObjectMemory{
        query.orWhere(column, op, value)
        return objMem
    }

    /**
     * Alias of andWhereCondition()
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun whereCondition(condition: String, params: ArrayMemory? = null): ObjectMemory{
        return andWhereCondition(condition, params)
    }

    /**
     * Creates a new "AND WHERE" condition for the query.
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun andWhereCondition(condition: String, params: ArrayMemory? = null): ObjectMemory{
        query.andWhereCondition(condition, params.toPureList())
        return objMem
    }

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun orWhereCondition(condition: String, params: ArrayMemory? = null): ObjectMemory{
        query.orWhereCondition(condition, params.toPureList())
        return objMem
    }

    /**
     * Alias of andWhereOpen()
     *
     * @return
     */
    @Reflection.Signature
    fun whereOpen(): ObjectMemory{
        return andWhereOpen();
    }

    /**
     * Opens a new "AND WHERE (...)" grouping.
     *
     * @return
     */
    @Reflection.Signature
    fun andWhereOpen(): ObjectMemory{
        query.andWhereOpen()
        return objMem
    }

    /**
     * Opens a new "OR WHERE (...)" grouping.
     *
     * @return
     */
    @Reflection.Signature
    fun orWhereOpen(): ObjectMemory{
        query.orWhereOpen()
        return objMem
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    @Reflection.Signature
    fun whereClose(): ObjectMemory{
        return andWhereClose();
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    @Reflection.Signature
    fun andWhereClose(): ObjectMemory{
        query.andWhereClose()
        return objMem
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    @Reflection.Signature
    fun orWhereClose(): ObjectMemory{
        query.orWhereClose()
        return objMem
    }

    /**
     * Creates a "GROUP BY ..." filter.
     *
     * @param   column  column name
     * @return
     */
    @Reflection.Signature
    fun groupBy(column: String): ObjectMemory{
        query.groupBy(column)
        return objMem
    }

    /**
     * Creates a "GROUP BY ..." filter.
     *
     * @param   columns  column name
     * @return
     */
    @Reflection.Signature
    fun groupBys(vararg columns: String): ObjectMemory{
        query.groupBys(*columns)
        return objMem
    }

    /**
     * Alias of andHaving()
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    @Reflection.Signature
    fun having(column: String, op: String, value: Any?): ObjectMemory{
        return andHaving(column, op, value);
    }

    /**
     * Creates a new "AND HAVING" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    @Reflection.Signature
    fun andHaving(column: String, op: String, value: Any?): ObjectMemory{
        query.andHaving(column, op, value)
        return objMem
    }

    /**
     * Creates a new "OR HAVING" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    @Reflection.Signature
    fun orHaving(column: String, op: String, value: Any?): ObjectMemory{
        query.orHaving(column, op, value)
        return objMem
    }

    /**
     * Alias of andHavingCondition()
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun havingCondition(condition: String, params: ArrayMemory? = null): ObjectMemory{
        return andHavingCondition(condition, params)
    }

    /**
     * Creates a new "AND HAVING" condition for the query.
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun andHavingCondition(condition: String, params: ArrayMemory? = null): ObjectMemory{
        query.andHavingCondition(condition, params.toPureList())
        return objMem
    }

    /**
     * Creates a new "OR HAVING" condition for the query.
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun orHavingCondition(condition: String, params: ArrayMemory? = null): ObjectMemory{
        query.orHavingCondition(condition, params.toPureList())
        return objMem
    }

    /**
     * Alias of andHavingOpen()
     *
     * @return
     */
    @Reflection.Signature
    fun havingOpen(): ObjectMemory{
        return andHavingOpen();
    }

    /**
     * Opens a new "AND HAVING (...)" grouping.
     *
     * @return
     */
    @Reflection.Signature
    fun andHavingOpen(): ObjectMemory{
        query.andHavingOpen()
        return objMem
    }

    /**
     * Opens a new "OR HAVING (...)" grouping.
     *
     * @return
     */
    @Reflection.Signature
    fun orHavingOpen(): ObjectMemory{
        query.orHavingOpen()
        return objMem
    }

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return
     */
    @Reflection.Signature
    fun havingClose(): ObjectMemory{
        return andHavingClose();
    }

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return
     */
    @Reflection.Signature
    fun andHavingClose(): ObjectMemory{
        query.andHavingClose()
        return objMem
    }

    /**
     * Closes an open "OR HAVING (...)" grouping.
     *
     * @return
     */
    @Reflection.Signature
    fun orHavingClose(): ObjectMemory{
        query.orHavingClose()
        return objMem
    }

    /**
     * Applies sorting with "ORDER BY ..."
     *
     * @param   column     column name or DbExpr
     * @param   direction  direction of sorting
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun orderBy(column: String, direction: String? = null): ObjectMemory{
        query.orderBy(column, direction)
        return objMem
    }

    /**
     * Multiple OrderBy
     *
     * @param orders
     * @return
     */
    @Reflection.Signature
    fun orderBys(orders: ArrayMemory): ObjectMemory{
        if(orders.isMap)
            query.orderBys(orders.toPureMap() as Map<String, String?>)
        else
            query.orderBys(*(orders.toPureArray() as Array<String>))
        return objMem
    }


    /**
     * Return up to "LIMIT ..." results
     *
     * @param  limit
     * @param  offset
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun limit(limit: Int, offset: Int = 0): ObjectMemory{
        query.limit(limit, offset)
        return objMem
    }

    /**
     * 设置查询加锁
     *
     * @return
     */
    @Reflection.Signature
    fun forUpdate(): ObjectMemory{
        query.forUpdate()
        return objMem
    }

    /****************************** 执行sql ********************************/
    /**
     * 查找一个： select ... limit 1语句
     *
     * @param params 参数
     * @return 一个数据
     */
    @Reflection.Signature
    @JvmOverloads
    public fun findRow(params: ArrayMemory? = null): Map<String, Any?>? {
        return query.findMap(params.toPureList())
    }

    /**
     * 查找全部： select ... 语句
     *
     * @param params 参数
     * @return 全部数据
     */
    @Reflection.Signature
    @JvmOverloads
    public fun findRows(params: ArrayMemory? = null): List<Map<String, Any?>> {
        return query.findMaps(params.toPureList())
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param params 参数
     * @return 一个数据
     */
    @Reflection.Signature
    @JvmOverloads
    public fun findModel(env: Environment, params: ArrayMemory? = null): PModel? {
        val item = ormQuery.findRow(params.toPureList()) {
            ormQuery.ormMeta.result2model<Orm>(it) // 此处需返回Orm对象，不能转为PModel对象，否则影响联查
        }
        if(item == null)
            return null
        return PModel.of(env, item)
    }

    /**
     * 查找多个： select 语句
     *
     * @param params 参数
     * @return 列表
     */
    @Reflection.Signature
    @JvmOverloads
    public fun findModels(env: Environment, params: ArrayMemory? = null): List<PModel> {
        val ormMeta = ormQuery.ormMeta
        val items = ormQuery.findRows(params.toPureList()) {
            ormMeta.result2model<Orm>(it) // 此处需返回Orm对象，不能转为PModel对象，否则影响联查
        }
        // 复用list
        return items.mapSelf {
            PModel.of(env, it)
        }
    }

    /**
     * 统计行数： count语句
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    public fun count(params: ArrayMemory? = null):Int{
        return query.count(params.toPureList())
    }

    /**
     * 加总列值： sum语句
     *
     * @param column 列
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    public fun sum(column: String, params: ArrayMemory? = null):Int{
        return query.sum(column, params.toPureList())
    }

    /**
     * 插入：insert语句
     *
     *  @param generatedColumn 返回的自动生成的主键名
     *  @param params 参数
     *  @param db 数据库连接
     * @return 新增的id
     */
    @Reflection.Signature
    @JvmOverloads
    public fun insert(generatedColumn:String?, params: ArrayMemory? = null): Long {
        val generatedColumn = if(generatedColumn.isNullOrBlank()) null else generatedColumn
        return query.insert(generatedColumn, params.toPureList())
    }

    /**
     * 更新：update语句
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    public fun update(params: ArrayMemory? = null): Boolean {
        return query.update(params.toPureList())
    }

    /**
     * 删除
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    public fun delete(params: ArrayMemory? = null): Boolean {
        return query.delete(params.toPureList())
    }

    /**
     * 自增
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    public fun incr(column: String, step: Int, params: ArrayMemory? = null): Boolean{
        return query.incr(column, step, params.toPureList())
    }

    /**
     * 批量插入
     *
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param db 数据库连接
     * @return
     */
    @Reflection.Signature
    public fun batchInsert(paramses: ArrayMemory): IntArray {
        return query.batchInsert(paramses.toPureList())
    }

    /**
     * 批量更新
     *
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param db 数据库连接
     * @return
     */
    @Reflection.Signature
    public fun batchUpdate(paramses: ArrayMemory): IntArray {
        return query.batchUpdate(paramses.toPureList())
    }

    /**
     * 批量插入
     *
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param db 数据库连接
     * @return
     */
    @Reflection.Signature
    public fun batchDelete(paramses: ArrayMemory): IntArray {
        return query.batchDelete(paramses.toPureList())
    }

    /****************************** OrmQueryBuilder实现 ********************************/
    /**
     * 联查单表
     *
     * @param name 关联关系名
     * @param columns 关联模型的字段列表
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    public fun with(name: String, columns: ArrayMemory? = null): ObjectMemory {
        if(columns != null && columns.size() > 0) {
            val cols = SelectColumnList(columns.toPureList() as List<String>)
            ormQuery.with(name, true, cols)
        }else {
            ormQuery.with(name)
        }
        return objMem
    }

    /**
     * 联查多表
     *
     * @param names 关联关系名的数组
     * @return
     */
    @Reflection.Signature
    public fun withs(vararg names: String): ObjectMemory {
        ormQuery.withs(*names)
        return objMem
    }

}