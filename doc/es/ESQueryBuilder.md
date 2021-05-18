# ESQueryBuilder
es查询构建器，是通过提供类sql的一系列方法，来帮助开发者快速构建原生es查询dsl.

## 6 高级查询

### 6.1 `Join` 语句

如果你要联查多个表，则你需要使用 `join()` 与 `on()` 方法. 

`join()` 需要2个参数
* 表名：可以是 `String` 或 `Pair<String, String>` （包含表名+表别名）
* 连接方式: LEFT（左连接）, RIGHT（右连接）, INNER（内连接）.

`on()` 方法主要用于设置2个关联表的连接条件，与 `where()` 方法类似，但它需要3个参数; 1 左字段名 2 符号 3 右字段名. 多次调用 `on()` 方法会构建多个连接条件，条件之间用 "AND" 操作符来连接

```
// 使用`JOIN` 来查询出作者 "smith" 关联的所有文章
query.select("authors.name", "posts.content").from("authors").join("posts").on("authors.id", "=", "posts.author_id").where("authors.name", "=", "smith").findMaps();
```

生成sql如下：

```
SELECT `authors`.`name`, `posts`.`content` FROM `authors` JOIN `posts` ON (`authors`.`id` = `posts`.`author_id`) WHERE `authors`.`name` = "smith"
```

如果你要使用不同的连接方式（LEFT / RIGHT / INNER），你只需要调用 `join("columName", "joinType")`，就是用第二个参数来指定连接方式：

```
// 使用`LEFT JOIN` 来查询出作者 "smith" 关联的所有文章
query.from("authors").join("posts", "LEFT").on("authors.id", "=", "posts.author_id").where("authors.name", "=", "smith");
```

生成sql如下：

```
SELECT `authors`.`name`, `posts`.`content` FROM `authors` LEFT JOIN `posts` ON (`authors`.`id` = `posts`.`author_id`) WHERE `authors`.`name` = "smith"
```

[!!] 如果你联查的多个表中存在同名的字段，则你在指定返回字段时，最好加上表前缀，来避免sql执行异常. 如果遇到`未明确定义的列（Ambiguous column name）`的错误时，你需要给字段加上表前缀，或者字段别名。

### 6.2 聚合函数

SQL中提供的聚合函数可以用来统计、求和、求最值等，如 `COUNT()`, `SUM()`, `AVG()`. 他们通常是结合 `groupBy()` 来分组统计，或结合 `having()` 来过滤聚合结果

```
query.select("username", DbExpr("COUNT(`id`)", "total_posts", false)).from("posts").groupBy("username").having("total_posts", ">=", 10).findMaps()
```

生成sql如下：

```
SELECT `username`, COUNT(`id`) AS `total_posts` FROM `posts` GROUP BY `username` HAVING `total_posts` >= 10
```

### 6.3 子查询

查询构建器对象可以作为很多方法的参数，来构建子查询。让我们用上面的查询，传给新的查询作为子查询：

```
// subquery
val sub = DbQueryBuilder().select("username", DbExpr("COUNT(`id`)", "total_posts", false))
        .from("posts").groupBy("username").having("total_posts", ">=", 10);

// join subquery
DbQueryBuilder().select("profiles.*", "posts.total_posts").from("profiles")
.join(DbExpr(sub, "posts", false), "INNER").on("profiles.username", "=", "posts.username").findMaps()
```

生成sql如下：

```
SELECT `profiles`.*, `posts`.`total_posts` FROM `profiles` INNER JOIN
( SELECT `username`, COUNT(`id`) AS `total_posts` FROM `posts` GROUP BY `username` HAVING `total_posts` >= 10 ) `posts`
ON `profiles`.`username` = `posts`.`username`
```

Insert 查询也可以接入 Select 子查询

```
// subquery
val sub = DbQueryBuilder().select("username", DbExpr("COUNT(`id`)", "total_posts", false))
.from("posts").groupBy("username").having("total_posts", ">=", 10);

// insert subquery
DbQueryBuilder().table("post_totals").insertColumns("username", "posts").values(sub).insert()
```

