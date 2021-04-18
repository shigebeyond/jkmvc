package net.jkcode.jkmvc.es

import org.elasticsearch.index.query.QueryBuilder

import java.util.ArrayList

class ESQueryBuilders : ESCriterion {

    private val list = ArrayList<QueryBuilder>()

    /**
     * 功能描述：Term 查询
     * @param field 字段名
     * @param value 值
     */
    fun term(field: String, value: Any): ESQueryBuilders {
        list.add(ESSimpleExpression(field, value, ESCriterion.Operator.TERM).toBuilder())
        return this
    }

    /**
     * 功能描述：Terms 查询
     * @param field 字段名
     * @param values 集合值
     */
    fun terms(field: String, values: Collection<Any>): ESQueryBuilders {
        list.add(ESSimpleExpression(field, values).toBuilder())
        return this
    }

    /**
     * 功能描述：fuzzy 查询
     * @param field 字段名
     * @param value 值
     */
    fun fuzzy(field: String, value: Any): ESQueryBuilders {
        list.add(ESSimpleExpression(field, value, ESCriterion.Operator.FUZZY).toBuilder())
        return this
    }

    /**
     * 功能描述：Range 查询
     * @param from 起始值
     * @param to 末尾值
     */
    fun range(field: String, from: Any, to: Any): ESQueryBuilders {
        list.add(ESSimpleExpression(field, from, to).toBuilder())
        return this
    }

    /**
     * 功能描述：Range 查询
     * @param queryString 查询语句
     */
    fun queryString(queryString: String): ESQueryBuilders {
        list.add(ESSimpleExpression(queryString, ESCriterion.Operator.QUERY_STRING).toBuilder())
        return this
    }

    /**
     * 功能描述：wild 查询
     * @param value 查询语句
     */
    fun wildString(field: String, value: String): ESQueryBuilders {
        list.add(ESSimpleExpression(field, value, ESCriterion.Operator.WILD).toBuilder())
        return this
    }

    /**
     * 功能描述：prefix 查询
     * @param value 查询语句
     */
    fun prefixString(field: String, value: String): ESQueryBuilders {
        list.add(ESSimpleExpression(field, value, ESCriterion.Operator.PREFIX).toBuilder())
        return this
    }


    override fun listBuilders(): List<QueryBuilder> {
        return list
    }
}