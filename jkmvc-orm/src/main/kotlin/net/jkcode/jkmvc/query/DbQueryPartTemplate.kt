package net.jkcode.jkmvc.query

import net.jkcode.jkmvc.db.IDb
import kotlin.math.min
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

/**
 * 基于模板替换参数的sql子句
 *   替换参数，如将行参表名替换为真实表名
 *   本来可以用 String.replace() 来做的, 但为了优化性能而手动解析与替换参数
 *
 * @author shijianhang
 * @date 2016-10-13
 */
class DbQueryPartTemplte(public val template: String) : IDbQueryPart{

    companion object{

        /**
         * 参数正则
         */
        public val paramRegx = "<([^\\>]+)>".toRegex()

        /**
         * 缓存字段填充方法
         */
        protected val fieldFillers: Map<String, KFunction2<DbQueryBuilderAction, IDb, String>> = mapOf(
                "table" to DbQueryBuilderAction::fillTable,
                "columns" to DbQueryBuilderAction::fillColumns,
                "values" to DbQueryBuilderAction::fillValues,
                "columnValues" to DbQueryBuilderAction::fillColumnValues,
                "distinct" to DbQueryBuilderAction::fillDistinct
        )
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
        paramRegx.replace(template) { result: MatchResult ->
            // 调用对应方法, 如 fillTable() / fillColumns() / fillValues() / fillDistinct() / fillColumnValues()
            val method = fieldFillers[result.groupValues[1]];
            method?.call(this, db).toString();
        }
    }

    /**
     * 清空
     * @return
     */
    override fun clear() {
        TODO("Not yet implemented")
    }

}