This will generate the following query:

```
INSERT INTO `post_totals` (`username`, `posts`) 
SELECT `username`, COUNT(`id`) AS `total_posts` FROM `posts` GROUP BY `username` HAVING `total_posts` >= 10 
```

### 6.4 布尔操作符与嵌套子句

多个 `WHERE` 与 `HAVING` 子句是用布尔操作符（`AND`/`OR`）来连接的。无前缀或前缀为 `and` 的方法的操作符是`AND`. 前缀为 `or` 的方法的操作符是`OR`. `WHERE` 与 `HAVING` 子句可以嵌套使用，你可以使用后缀为`open` 的方法来开启一个分组，使用后缀为 `close` 的方法来关闭一个分组. 

```
query.from("user")
    .whereOpen()
        .where("id", "IN", arrayOf(1, 2, 3, 5))
        .andWhereOpen()
            .where("lastLogin", "<=", System.currentTimeMillis() / 1000)
            .orWhere("lastLogin", "IS", null)
        .andWhereClose()
    .whereClose()
    .andWhere("removed","IS", null)
    .findMaps()
```

生成sql如下：

```
SELECT  * FROM `user` WHERE ( `id` IN (1, 2, 3, 5)  AND ( `lastLogin` <= 1511069644 OR `lastLogin` IS null )) AND `removed` IS null
```

### 6.5 数据库表达式

在 `DbQueryBuilder` 的 `insert/update` 语句中，要保存的字段值总是要被转义 `Db::quote(value:Any?)`。但是有时候字段值是一个原生的表达式与函数调用，此时是不需要转义的。

因此，我们需要数据库表达式 `DbExpr`，用于添加不转义的字段值，表示要保存的字段值是一个sql表达式，如 now() / column1 + 1

```
DbQueryBuilder().table("user")
    .set("login_count", DbExpr("login_count + 1")) // 等价于 .set("login_count", "login_count + 1", true)
    .where("id", "=", 45)
    .update();
```

生成sql如下：

```
UPDATE `user` SET `login_count` = `login_count` + 1 WHERE `id` = 45
```

[!!] 你必须要事先保证：创建`DbExpr(input:String)` 时传递的表达式是有效并且已转义过的。

## 7 例子

```
// 获得 Db 对象
val db: Db = Db.instance()

// 开启事务
db.transaction {
    // 插入
    var id = DbQueryBuilder(db).table("user").insertColumns("name", "age").value("shi", 1).insert("id");
    println("插入user表：" + id)

    // 查询一条数据
    val row = DbQueryBuilder(db).table("user").where("id", "=", id).findMap()
    println("查询user表：" + row)

    // 更新
    var f = DbQueryBuilder(db).table("user").sets(mapOf("name" to "wang", "age" to 2)).where("id", "=", id).update();
    println("更新user表：" + f)

    // 查询多条数据
    val rows = DbQueryBuilder(db).table("user").orderBy("id").limit(1).findMaps()
    println("查询user表：" + rows)

    // 删除
    f = DbQueryBuilder(db).table("user").where("id", "=", id).delete();
    println("删除user表：" + f)
}
```

# 高级查询
以下代码详细参考单元测试类`EsQueryBuilderTests`

## 1. 基本查询(条件+分页+排序)

```kotlin
@Test
fun testSearch() {
    // 构建query builder
    val query = ESQueryBuilder()
            .must("searchable", "=", true)
            .must("driverUserName", "=", "张三")
            .select("cargoId", "driverUserName", "loadAddress", "companyId")
            .orderByField("id")
            .limit(10, 30)

    // 执行搜索
    val (list, size) = query.searchDocs(RecentOrder::class.java)
    println("查到 $size 个文档")
    for (item in list)
        println(item)
}
```

