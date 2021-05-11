package net.jkcode.jkmvc.query

import net.jkcode.jkmvc.db.DbResultRow
import net.jkcode.jkmvc.db.IDb
import net.jkcode.jkmvc.orm.*

/**
 * sql构建器
 * 1 作用
 *   依次继承 DbQueryBuilderAction 处理动作子句 + DbQueryBuilderDecoration 处理修饰子句
 *   提供select/where等类sql的方法, 但是调用方法时, 不直接拼接sql, 而是在compile()时才延迟拼接sql, 因为调用方法时元素可以无序, 但生成sql时元素必须有序
 *
 * 2 CharSequence接口
 *   为了适配 DbQueryBuilder 中的查询方法的查询参数类型, 如 select() / where()
 *   否则要重载很多方法来接收 DbExpr 参数
 *
 * @author shijianhang
 * @date 2016-10-13
 */
abstract class IDbQueryBuilder: IDbQueryBuilderQuoter, IDbQueryBuilderAction, IDbQueryBuilderDecoration, CharSequence by "", IDbQuery() {

    /**
     * 克隆对象: 单纯用于改权限为public
     *
     * @return o
     */
    public override fun clone(): Any{
        return super.clone()
    }

    /**
     * 克隆对象, 同clone(), 只转换下返回类型为 IDbQueryBuilder
     *
     * @param clearSelect 清空select参数
     * @return o
     */
    public abstract fun copy(clearSelect: Boolean = false): IDbQueryBuilder

    /****************************** 编译sql ********************************/
    /**
     * 编译sql
     *
     * @param action sql动作：select/insert/update/delete
     * @param db 数据库连接
     * @param clone 是否克隆, 默认是
     * @return 编译好的sql
     */
    public abstract fun compile(action: SqlAction, db: IDb = defaultDb, clone: Boolean = true): CompiledSql;

    /**
     * 编译select语句
     *
     * @param db 数据库连接
     * @return 编译好的sql
     */
    public fun compileSelect(db: IDb = defaultDb): CompiledSql {
        return compile(SqlAction.SELECT, db)
    }

    /**
     * 编译select ... limit 1语句
     *
     * @param db 数据库连接
     * @return 编译好的sql
     */
    public fun compileSelectOne(db: IDb = defaultDb): CompiledSql {
        return limit(1).compile(SqlAction.SELECT, db)
    }

    /**
     * 编译select count() 语句
     *
     * @param db 数据库连接
     * @return 编译好的sql
     */
    public fun compileCount(db: IDb = defaultDb): CompiledSql {
        return select(DbExpr("count(1)", "NUM", false) /* oracle会自动转为全大写 */).compile(SqlAction.SELECT, db)
    }

    /**
     * 编译insert语句
     *
     * @param db 数据库连接
     * @return 编译好的sql
     */
    public fun compileInsert(db: IDb = defaultDb): CompiledSql {
        return compile(SqlAction.INSERT, db)
    }

    /**
     * 编译update语句
     *
     * @param db 数据库连接
     * @return 编译好的sql
     */
    public fun compileUpdate(db: IDb = defaultDb): CompiledSql {
        return compile(SqlAction.UPDATE, db)
    }

    /**
     * 编译delete语句
     *
     * @param db 数据库连接
     * @return 编译好的sql
     */
    public fun compileDelete(db: IDb = defaultDb): CompiledSql {
        return compile(SqlAction.DELETE, db)
    }

    /****************************** 执行sql ********************************/
    /**
     * 统计行数： count语句
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    public abstract fun count(params: List<*> = emptyList<Any>(), db: IDb = defaultDb):Int;

    /**
     * 加总列值： sum语句
     *
     * @param column 列
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    public abstract fun sum(column: String, params: List<*> = emptyList<Any>(), db: IDb = defaultDb):Int;

    /**
     * 编译 + 执行
     *
     * @param action sql动作：select/insert/update/delete
     * @param params 参数
     * @param generatedColumn 返回自动生成的主键名
     * @param db 数据库连接
     * @return 影响行数|新增id
     */
    public abstract fun execute(action: SqlAction, params:List<Any?> = emptyList(), generatedColumn:String? = null, db: IDb = defaultDb): Long;

    /**
     * 插入：insert语句
     *
     *  @param generatedColumn 返回的自动生成的主键名
     *  @param params 参数
     *  @param db 数据库连接
     * @return 新增的id
     */
    public fun insert(generatedColumn:String? = null, params: List<*> = emptyList<Any>(), db: IDb = defaultDb): Long {
        return execute(SqlAction.INSERT, params, generatedColumn, db);
    }

