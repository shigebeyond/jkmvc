# EsManager
es索引管理类, jkorm-es库的低级API, 是对[jest](https://github.com/searchbox-io/Jest)的初步封装, 包含对索引的管理与文档的增删改查等功能. 
以下代码详细参考单元测试类`EsTests`

## 创建索引
```kotlin
@Test
fun testCreateIndex() {
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
    var r = esmgr.createIndex(index)
    println("创建索引[$index]: " + r)
    r = esmgr.putMapping(index, type, mapping)
    println("设置索引[$index]映射[$type]: " + r)
}
```

## 获得索引
```kotlin
@Test
fun testGetIndex() {
    val setting = esmgr.getSetting(index)
    println("----------- setting ----------")
    println(setting)
    val mapping = esmgr.getMapping(index, type)
    println("----------- mapping ----------")
    println(mapping)
}
```

## 检查索引是否存在
```kotlin
@Test
fun testIndexExist() {
    val r = esmgr.indexExist(index)
    System.out.println("索引[$index]是否存在：" + r)
}
```

## 刷新索引, 以便修改数据后能被检索到
```kotlin
@Test
fun testRefreshIndex() {
    esmgr.refreshIndex(index)
}
```

## 删除索引
```kotlin
@Test
fun testDeleteIndex() {
    //删除
    val r = esmgr.deleteIndex(index)
    System.out.println("删除索引[$index]：" + r)
}
```

## 保存单个文档
如果指定id且存在, 则更新, 否则新增
```kotlin
@Test
fun testIndexDoc() {
    val e = buildEntity(1)
    val r = esmgr.indexDoc(index, type, e)
    println("保存单个文档: " + r)
}
```

## 批量保存多个文档
```kotlin
@Test
fun testBulkIndexDocs() {
    val items = ArrayList<MessageEntity>()

    for (i in 1..10) {
        val e = buildEntity(i)
        items.add(e)
    }

    esmgr.bulkIndexDocs(index, type, items)
    println("批量保存")
}
```

## 更新单个文档
提交所有字段值

```kotlin
@Test
fun testUpdateDoc() {
    val e = esmgr.getDoc(index, type, "1", MessageEntity::class.java)!!
    e.fromUid = randomInt(10)
    e.toUid = randomInt(10)
    val r = esmgr.updateDoc(index, type, e, "1")
    println("更新文档: " + r)
}
```

## 部分更新单个文档
提交部分字段值, 减少网络传输的数据量
```kotlin
@Test
fun testPartUpdateDoc() {
    val r = esmgr.updateDoc(index, type, mapOf("name" to "shi"), "1")
    println("部分更新文档: " + r)
}
```

## 根据id获得单个文档
```kotlin
@Test
fun testGetDoc() {
    val id = "1"
    val entity = esmgr.getDoc(index, type, id, MessageEntity::class.java)
    System.out.println("查单个：" + entity.toString())
}
```

## 根据id获得多个文档
```kotlin
@Test
fun testMultiGetDoc() {
    val ids = listOf("1", "2")
    val entities = esmgr.multGetDocs(index, type, ids, MessageEntity::class.java)
    System.out.println("查多个：" + entities)
}
```

## 搜索文档
通过`EsQueryBuilder`来挂载搜索条件
```
@Test
fun testSearch() {
    val ts = startTime + 120 // 2分钟
    println("timestamp: $ts")
    val query = EsQueryBuilder()
            .filter("fromUid", ">=", 7)
            .must("toUid", ">=", 8)
            .must("created", "<=", 120)
            .must("content", "like", "Welcome")
            .shouldWrap {
                must("content", "like", "Welcome")
                must("created", "<=", ts) // 两分钟前发的
            }
            .shouldWrap {
                must("content", "like", "Goodbye")
                must("created", ">", ts) // 两分钟内发的
            }
            .mustWrap {
                should("created", "=", 120)
                should("fromUid", "=", 8)
            }
            .must("toUid", ">=", 8)
            .limit(10)
            .orderByField("id")

    val (list, size) = esmgr.searchDocs(index, type, query, MessageEntity::class.java)
    println("查到 $size 个文档")
    for (item in list)
        println(item)
}
```

## 带游标的搜索文档
```kotlin
@Test
fun testScroll() {
    val pageSize = 5
    val c = EsQueryBuilder().index(index).type(type).scrollDocs(MessageEntity::class.java, pageSize, 100000)
    val times = c.size / pageSize + 1
    println("记录数=${c.size}, 每次取=$pageSize, 取次数=$times")
    for (item in c)
        println(item)
}
```

## 根据id删除单个文档
```kotlin
@Test
fun testDeleteDoc() {
    esmgr.deleteDoc(index, type, "1")
    println("删除id=1文档")
}
```

## 搜索并删除文档
通过`EsQueryBuilder`来挂载搜索条件
```kotlin
@Test
fun testSearchDeleteDoc() {
    val pageSize = 5
    val query = EsQueryBuilder()
    val ids = esmgr.deleteDocsByQuery2(index, type, query, pageSize, 100000)
    println("删除" + ids.size + "个文档: id in " + ids)
}
```
