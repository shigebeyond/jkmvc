package net.jkcode.jkmvc.tests.es

import net.jkcode.jkmvc.es.*
import net.jkcode.jkmvc.tests.entity.Game
import net.jkcode.jkmvc.tests.entity.Player
import net.jkcode.jkutil.common.randomInt
import org.junit.Test
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.builder.SearchSourceBuilder
import java.util.ArrayList


/**
 * 聚合例子参考 https://www.cnblogs.com/xionggeclub/p/7975982.html
 */
class AggregationTests {

    private val index = "player_index"

    private val type = "_doc"

    private val esmgr = EsManager.instance()

    private val myBuilder = ESQueryBuilder().index(index).type(type)

    val nativebuilder = SearchSourceBuilder()

    @Test
    fun testAll() {
        testDeleteIndex()
        testCreateIndex()
        testBulkIndexDocs()
        testSearch()
    }

    @Test
    fun testCreateIndex() {
        // gson还是必须用双引号
        var mapping = """{
    '_doc':{
        'properties':{
            'id' : {
                'type' : 'integer'
            },
            'name': {
                'type': 'keyword'
            },
            'age': {
                'type': 'integer'
            },
            'salary': {
                'type': 'integer'
            },
            'team': {
                'type': 'keyword'
            },
            'position': {
                'type': 'keyword'
            },
            'games': {
                'type': 'nested',
                'properties' : {
                    'id' : {
                      'type' : 'integer'
                    },
                    'title' : {
                      'type' : 'text'
                    },
                    'score' : {
                      'type' : 'integer'
                    }
                }
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

    @Test
    fun testDeleteIndex() {
        //删除
        val r = esmgr.deleteIndex(index)
        System.out.println("删除索引[$index]：" + r)
    }

    @Test
    fun testBulkIndexDocs() {
        val items = ArrayList<Player>()

        for (i in 0 until 5) {
            val e = buildPlayer(i)
            items.add(e)
        }

        esmgr.bulkIndexDocs(index, type, items)
        println("批量插入")
    }

    @Test
    fun testSearch() {
        val (list, size) = myBuilder.searchDocs(HashMap::class.java)
        println("查到 $size 个文档")
        for (item in list)
            println(item)
    }

    private fun buildPlayer(i: Int): Player {
        val e = Player()
        e.id = i + 1
        e.name = arrayOf("张三", "李四", "王五", "赵六", "钱七")[i]
        e.team = arrayOf("骑士", "湖人").random()
        e.position = arrayOf("前锋", "后卫").random()
        e.age = randomInt(30)
        e.salary = (randomInt(5) + 1) * 1000
        e.games = (0..1).map {i ->
            buildGame(i+1)
        }
        return e
    }

    private fun buildGame(i: Int): Game {
        val e = Game()
        e.id = i
        e.title = "第${i}比赛"
        e.score = randomInt(100)
        return e
    }

    /**
     * select count(1), count(position) as count_position, max(age) as max_age, team from player_index group by team;
     */
    @Test
    fun testMy0(){
        val query = myBuilder
                .aggByAndWrapSubAgg("team") { // 别名是team
                    aggBy("count(position)") // 别名是count_position
                    aggBy("max(age)") // 别名是max_age
                }
        println(query.toSearchSource(false))
    }
    @Test
    fun testNative0(){
        val teamAgg = AggregationBuilders.terms("team ").field("team")
        val posAgg = AggregationBuilders.terms("pos_count").field("position")
        val ageAgg = AggregationBuilders.max("max_age").field("age")
        nativebuilder.aggregation(teamAgg.subAggregation(posAgg).subAggregation(ageAgg))
        println(nativebuilder.toString())
    }

    /**
     * select team, count(*) as player_count from player group by team;
     */
    @Test
    fun testMy1(){
        val query = myBuilder
                //.aggBy("team", "player_count")
                .aggBy("team") // 别名是team
        println(query.toSearchSource(false))
    }
    @Test
    fun testNative1(){
        val teamAgg = AggregationBuilders.terms("player_count ").field("team")
        nativebuilder.aggregation(teamAgg)
        println(nativebuilder.toString())
    }

    /**
     * select team, count(*) as pos_count from player group by team, position;
     */
    @Test
    fun testMy2(){
        val query = myBuilder
                    /*.aggByAndWrapSubAgg("team", "player_count") {
                        aggBy("position", "pos_count")
                    }*/
                    .aggByAndWrapSubAgg("team") { // 别名是team
                        aggBy("count(position)") // 别名是count_position
                    }
        println(query.toSearchSource(false))
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
                    /*.aggByAndWrapSubAgg("team", "player_count") {
                        aggBy("max(age)", "max_age")
                    }*/
                    .aggByAndWrapSubAgg("team") { // 别名是team
                        aggBy("max(age)") // 别名是max_age
                    }
        println(query.toSearchSource(false))
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
                    /*.aggByAndWrapSubAgg("team") {
                        aggBy("avg(age)", "avg_age")
                        aggBy("sum(salary)", "total_salary")
                    }*/
                    .aggByAndWrapSubAgg("team") { // 别名是team
                        aggBy("avg(age)") // 别名是avg_age
                        aggBy("sum(salary)") // 别名是sum_salary
                    }
        println(query.toSearchSource(false))
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
                        aggBy("sum(salary)", "total_salary", false) // 降序
                    }
        println(query.toSearchSource(false))
    }
    @Test
    fun testNative5(){
        val teamAgg = AggregationBuilders.terms("team").order(Terms.Order.aggregation("total_salary ", false))
        val salaryAgg = AggregationBuilders.avg("total_salary ").field("salary");
        nativebuilder.aggregation(teamAgg.subAggregation(salaryAgg));
        println(nativebuilder.toString())
    }

    @Test
    fun testNative6(){
        // 每个队伍 -- select count(position), sum(salary), sum(games.score), team from player_index group by team;
        val teamAgg = AggregationBuilders.terms("team").order(Terms.Order.aggregation("sum_games_score", false))
        val cardinalityPositionAgg = AggregationBuilders.cardinality("cardinality_position ").field("position");
        val sumSalaryAgg = AggregationBuilders.sum("sum_salary").field("salary");
        teamAgg.subAggregation(cardinalityPositionAgg)
        teamAgg.subAggregation(sumSalaryAgg)
        // 子文档
        val nestedGamesAgg1 = AggregationBuilders.nested("games", "games")
        val sumScoreAgg1 = AggregationBuilders.sum("sum_games_score").field("games.score");
        nestedGamesAgg1.subAggregation(sumScoreAgg1)
        teamAgg.subAggregation(nestedGamesAgg1)

        // 每个队伍+职位 -- select avg(age), team, position from player_index group by team, position;
        val subPositionAgg = AggregationBuilders.terms("position")
        val avgAgeAgg = AggregationBuilders.avg("avg_age").field("age");
        subPositionAgg.subAggregation(avgAgeAgg)
        teamAgg.subAggregation(subPositionAgg)

        // 每个职位 -- select avg(salary), sum(games.score), position from player_index group by position; -- sum(games.score)不能执行
        val positionAgg = AggregationBuilders.terms("position").order(Terms.Order.aggregation("sum_games_score", false))
        val avgSalaryAgg = AggregationBuilders.avg("avg_salary").field("salary");
        positionAgg.subAggregation(avgSalaryAgg)
        // 子文档
        val nestedGamesAgg2 = AggregationBuilders.nested("games", "games")
        val sumScoreAgg2 = AggregationBuilders.sum("sum_games_score").field("games.score");
        nestedGamesAgg2.subAggregation(sumScoreAgg2)
        positionAgg.subAggregation(nestedGamesAgg2)

        // 每场比赛 -- select sum(games.score) from  player_index group by games.id
        val nestedGamesAgg3 = AggregationBuilders.nested("games", "games")
        val subGameIdAgg = AggregationBuilders.terms("games.id").order(Terms.Order.aggregation("sum_games_score", false))
        val sumScoreAgg3 = AggregationBuilders.sum("sum_games_score").field("games.score");
        subGameIdAgg.subAggregation(sumScoreAgg3)
        nestedGamesAgg3.subAggregation(subGameIdAgg)

        nativebuilder.aggregation(teamAgg);
        nativebuilder.aggregation(positionAgg);
        nativebuilder.aggregation(nestedGamesAgg3);
        println(nativebuilder.toString())
    }

    @Test
    fun testMy6(){
        val query = myBuilder
                .aggByAndWrapSubAgg("team") { // 每个队伍 -- select count(position), sum(salary), sum(games.score), team from player_index group by team;
                    aggBy("cardinality(position)") // 每个队伍总职位数
                    aggBy("sum(salary)") // 每个队伍的总薪酬
                    aggByAndWrapSubAgg("nested(games)") { // 每个队伍的总分 -- 嵌套文档
                        aggBy("sum(games.score)", null, false)
                    }
                    aggByAndWrapSubAgg("position"){ // 每个队伍+职位 -- select avg(age), team, position from player_index group by team, position;
                        aggBy("avg(age)")
                    }
                }
                .aggByAndWrapSubAgg("position") { // 每个职位 -- select avg(salary), sum(games.score), position from player_index group by position; -- sum(games.score)不能执行
                    aggBy("avg(salary)") // 每个职位的平均薪酬
                    aggByAndWrapSubAgg("nested(games)") { // 每个职位的总分 -- 嵌套文档
                        aggBy("sum(games.score)", null, false)
                    }
                }
                .aggByAndWrapSubAgg("nested(games)") { // 每场比赛 -- select sum(games.score) from  player_index group by games.id
                    aggByAndWrapSubAgg("games.id"){ // 每场比赛的总分 -- 嵌套文档 https://blog.csdn.net/z327092292/article/details/95203647
                        aggBy("sum(games.score)", null, false)
                    }
                }
        println(query.toSearchSource(false))

        val result = query.searchDocs()

        // 每个队伍 -- select count(position), sum(salary), team from player_index group by team;
//        val teamRows = result.aggregations.flattenAggRows("team")
//        println("统计每个队伍:" + teamRows)

        // 每个队伍 -- select count(position), sum(salary), sum(games.score), team from player_index group by team;
        val teamRows2 = result.aggregations.flattenAggRows("team"){ bucket, row ->
            handleSingleValueAgg(bucket, row)
            val games = bucket.getAggregation("games", NestedAggregation::class.java) // 嵌套文档的聚合
            row["sum_games_score"] = games.getSumAggregation("sum_games_score").sum
        }
        println("统计每个队伍:" + teamRows2)

        // 每个职位 -- select avg(salary), sum(games.score), position from player_index group by position; -- sum(games.score)不能执行
        val positionRows = result.aggregations.flattenAggRows("position"){ bucket, row ->
            row["avg_salary"] = bucket.getAvgAggregation("avg_salary").avg
            val games = bucket.getAggregation("games", NestedAggregation::class.java) // 嵌套文档的聚合
            row["sum_games_score"] = games.getSumAggregation("sum_games_score").sum
        }
        println("统计每个职位:" + positionRows)

        // 每场比赛 -- select sum(games.score) from  player_index group by games.id --
        val gameRows = result.aggregations.getAggregation("games", NestedAggregation::class.java)
                .flattenAggRows("games_id")
        println("统计每场比赛:" + gameRows)

        // 每个队伍+职位 -- select avg(age), team, position from player_index group by team, position;
        val teamPositionRows = result.aggregations.flattenAggRows("team.position")
        println("统计每个队伍+职位:" + teamPositionRows)
    }

}