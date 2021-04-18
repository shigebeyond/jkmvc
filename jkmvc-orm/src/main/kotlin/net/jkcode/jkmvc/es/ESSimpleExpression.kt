package net.jkcode.jkmvc.es

import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders

import com.zhuanche.es.jest.ESCriterion.Operator

class ESSimpleExpression {
    private var fieldName: String? = null       //属性名
    private var value: Any? = null           //对应值
    private val values: Collection<Any>           //对应值
    private var operator: ESCriterion.Operator? = null      //计算符
    private val from: Any
    private val to: Any

    protected constructor(fieldName: String, value: Any, operator: Operator) {
        this.fieldName = fieldName
        this.value = value
        this.operator = operator
    }

    protected constructor(value: String, operator: Operator) {
        this.value = value
        this.operator = operator
    }

    constructor(fieldName: String, values: Collection<Any>) {
        this.fieldName = fieldName
        this.values = values
        this.operator = Operator.TERMS
    }

    constructor(fieldName: String, from: Any, to: Any) {
        this.fieldName = fieldName
        this.from = from
        this.to = to
        this.operator = Operator.RANGE
    }

    fun toBuilder(): QueryBuilder? {
        var qb: QueryBuilder? = null
        when (operator) {
            ESCriterion.Operator.TERM -> qb = QueryBuilders.termQuery(fieldName, value!!)
            ESCriterion.Operator.TERMS -> qb = QueryBuilders.termsQuery(fieldName, values)
            ESCriterion.Operator.RANGE -> qb = QueryBuilders.rangeQuery(fieldName).from(from).to(to).includeLower(true).includeUpper(true)
            ESCriterion.Operator.FUZZY -> qb = QueryBuilders.fuzzyQuery(fieldName, value!!)
            ESCriterion.Operator.WILD -> qb = QueryBuilders.wildcardQuery(fieldName, value!!.toString())
            ESCriterion.Operator.PREFIX -> qb = QueryBuilders.prefixQuery(fieldName, value!!.toString())
            ESCriterion.Operator.QUERY_STRING -> qb = QueryBuilders.queryStringQuery(value!!.toString())
        }

        return qb
    }
}