而使用原生Java High Level REST Client接口方式：
```java
// 构建query builder
final BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
final TermsQueryBuilder searchable = QueryBuilders.termQuery("searchable", true);
final TermsQueryBuilder driverUserName = QueryBuilders.termQuery("driverUserName", "张三");
queryBuilder.must(searchable).must(driverUserName);

SearchSourceBuilder searchSource = new SearchSourceBuilder();
searchSource.query(queryBuilder);
searchSource.fetchSource(new String[]{"cargoId", "driverUserName", "loadAddress", "companyId"},
        new String[0]);
searchSource.sort("id", SortOrder.ASC);
searchSource.from(30);
searchSource.size(10);

// 执行搜索
......
```

对比生成的es搜索DSL如下, 左边是jkorm-es生成代码, 右边是原生API生成代码
![](img/es/search-compare.png)
基本上一样, 只是select字段的顺序不同, 这是因为jkorm-es使用了HashSet(排重)来接收字段, 导致字段顺序变更

## 2. 复杂查询
非常复杂的查询, 但使用 jkorm-es 库可以做到非常简单与可读

```kotlin
/**
 * 复杂查询
 *  对比 https://mp.weixin.qq.com/s/GFRiiQEk-JLpPnCi_WrRqw
 *      https://www.infoq.cn/article/u4Xhw5Q3jfLE1brGhtbR
 */
@Test
fun testComplexSearch() {
    val query = ESQueryBuilder()
            .index(index)
            .type(type)
            .mustWrap { // city
                mustWrap { // start city
                    should("startCityId", "IN", arrayOf(320705, 931125))
                    should("startDistrictId", "IN", arrayOf(684214, 981362))
                }
                mustWrap { // end city
                    should("endCityId", "IN", arrayOf(589421, 953652))
                    should("endDistrictId", "IN", arrayOf(95312, 931125))
                }
            }
            .must("updateTime", ">=", 1608285822239L) // range
            .must("cargoLabels", "IN", arrayOf("水果", "生鲜")) // cargoLabels

            .mustNot("cargoCategory", "IN", arrayOf("A", "B")) // cargoCategory
            .mustNot("featureSort", "=", "好货") // cargoCategory

            .shouldWrap {
                mustNot("cargoChannel", "IN", arrayOf("长途货源", "一口价货源")) // cargoChannel
                shouldWrap {
                    must("searchableSources", "=", "ALL") // searchableSources
                    mustWrap { // security
                        must("cargoChannel", "IN", arrayOf("No.1", "No.2", "No.3")) // cargoChannel
                        must("securityTran", "=", "平台保证") // securityTran
                    }
                }
            }

    query.select("cargoId", "startDistrictId", "startCityId", "endDistrictId", "endCityId", "updateTime", "cargoLabels",
            "cargoCategory", "featureSort", "cargoChannel", "searchableSources", "securityTran")

    query.orderByField("duplicate")
            .orderByScript("searchCargo-script", mapOf("searchColdCargoTop" to 0))

    // 执行搜索
    val (list, size) = query.searchDocs(RecentOrder::class.java)
    println("查到 $size 个文档")
    for (item in list)
        println(item)
}
```

而使用原生Java High Level REST Client接口方式, 非常复杂与难看, 光这么多个条件对应BoolQueryBuilder对象创建与命名与关联就够人喝一壶, 代码量近乎jkorm-es的2倍, 且代码远没有jkorm-es清晰易懂：

