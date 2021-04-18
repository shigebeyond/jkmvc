package net.jkcode.jkmvc.es

import org.apache.commons.collections4.CollectionUtils
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortOrder

import java.util.ArrayList
import java.util.HashMap

/**
 * 查询条件
 */
class ESQueryBuilderConstructor {

    var size = Integer.MAX_VALUE

    var from = 0

    var sorts: MutableMap<String, SortOrder>? = null

    //查询条件容器
    private val mustCriterions = ArrayList<ESCriterion>()

    private val shouldCriterions = ArrayList<ESCriterion>()

    private val mustNotCriterions = ArrayList<ESCriterion>()

    var includeFields: Array<String>? = null
    var excludeFields: Array<String>? = null

    //构造builder
    fun listBuilders(): QueryBuilder? {
        val count = mustCriterions.size + shouldCriterions.size + mustNotCriterions.size
        val boolQueryBuilder = QueryBuilders.boolQuery()
        var queryBuilder: QueryBuilder? = null

        if (count >= 1) {
            //must容器
            if (!CollectionUtils.isEmpty(mustCriterions)) {
                for (criterion in mustCriterions) {
                    for (builder in criterion.listBuilders()) {
                        queryBuilder = boolQueryBuilder.must(builder)
                    }
                }
            }
            //should容器
            if (!CollectionUtils.isEmpty(shouldCriterions)) {
                for (criterion in shouldCriterions) {
                    for (builder in criterion.listBuilders()) {
                        queryBuilder = boolQueryBuilder.should(builder)
                    }

                }
            }
            //must not 容器
            if (!CollectionUtils.isEmpty(mustNotCriterions)) {
                for (criterion in mustNotCriterions) {
                    for (builder in criterion.listBuilders()) {
                        queryBuilder = boolQueryBuilder.mustNot(builder)
                    }
                }
            }
            return queryBuilder
        } else {
            return null
        }
    }

    /**
     * 增加简单条件表达式
     */
    fun must(criterion: ESCriterion?): ESQueryBuilderConstructor {
        if (criterion != null) {
            mustCriterions.add(criterion)
        }
        return this
    }

    /**
     * 增加简单条件表达式
     */
    fun should(criterion: ESCriterion?): ESQueryBuilderConstructor {
        if (criterion != null) {
            shouldCriterions.add(criterion)
        }
        return this
    }

    /**
     * 增加简单条件表达式
     */
    fun mustNot(criterion: ESCriterion?): ESQueryBuilderConstructor {
        if (criterion != null) {
            mustNotCriterions.add(criterion)
        }
        return this
    }

    fun addSort(field: String, sort: SortOrder): ESQueryBuilderConstructor {
        if (this.sorts == null) {
            sorts = HashMap(4)
        }
        sorts!![field] = sort
        return this
    }
}