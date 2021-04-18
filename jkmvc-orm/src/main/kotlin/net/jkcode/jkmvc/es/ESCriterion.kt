package net.jkcode.jkmvc.es

import org.elasticsearch.index.query.QueryBuilder

/**
 * 查询条件
 */
interface ESCriterion {
    enum class Operator {
        TERM, TERMS, RANGE, FUZZY, QUERY_STRING, MISSING, WILD, PREFIX
    }

    fun listBuilders(): List<QueryBuilder>
}