```java
// 构建query builder
final BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
final TermsQueryBuilder startCityId = QueryBuilders.termsQuery("startCityId", Lists.newArrayList(320705L, 931125L));
final TermsQueryBuilder startDistrictId = QueryBuilders.termsQuery("startDistrictId", Lists.newArrayList(684214L, 981362L));
final TermsQueryBuilder endCityId = QueryBuilders.termsQuery("endCityId", Lists.newArrayList(589421L, 953652L));
final TermsQueryBuilder endDistrictId = QueryBuilders.termsQuery("endDistrictId", Lists.newArrayList(95312L, 931125L));
final BoolQueryBuilder startBuilder = QueryBuilders.boolQuery();
startBuilder.should(startCityId).should(startDistrictId);
final BoolQueryBuilder endBuilder = QueryBuilders.boolQuery();
endBuilder.should(endCityId).should(endDistrictId);
final BoolQueryBuilder cityBuilder = QueryBuilders.boolQuery();
cityBuilder.must(startBuilder);
cityBuilder.must(endBuilder);
queryBuilder.must(cityBuilder);
final RangeQueryBuilder rangeBuilder = QueryBuilders.rangeQuery("updateTime");
queryBuilder.must(rangeBuilder.from(1608285822239L));
final TermsQueryBuilder cargoLabelsBuilder = QueryBuilders.termsQuery("cargoLabels", Lists.newArrayList("水果", "生鲜"));
queryBuilder.must(cargoLabelsBuilder);
final TermsQueryBuilder cargoCategoryBuilder = QueryBuilders.termsQuery("cargoCategory", Lists.newArrayList("A", "B"));
final TermQueryBuilder featureSortBuilder = QueryBuilders.termQuery("featureSort", "好货");
queryBuilder.mustNot(cargoCategoryBuilder);
queryBuilder.mustNot(featureSortBuilder);
final BoolQueryBuilder cargoChannelBuilder = QueryBuilders.boolQuery();
queryBuilder.should(cargoChannelBuilder);
final TermsQueryBuilder channelBuilder = QueryBuilders.termsQuery("cargoChannel", Lists.newArrayList("长途货源", "一口价货源"));
cargoChannelBuilder.mustNot(channelBuilder);
final BoolQueryBuilder searchableSourcesBuilder = QueryBuilders.boolQuery();
cargoChannelBuilder.should(searchableSourcesBuilder);
final TermQueryBuilder sourceBuilder = QueryBuilders.termQuery("searchableSources", "ALL");
searchableSourcesBuilder.must(sourceBuilder);
final BoolQueryBuilder securityTranBuilder = QueryBuilders.boolQuery();
searchableSourcesBuilder.must(securityTranBuilder);
securityTranBuilder.must(QueryBuilders.termsQuery("cargoChannel", "No.1", "No.2", "No.3"));
securityTranBuilder.must(QueryBuilders.termQuery("securityTran", "平台保证"));

SearchSourceBuilder searchSource = new SearchSourceBuilder();
searchSource.query(queryBuilder);
searchSource.fetchSource(new String[]{"cargoId", "startDistrictId", "startCityId", "endDistrictId", "endCityId", "updateTime", "cargoLabels",
                "cargoCategory", "featureSort", "cargoChannel", "searchableSources", "securityTran"},
        new String[0]);
searchSource.sort("duplicate", SortOrder.ASC);
ScriptSortBuilder sortBuilder = SortBuilders.scriptSort(new org.elasticsearch.script.Script(ScriptType.INLINE,
        "painless", "searchCargo-script", Collections.emptyMap(), Collections.singletonMap("searchColdCargoTop", 0)),
        ScriptSortBuilder.ScriptSortType.STRING).order(SortOrder.ASC);
searchSource.sort(sortBuilder);

// 执行搜索
......
```

对比生成的es搜索DSL如下, 左边是jkorm-es生成代码, 右边是原生API生成代码
![](img/es/complex-search-compare.png)
基本上一样, 只是select字段的顺序不同, 这是因为jkorm-es使用了HashSet(排重)来接收字段, 导致字段顺序变更

# 聚合
以下代码详细参考单元测试类`EsAggregationTests`

## 1. 简单聚合
demo: 按照队伍team进行分组(桶)

