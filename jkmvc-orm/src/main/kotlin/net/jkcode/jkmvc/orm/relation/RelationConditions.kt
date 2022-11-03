package net.jkcode.jkmvc.orm.relation

import net.jkcode.jkmvc.orm.OrmQueryBuilder

/**
 * 关联关系的联查条件
 * @author shijianhang<772910474@qq.com>
 * @date 2020-06-26 9:52 AM
 */
data class RelationConditions(
        public val conditions :Map<String, Any?>, // 查询条件
        public var queryAction: ((query: OrmQueryBuilder, lazy: Boolean)->Unit)? // 查询对象的回调函数
){
    companion object{

        /**
         * 空条件
         */
        val EmptyConditions = RelationConditions(emptyMap(), null)
    }

    public val size: Int
        get() = conditions.size + if(queryAction == null) 0 else 1

    /**
     * 添加回调函数
     *   即合并2个回调函数
     * @param nextAction 下个回调函数
     */
    public fun addQueryAction(nextAction: (query: OrmQueryBuilder, lazy: Boolean)->Unit){
        if(queryAction == null) {
            queryAction = nextAction
            return
        }

        // 合并2个回调函数
        val preAction = queryAction
        queryAction = { query, lazy ->
            preAction!!(query, lazy)
            nextAction(query, lazy)
        }
    }

    /**
     * 对query builder应用联查
     * @param query
     * @param lazy 是否延迟加载, 分2条sql, 否则同一条sql
     */
    public fun applyQuery(query: OrmQueryBuilder, lazy: Boolean) {
        if(lazy)
            query.wheres(conditions)
        else
            query.ons(conditions, false)

        queryAction?.invoke(query, lazy)
    }

}