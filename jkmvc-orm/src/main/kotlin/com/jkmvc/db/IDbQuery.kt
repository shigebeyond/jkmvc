package com.jkmvc.db

import com.jkmvc.orm.IOrm
import com.jkmvc.orm.rowTranformer
import org.apache.commons.collections.map.HashedMap
import kotlin.reflect.KClass

/**
 * 查询sql执行
 *
 * 1 db的动态性
 *   所有sql执行方法的最后一个参数都是 db 对象, 每次执行sql都需要指定一个 db, 这是为了抽取子类的共同需求
 *   1.1 DbQueryBuilder 需要递延指定 db, 在最后要编译与执行sql时才指定, 方便实现按需选择不同的连接, 如读写分离
 *   1.2 CompiledSql 只为缓存编译的sql, 跨请求的存在, 不可能预先确定 db
 *
 * 2 db的预设性
 *   在Orm模型中, db是预先定义在元数据中, 以后执行sql也不需要你重新指定db
 *   => 抽象属性 defaultDb 来记录预设的 db
 *
 * 3 注：为什么不是接口，而是抽象类？
 *    因为我需要实现几个 inline 方法, 这几个方法都需要具体化泛型，因此需要内联实现inline，但是inline不能用于接口方法/抽象方法，因此我直接在该类中实现这两个方法，该类也只能由接口变为抽象类
 */
abstract class IDbQuery{

    /**
     * 默认db名
     *   用在sql执行方法中最后一个参数中, 默认使用该属性对应的db
     */
    public abstract val defaultDb: IDb

    /**
     * 查找多个： select 语句
     *
     * @param params 参数
     * @param db 数据库连接
     * @param transform 转换函数
     * @return 列表
     */
    public abstract fun <T:Any> findAll(params: List<Any?> = emptyList(), db: IDb = defaultDb, transform: (Row) -> T): List<T>

    /**
     * 查找多个： select 语句
     *
     * @param params 参数
     * @param db 数据库连接
     * @return 列表
     */
    public fun findAllRows(params: List<Any?> = emptyList(), db: IDb = defaultDb): List<Row>{
        return findAll(params, db, ::HashedMap) as List<Row>
    }

    /**
     * 查找多个： select 语句
     *
     * @param params 参数
     * @param db 数据库连接
     * @return 列表
     */
    public inline fun <reified T: IOrm> findAllModels(params: List<Any?> = emptyList(), db: IDb = defaultDb): List<T> {
        return findAll(params, db, T::class.rowTranformer)
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param params 参数
     * @param db 数据库连接
     * @param transform 转换函数
     * @return 一个数据
     */
    public fun findRow(params: List<Any?> = emptyList(), db: IDb = defaultDb): Row?{
        return find(params, db, ::HashedMap) as Row?
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param params 参数
     * @param db 数据库连接
     * @param transform 转换函数
     * @return 一个数据
     */
    public abstract fun <T:Any> find(params: List<Any?> = emptyList(),  db: IDb = defaultDb, transform: (Row) -> T): T?

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param params 参数
     * @param db 数据库连接
     * @return 一个数据
     */
    public inline fun <reified T: IOrm> findModel(params: List<Any?> = emptyList(), db: IDb = defaultDb): T? {
        return find(params, db, T::class.rowTranformer)
    }

    /**
     * 查询一列（多行）
     *
     * @param params 参数
     * @param clazz 值类型
     * @param db 数据库连接
     * @return
     */
    public abstract fun <T:Any> findColumn(params: List<Any?> = emptyList(), clazz: KClass<T>? = null, db: IDb = defaultDb): List<T?>

    /**
     * 查询一列（多行）
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    public inline fun <reified T:Any> findColumn(params: List<Any?> = emptyList(), db: IDb = defaultDb): List<T?> {
        return findColumn(params, T::class, db)
    }

    /**
     * 查询一行一列
     *
     * @param params 参数
     * @param clazz 值类型
     * @param db 数据库连接
     * @return
     */
    public abstract fun <T:Any> findCell(params: List<Any?> = emptyList(), clazz: KClass<T>? = null, db: IDb = defaultDb): Cell<T>

    /**
     * 查询一行一列
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    public inline fun <reified T:Any> findCell(params: List<Any?> = emptyList(), db: IDb = defaultDb): Cell<T> {
        return findCell(params, T::class, db)
    }

}