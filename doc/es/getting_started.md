# jkorm-es库
jkorm-es库是一个Elasticsearch ORM框架, 底层基于[jest](https://github.com/searchbox-io/Jest), 不但提供简单易用的ORM实体与仓库类, 还提供简单又强大的query dsl语法, 来帮助你快速编写出读写elasticsearch的代码, 简单易读, 开发便捷, 大大降低我们的开发成本.

本文做简单的介绍, 详细文档参考
1. [实体类 - 映射文档](doc/es/Entity.md)
2. [EsManager - 管理es索引](doc/es/EsManager.md)
3. [EsDocRepository - 实体仓库类](doc/es/EsDocRepository.md)
4. [EsQueryBuilder - 查询构建器](doc/es/EsQueryBuilder.md)

## 添加依赖

1. gradle
```
compile "net.jkcode.jkmvc:jkmvc-orm:1.9.0"
```

2. maven
```
<dependency>
    <groupId>net.jkcode.jkmvc</groupId>
    <artifactId>jkmvc-orm</artifactId>
    <version>1.9.0</version>
</dependency>
```

## 配置es.yaml
vim src/main/resources/es.yaml
```yaml
default: # es配置名, 可有多个配置
  esUrl: http://localhost:9200 # es server地址, 多个用逗号分割
  maxTotal: 100 # 连接池中总的最大连接数
  maxTotalPerRoute: 100 # 每个路由(host+port)的最大连接数
```

## 实体类及注解

demo

```kotlin
@EsDoc("message_index", "_doc")
open class MessageEntity: OrmEntity() {

    // 代理属性读写
    @EsId
    public var id:Int by property() // 消息id

    public var fromUid:Int by property() // 发送人id

    public var toUid:Int by property() // 接收人id

    public var created:Long by property() // 接收人id

    public var content:String by property() // 消息内容

    override fun toString(): String {
        return "MessageEntity(" + toMap() + ")"
    }

}
```

1. 实体类, 我们继承了db orm中OrmEntity类体系, 主要是方便db orm与es orm相互调用

2. 两个注解：

2.1 `@EsDoc` 作用在类, 标记实体类为文档对象, 一般有3个属性
```kotlin
annotation class EsDoc(
        public val index: String, // 索引名
        public val type: String = "_doc", // 类型
        public val esName: String = "default" // es配置名
)
```

2.2 `@EsId` 作用在成员变量, 标记一个字段作为_id主键

这2个注解是非常精简的, 仅仅是关注索引名与_id主键, 没有过多关注索引存储(如分片数/副本数)与字段元数据(字段类型/分词器)等等, 这些都是在框架之外由运维人员自行维护的, 从而达到简化代码的目的.


## 创建索引

```kotlin
import net.jkcode.jkmvc.es.EsManager

// gson还是必须用双引号
var mapping = """{
    '_doc':{
        'properties':{
            'id':{
                'type':'long'
            },
            'fromUid':{
                'type':'long'
            },
            'toUid':{
                'type':'long'
            },
            'content':{
                'type':'text'
            },
            'created':{
                'type':'long'
            }
        }
    }
}"""
// gson还是必须用双引号
mapping = mapping.replace('\'', '"')
println(mapping)

val esmgr = EsManager.instance()
var r = esmgr.createIndex(index)
println("创建索引[$index]: " + r)

r = esmgr.putMapping(index, type, mapping)
println("设置索引[$index]映射[$type]: " + r)
```


## 增删改操作
jkorm-es库通过实体仓储类`EsDocRepository`来提供了针对实体类的各种基本的CRUD功能.
以下代码详细参考单元测试类`EsDocRepositoryTests`

### 1. 实例化 EsDocRepository

```kotlin
import net.jkcode.jkmvc.es.EsDocRepository

val rep = EsDocRepository.instance(MessageEntity::class.java)
```

### 2. 单个保存(id存在就是修改, 否则就是插入)
```kotlin
@Test
fun testSave() {
    val e = buildEntity(1)
    val r = rep.save(e)
    println("插入单个文档: " + r)
}
```


### 3. 批量保存
```kotlin
@Test
fun testSaveAll() {
    val items = ArrayList<MessageEntity>()

    for (i in 1..10) {
        val e = buildEntity(i)
        items.add(e)
    }

    rep.saveAll(items)
    println("批量插入")
}
```

### 4. 增量更新
```kotlin
@Test
fun testUpdate() {
    val e = rep.findById("1")!!
    e.fromUid = randomInt(10)
    e.toUid = randomInt(10)
    val r = rep.update(e)
    println("更新文档: " + r)
}
```

### 5. 单个删除
```kotlin
@Test
fun testDeleteById() {
    rep.deleteById("1")
    println("删除id=1文档")
}
```

### 6. 批量删除
```kotlin
@Test
fun testDeleteAll() {
    val pageSize = 5
    val query = rep.queryBuilder()
            .must("fromUid", ">=", 0)
    //val ids = rep.deleteAll(query)
    val ids = query.deleteDocs(pageSize)
    println("删除" + ids.size + "个文档: id in " + ids)
}
```

### 7. 根据id查询单个
```kotlin
@Test
fun testFindById() {
    val id = "1"
    val entity = rep.findById(id)
    println("查单个：" + entity.toString())
}
```

### 8. 查询全部, 并按照id排序
```kotlin
@Test
fun testFindAll() {
    val query = rep.queryBuilder()
            //.must("fromUid", ">=", 0)
            .orderByField("id") // 排序
            .limit(10) // 分页
    val (list, size) = rep.findAll(query)
    println("查到 $size 个文档")
    for (item in list)
        println(item)
}
```

## 高级查询
以下代码详细参考单元测试类`EsQueryBuilderTests`

### 1. 基本查询(条件+分页+排序)

```kotlin
@Test
fun testSearch() {
    // 构建query builder
    val query = EsQueryBuilder()
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
![](../../img/es/search-compare.png)
基本上一样, 只是select字段的顺序不同, 这是因为jkorm-es使用了HashSet(排重)来接收字段, 导致字段顺序变更

### 2. 复杂查询
非常复杂的查询, 但使用 jkorm-es 库可以做到非常简单与可读

```kotlin
/**
 * 复杂查询
 *  对比 https://mp.weixin.qq.com/s/GFRiiQEk-JLpPnCi_WrRqw
 *      https://www.infoq.cn/article/u4Xhw5Q3jfLE1brGhtbR
 */
@Test
fun testComplexSearch() {
    val query = EsQueryBuilder()
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
![](../../img/es/complex-search-compare.png)
基本上一样, 只是select字段的顺序不同, 这是因为jkorm-es使用了HashSet(排重)来接收字段, 导致字段顺序变更

## 聚合
以下代码详细参考单元测试类`EsAggregationTests`

### 1. 简单聚合
demo: 按照队伍team进行分组(桶)

```kotlin
@Test
public void testAgg(){
    // 构建聚合查询
    val query = EsQueryBuilder()
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

#### 解析聚合结果
方法 `net.jkcode.jkmvc.es._JestKt.path: String, aggValueCollector: (bucket: Bucket, row: MutableMap<String, Any>) -> Unit = ::handleSingleValueAgg): ArrayList<Map<String, Any>>` 将树型的聚合结果进行扁平化, 转为多行的Map, 每个Map的key是统计字段名, value是统计字段值;
如上面代码的结果输出如下:

```
统计每个队伍:[{max_age=28.0, count_position=4, count=4, team=湖人},
             {max_age=9.0, count_position=1, count=1, team=骑士}]
```

由此可见, 输出为多行的Map, Map的key是聚合字段名[team, count_position, max_age];
这种将树型聚合结果进行扁平化, 转为类似于select sql查询结果, 不用逐层去解析聚合结果, 方便开发者像往常一样开发, 简单易理解. 

限制: 只能收集单值聚合的结果, 其他聚合对象需手动收集

### 2. 复杂聚合

```kotlin
@Test
fun testComplexAgg(){
    // 构建聚合查询
    val query = EsQueryBuilder()
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
![](../../img/es/complex-agg-compare.png)
基本上一样, 只是 terms 聚合多了field定义, 没啥影响