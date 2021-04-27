package net.jkcode.jkmvc.tests.es

import net.jkcode.jkmvc.es.ESQueryBuilder
import net.jkcode.jkmvc.es.EsManager
import org.junit.Test
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.builder.SearchSourceBuilder


/**
 * 聚合例子参考 https://www.cnblogs.com/xionggeclub/p/7975982.html
 */
class AggregationTests {

    private val index = "message_index"

    private val type = "_doc"

    private val esmgr = EsManager.instance()

    private val myBuilder = ESQueryBuilder()

    val nativebuilder = SearchSourceBuilder()

    @Test
    fun testMyCreateIndex() {
        // gson还是必须用双引号
        var mapping = """{
    '_doc':{
        'properties':{
            'name': {
                'index': 'not_analyzed',
                'type': 'string'
            },
            'age': {
                'type': 'integer'
            },
            'salary': {
                'type': 'integer'
            },
            'team': {
                'index': 'not_analyzed',
                'type': 'string'
            },
            'position': {
                'index': 'not_analyzed',
                'type': 'string'
            }
        }
    }
}"""
        // gson还是必须用双引号
        mapping = mapping.replace('\'', '"')
        println(mapping)
        var r = esmgr.createIndex(index)
        println("创建索引[$index]: " + r)
        r = esmgr.putMapping(index, type, mapping)
        println("设置索引[$index]映射[$type]: " + r)
    }

    /**
     * select team, count(*) as player_count from player group by team;
     */
    @Test
    fun testMy1(){
        val query = myBuilder
                .aggBy("team", "player_count")
        query.toSearchSource()
    }
    @Test
    fun testNative1(){
        val teamAgg = AggregationBuilders.terms("player_count ").field("team")
        nativebuilder.aggregation(teamAgg)
        println(nativebuilder.toString())
    }

    /**
     * select team, position, count(*) as pos_count from player group by team, position;
     */
    @Test
    fun testMy2(){
        val query = myBuilder
                    .aggByAndWrapSubAgg("team", "player_count") {
                        aggBy("position", "pos_count")
                    }
        query.toSearchSource()
    }
    @Test
    fun testNative2(){
        val teamAgg = AggregationBuilders.terms("player_count ").field("team")
        val posAgg = AggregationBuilders.terms("pos_count").field("position")
        nativebuilder.aggregation(teamAgg.subAggregation(posAgg))
        println(nativebuilder.toString())
    }

    /**
     * select team, max(age) as max_age from player group by team;
     */
    @Test
    fun testMy3(){
        val query = myBuilder
                    .aggByAndWrapSubAgg("team", "player_count") {
                        aggBy("max(age)")
                    }
        query.toSearchSource()
    }
    @Test
    fun testNative3(){
        val teamAgg = AggregationBuilders.terms("player_count ").field("team")
        val ageAgg = AggregationBuilders.max("max_age").field("age")
        nativebuilder.aggregation(teamAgg.subAggregation(ageAgg))
        println(nativebuilder.toString())
    }

    /**
     * select team, avg(age)as avg_age, sum(salary) as total_salary from player group by team;
     */
    @Test
    fun testMy4(){
        val query = myBuilder
                    .aggByAndWrapSubAgg("team") {
                        aggBy("avg(age)", "avg_age")
                        aggBy("sum(salary)", "total_salary")
                    }
        query.toSearchSource()
    }
    @Test
    fun testNative4(){
        val teamAgg = AggregationBuilders.terms("team")
        val ageAgg = AggregationBuilders.avg("avg_age").field("age")
        val salaryAgg = AggregationBuilders.sum("total_salary ").field("salary")
        nativebuilder.aggregation(teamAgg.subAggregation(ageAgg).subAggregation(salaryAgg))
        println(nativebuilder.toString())
    }

    /**
     * select team, sum(salary) as total_salary from player group by team order by total_salary desc;
     */
    @Test
    fun testMy5(){
        val query = myBuilder
                    .aggByAndWrapSubAgg("team") {
                        aggBy("sum(salary)", "total_salary", false)
                    }
        query.toSearchSource()
    }
    @Test
    fun testNative5(){
        val teamAgg = AggregationBuilders.terms("team").order(Terms.Order.aggregation("total_salary ", false))
        val salaryAgg = AggregationBuilders.avg("total_salary ").field("salary");
        nativebuilder.aggregation(teamAgg.subAggregation(salaryAgg));
        println(nativebuilder.toString())
    }

    @Test
    fun testMy6(){
        val query = myBuilder
                .aggByAndWrapSubAgg("team") {
                    aggBy("count(position)")
                    aggBy("sum(salary)")
                    aggByAndWrapSubAgg("nested(game)") {
                        aggBy("sum(game.score)", null, false)
                    }
                }
        query.toSearchSource()
    }

    /**
     * https://blog.csdn.net/z327092292/article/details/95203647
     */
    @Test
    fun testMy7(){
        /*val query = myBuilder
                .aggByAndWrapSubAgg("nested(wordFrequency)", "wordgroup") {
                    aggByAndWrapSubAgg("wordFrequency.keyword", "word"){
                        aggBy("sum(wordFrequency.count)", "wordnum", false)
                    }
                }*/
        val query = myBuilder
                .aggByAndWrapSubAgg("nested(wordFrequency)") {
                    aggByAndWrapSubAgg("wordFrequency.keyword"){
                        aggBy("sum(wordFrequency.count)", null, false)
                    }
                }
        query.toSearchSource()
    }
}