```kotlin
@Test
public void testAgg(){
    // 构建聚合查询
    val query = ESQueryBuilder()
        .index(index)
        .type(type)
        .aggByAndWrapSubAgg("team") { // 别名是team
            aggBy("count(position)") // 别名是count_position
            aggBy("max(age)") // 别名是max_age
        }

    // 执行查询
    val result = query.searchDocs()

    // 转换聚合结果
    // 每个队伍 -- select count(1), count(position) as count_position, max(age) as max_age, team from player_index group by team;
    val teamRows = result.aggregations.flattenAggRows("team") // 将树型的聚合结果进行扁平化, 转为多行的Map, 每个Map的key是统计字段名, value是统计字段值
    println("统计每个队伍:" + teamRows)
}
```

而使用原生Java High Level REST Client接口方式
```java
// 构建聚合查询
val teamAgg = AggregationBuilders.terms("team ").field("team")
val posAgg = AggregationBuilders.count("count_position").field("position")
val ageAgg = AggregationBuilders.max("max_age").field("age")

SearchSourceBuilder searchSource = new SearchSourceBuilder();
searchSource.aggregation(teamAgg.subAggregation(posAgg).subAggregation(ageAgg))

// 执行查询
......
```

生成的es搜索DSL是一样的
```json
{
  "aggregations" : {
    "team " : {
      "terms" : {
        "field" : "team",
        "size" : 10,
        "min_doc_count" : 1,
        "shard_min_doc_count" : 0,
        "show_term_doc_count_error" : false,
        "order" : [
          {
            "_count" : "desc"
          },
          {
            "_term" : "asc"
          }
        ]
      },
      "aggregations" : {
        "count_position" : {
          "value_count" : {
            "field" : "position"
          }
        },
        "max_age" : {
          "max" : {
            "field" : "age"
          }
        }
      }
    }
  }
}
```

### 解析聚合结果
方法 `net.jkcode.jkmvc.es._JestKt.path: String, aggValueCollector: (bucket: Bucket, row: MutableMap<String, Any>) -> Unit = ::handleSingleValueAgg): ArrayList<Map<String, Any>>` 将树型的聚合结果进行扁平化, 转为多行的Map, 每个Map的key是统计字段名, value是统计字段值;
如上面代码的结果输出如下:

```
统计每个队伍:[{max_age=28.0, count_position=4, count=4, team=湖人},
             {max_age=9.0, count_position=1, count=1, team=骑士}]
```

由此可见, 输出为多行的Map, Map的key是聚合字段名[team, count_position, max_age];
这种将树型聚合结果进行扁平化, 转为类似于select sql查询结果, 不用逐层去解析聚合结果, 方便开发者像往常一样开发, 简单易理解. 

限制: 只能收集单值聚合的结果, 其他聚合对象需手动收集

## 2. 复杂聚合

```kotlin
@Test
fun testComplexAgg(){
    // 构建聚合查询
    val query = ESQueryBuilder()
            .index(index)
            .type(type)
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

    // 执行查询
    val result = query.searchDocs()

    // 解析聚合结果
    // 每个队伍 -- select count(position), sum(salary), sum(games.score), team from player_index group by team; -- sum(games.score)值错误, 为0, 只能自己算
    val teamRows2 = result.aggregations.flattenAggRows("team"){ bucket, row ->
        handleSingleValueAgg(bucket, row)
        val games = bucket.getAggregation("games", NestedAggregation::class.java) // 嵌套文档的聚合
        row["sum_games_score"] = games.getSumAggregation("sum_games_score").sum
    }
    println("统计每个队伍:" + teamRows2)

    // 每个职位 -- select avg(salary), sum(games.score), position from player_index group by position; -- sum(games.score)值错误, 为0, 只能自己算
    val positionRows = result.aggregations.flattenAggRows("position"){ bucket, row ->
        row["avg_salary"] = bucket.getAvgAggregation("avg_salary").avg
        val games = bucket.getAggregation("games", NestedAggregation::class.java) // 嵌套文档的聚合
        row["sum_games_score"] = games.getSumAggregation("sum_games_score").sum
    }
    println("统计每个职位:" + positionRows)

    // 每场比赛 -- select sum(games.score) from  player_index group by games.id -- 错误: Grouping isn't (yet) compatible with nested fields [games.id]]
    val gameRows = result.aggregations.getAggregation("games", NestedAggregation::class.java)
            .flattenAggRows("games.id")
    println("统计每场比赛:" + gameRows)

    // 每个队伍+职位 -- select avg(age), team, position from player_index group by team, position;
    val teamPositionRows = result.aggregations.flattenAggRows("team>position")
    println("统计每个队伍+职位:" + teamPositionRows)
}
```