    /**
     * 更新：update语句
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    public fun update(params: List<*> = emptyList<Any>(), db: IDb = defaultDb): Boolean {
        return execute(SqlAction.UPDATE, params, null, db) > 0;
    }

    /**
     * 删除
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    public fun delete(params: List<*> = emptyList<Any>(), db: IDb = defaultDb): Boolean {
        return execute(SqlAction.DELETE, params, null, db) > 0;
    }

    /**
     * 自增
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    public abstract fun incr(column: String, step: Int = 1, params: List<*> = emptyList<Any>(), db: IDb = defaultDb): Boolean

    /**
     * 批量更新有参数的sql
     *
     * @param action sql动作：select/insert/update/delete
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param db 数据库连接
     * @return
     */
    public abstract fun batchExecute(action: SqlAction, paramses: List<Any?>, db: IDb = defaultDb): IntArray;

    /**
     * 批量插入
     *
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param db 数据库连接
     * @return
     */
    public fun batchInsert(paramses: List<Any?>, db: IDb = defaultDb): IntArray {
        return batchExecute(SqlAction.INSERT, paramses, db)
    }

    /**
     * 批量更新
     *
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param db 数据库连接
     * @return
     */
    public fun batchUpdate(paramses: List<Any?>, db: IDb = defaultDb): IntArray {
        return batchExecute(SqlAction.UPDATE, paramses, db)
    }

    /**
     * 批量插入
     *
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param db 数据库连接
     * @return
     */
    public fun batchDelete(paramses: List<Any?>, db: IDb = defaultDb): IntArray {
        return batchExecute(SqlAction.DELETE, paramses, db)
    }

    /****************************** 优化分页查询 ********************************/
    /**
     * 通过select id子查询来加快分页查询
     *
     * @param limit
     * @param offset
     * @param idField id字段名
     * @param whereOnlyIds 外部查询的查询条件只保留id条件, 去掉其他旧条件, 仅适用于id字段, 非id字段可能要保留旧条件
     * @param params 参数
     * @param db 数据库连接
     * @param transform 行转换函数
     * @return 列表
     */
    public abstract fun <T:Any> fastFindPageBySubquery(limit: Int, offset: Int, idField: String, whereOnlyIds: Boolean = true, params: List<*> = emptyList<Any>(), db: IDb = defaultDb, transform: (DbResultRow) -> T): List<T>

    /**
     * 通过select id子查询来加快分页查询
     *
     * @param limit
     * @param offset
     * @param idField id字段名
     * @param whereOnlyIds 外部查询的查询条件只保留id条件, 去掉其他旧条件, 仅适用于id字段, 非id字段可能要保留旧条件
     * @param params 参数
     * @param convertingColumn 是否转换字段名
     * @param db 数据库连接
     * @return 列表
     */
    public fun fastFindPageMaps(limit: Int, offset: Int, idField: String, whereOnlyIds: Boolean = true, params: List<*> = emptyList<Any>(), convertingColumn: Boolean = false, db: IDb = defaultDb): List<Map<String, Any?>>{
        return fastFindPageBySubquery(limit, offset, idField, whereOnlyIds, params, db){ row ->
            row.toMap(convertingColumn)
        }
    }

    /**
     * 通过select id子查询来加快分页查询
     *
     * @param limit
     * @param offset
     * @param idField id字段名
     * @param whereOnlyIds 外部查询的查询条件只保留id条件, 去掉其他旧条件, 仅适用于id字段, 非id字段可能要保留旧条件
     * @param params 参数
     * @param db 数据库连接
     * @return 列表
     */
    public inline fun <reified T: IOrm> fastFindPageModels(limit: Int, offset: Int, idField: String, whereOnlyIds: Boolean = true, params: List<*> = emptyList<Any>(), db: IDb = defaultDb): List<T> {
        return fastFindPageBySubquery(limit, offset, idField, whereOnlyIds, params, db, T::class.modelRowTransformer)
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param limit
     * @param offset
     * @param idField id字段名
     * @param whereOnlyIds 外部查询的查询条件只保留id条件, 去掉其他旧条件, 仅适用于id字段, 非id字段可能要保留旧条件
     * @param params 参数
     * @param db 数据库连接
     * @return 一个数据
     */
    public inline fun <reified T: IEntitiableOrm<E>, reified E: OrmEntity> fastFindPageEntities(limit: Int, offset: Int, idField: String, whereOnlyIds: Boolean = true, params: List<*> = emptyList<Any>(), db: IDb = defaultDb): List<E> {
        return fastFindPageBySubquery(limit, offset, idField, whereOnlyIds, params, db, T::class.entityRowTransformer())
    }

}