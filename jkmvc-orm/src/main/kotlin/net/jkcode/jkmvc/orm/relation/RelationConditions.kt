package net.jkcode.jkmvc.orm.relation

import net.jkcode.jkmvc.orm.OrmQueryBuilder

/**
 * 关联关系的联查条件
 * @author shijianhang<772910474@qq.com>
 * @date 2020-06-26 9:52 AM
 */
data class RelationConditions(
        public val conditions :Map<String, Any?>, // 查询条件
        public val queryAction: ((OrmQueryBuilder)->Unit)? // 查询对象的回调函数
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
     * 对query builder应用联查
     * @param query
     */
    public fun applyQuery(query: OrmQueryBuilder) {
        query.ons(conditions, false)
        queryAction?.invoke(query)
    }

}