而使用原生Java High Level REST Client接口方式, 非常复杂与难看, 光这么多个条件对应AggregationBuilder对象创建与命名与关联就够人喝一壶, 代码量近乎jkorm-es的2倍, 且代码远没有jkorm-es清晰易懂

```java
// 构建聚合查询
// 每个队伍 -- select count(position), sum(salary), sum(games.score), team from player_index group by team;
TermsAggregationBuilder teamAgg = AggregationBuilders.terms("team").order(Terms.Order.aggregation("games>sum_games_score", false));
CardinalityAggregationBuilder cardinalityPositionAgg = AggregationBuilders.cardinality("cardinality_position ").field("position");
SumAggregationBuilder sumSalaryAgg = AggregationBuilders.sum("sum_salary").field("salary");
teamAgg.subAggregation(cardinalityPositionAgg);
teamAgg.subAggregation(sumSalaryAgg);
// 子文档
NestedAggregationBuilder nestedGamesAgg1 = AggregationBuilders.nested("games", "games");
SumAggregationBuilder sumScoreAgg1 = AggregationBuilders.sum("sum_games_score").field("games.score");
nestedGamesAgg1.subAggregation(sumScoreAgg1);
teamAgg.subAggregation(nestedGamesAgg1);

// 每个队伍+职位 -- select avg(age), team, position from player_index group by team, position;
TermsAggregationBuilder subPositionAgg = AggregationBuilders.terms("position");
AvgAggregationBuilder avgAgeAgg = AggregationBuilders.avg("avg_age").field("age");
subPositionAgg.subAggregation(avgAgeAgg);
teamAgg.subAggregation(subPositionAgg);

// 每个职位 -- select avg(salary), sum(games.score), position from player_index group by position; -- sum(games.score)不能执行
TermsAggregationBuilder positionAgg = AggregationBuilders.terms("position").order(Terms.Order.aggregation("games>sum_games_score", false));
AvgAggregationBuilder avgSalaryAgg = AggregationBuilders.avg("avg_salary").field("salary");
positionAgg.subAggregation(avgSalaryAgg);
// 子文档
NestedAggregationBuilder nestedGamesAgg2 = AggregationBuilders.nested("games", "games");
SumAggregationBuilder sumScoreAgg2 = AggregationBuilders.sum("sum_games_score").field("games.score");
nestedGamesAgg2.subAggregation(sumScoreAgg2);
positionAgg.subAggregation(nestedGamesAgg2);

// 每场比赛 -- select sum(games.score) from  player_index group by games.id
NestedAggregationBuilder nestedGamesAgg3 = AggregationBuilders.nested("games", "games");
TermsAggregationBuilder subGameIdAgg = AggregationBuilders.terms("games.id").order(Terms.Order.aggregation("sum_games_score", false));
SumAggregationBuilder sumScoreAgg3 = AggregationBuilders.sum("sum_games_score").field("games.score");
subGameIdAgg.subAggregation(sumScoreAgg3);
nestedGamesAgg3.subAggregation(subGameIdAgg);

SearchSourceBuilder nativebuilder = new SearchSourceBuilder();
nativebuilder.aggregation(teamAgg);
nativebuilder.aggregation(positionAgg);
nativebuilder.aggregation(nestedGamesAgg3);

// 执行查询
......

// 解析查询结果
......
```

对比生成的es搜索DSL如下, 左边是jkorm-es生成代码, 右边是原生API生成代码
![](img/es/complex-agg-compare.png)
基本上一样, 只是 terms 聚合多了field定义, 没啥影响