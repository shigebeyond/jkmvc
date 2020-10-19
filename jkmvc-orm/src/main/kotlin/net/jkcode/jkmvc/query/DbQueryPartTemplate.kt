package net.jkcode.jkmvc.query

import net.jkcode.jkmvc.db.IDb
import net.jkcode.jkutil.common.findAllGroupValue
import kotlin.reflect.KFunction3

/**
 * 基于模板替换参数的sql子句
 *   替换参数，如将行参表名替换为真实表名
 *   本来可以用 String.replace() 来做的, 但为了优化性能而手动解析与替换参数
 *
 * @author shijianhang
 * @date 2016-10-13
 */
class DbQueryPartTemplate(public val template: String) : IDbQueryPart{

    companion object{

        /**
         * 参数正则
         */
        public val paramRegx = "<([^\\>]+)>".toRegex()

        /**
         * 缓存参数填充方法
         */
        protected val paramsFillers: Map<String, KFunction3<DbQueryBuilderAction, IDb, StringBuilder, Unit>> = mapOf(
                "tables" to DbQueryBuilderAction::fillTables,
                "columns" to DbQueryBuilderAction::fillColumns,
                "values" to DbQueryBuilderAction::fillValues,
                "columnValues" to DbQueryBuilderAction::fillColumnValues,
                "distinct" to DbQueryBuilderAction::fillDistinct,
                "delTables" to DbQueryBuilderAction::fillDelTables
        )
    }

    /**
     * 文本部分
     */
    protected val texts: List<String> by lazy {
        paramRegx.split(template)
    }

    /**
     * 参数部分
     */
    protected val params: Collection<String> by lazy {
        paramRegx.findAllGroupValue(template, 1)
    }

    /**
     * 编译多个子表达式
     *
     * @param query 查询构建器
     * @param db 数据库连接
     * @param sql 保存编译sql
     */
    override fun compile(query: DbQueryBuilderDecoration, db: IDb, sql: StringBuilder) {
        // 替换参数: 表名/多个字段名/多个字段值
        /*sql = paramRegx.replace(template) { result: MatchResult ->
            val param = result.groupValues[1]
            callParamFiller(param, db)
        }*/
        sql.append(texts[0]) // 填充文本
        var i = 1
        for (param in params){
            callParamFiller(param, query, db, sql) // 填充参数
            sql.append(texts[i++]) // 填充文本
        }
    }

    /**
     * 调用参数对应方法, 来填充sql部分, 如表名/多个字段名/多个字段值
     *   如 fillTable() / fillColumns() / fillValues() / fillDistinct() / fillColumnValues()
     *
     * @param param 参数名
     * @param query 查询构建器
     * @param db 数据库连接
     * @param sql 保存编译sql
     */
    protected fun callParamFiller(param: String, query: DbQueryBuilderDecoration, db: IDb, sql: StringBuilder) {
        val method = paramsFillers[param]
        method?.call(query, db, sql);
    }

    /**
     * 清空
     * @return
     */
    override fun clear() {
        TODO("Not yet implemented")
    }

}