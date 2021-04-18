package net.jkcode.jkmvc.es.fuck

import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import java.util.*

class EsQuery {

    val query = QueryBuilders.boolQuery()

    val must: List<QueryBuilder> = LinkedList()
    val mustNot: List<QueryBuilder> = LinkedList()
    val should: List<QueryBuilder> = LinkedList()

    fun terms(){

    }

    fun range(){

    }

    fun fuzzy(){

    }

    fun query_string(){

    }

    fun missing(){

    }

    fun wild(){

    }

    fun prefix(){

    }



}