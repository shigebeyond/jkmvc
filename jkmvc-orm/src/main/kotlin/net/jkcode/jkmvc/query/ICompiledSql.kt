package net.jkcode.jkmvc.query

import net.jkcode.jkmvc.db.IDb
import java.util.*

/**
 * 编译好的sql
 *
 * @author shijianhang
 * @date 2017-6-10 下午8:02:47
 */
abstract class ICompiledSql: IDbQuery() {

    /****************************** 构建参数/sql *******************************/
    /**
     * 编译好的sql
     */
    public abstract var sql: String

    /**
     * 编译后的sql参数 / 静态参数
     */
    public abstract val staticParams: LinkedList<Any?>

    /**
     * 动态参数的个数 = 静态参数中?的个数
     */
    public abstract val dynamicParamsSize:Int;

    /**
     * 判断是否为空
     * @return
     */
    public abstract fun isEmpty(): Boolean

    /**
     * 清空编译结果
     * @return
     */
    public abstract fun clear(): ICompiledSql

    /**
     * 构建实际参数 = 静态参数 + 动态参数
     *
     * @param dynamicParams 动态参数
     * @return
     */
    public abstract fun buildParams(dynamicParams: List<*> = emptyList<Any>()): List<Any?>

    /**
     * 构建实际参数 = 静态参数 + 动态参数
     *
     * @param dynamicParams 动态参数
     * @return
     */
    public fun buildParams(dynamicParams: Array<out Any?>): List<Any?>{
        return buildParams(dynamicParams.asList())
    }

    /**
     * 构建实际参数 = 静态参数 + 动态参数
     *
     * @param dynamicParamses 多次处理的动态参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @return
     */
    public abstract fun buildBatchParamses(dynamicParamses: List<Any?>):List<Any?>;

    /**
     * 预览sql
     *
     * @param dynamicParams 动态参数
     * @param fromIndex 动态参数的开始索引
     * @param db 数据库连接
     * @return
     */
    public abstract fun previewSql(dynamicParams:List<Any?> = emptyList(), fromIndex:Int = 0, db: IDb = defaultDb): String

    /****************************** 执行sql *******************************/
    /**
     * 编译 + 执行
     *
     * @param params 动态参数
     * @param generatedColumn 返回的自动生成的主键名
     * @return 影响行数|新增id
     */
    public abstract fun execute(params: List<*>, generatedColumn:String?, db: IDb): Long

    /**
     * 批量更新有参数的sql
     *
     * @param action sql动作：select/insert/update/delete
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @return
     */
    public abstract fun batchExecute(paramses: List<Any?>, db: IDb): IntArray
}