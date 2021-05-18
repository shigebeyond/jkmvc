# EsDocRepository
EsDocRepository 实体仓储类, 提供了针对实体类的各种基本的CRUD功能。
以下代码详细参考单元测试类`EsDocRepositoryTests`

## 1. 实例化 EsDocRepository

```kotlin
import net.jkcode.jkmvc.es.EsDocRepository

val rep = EsDocRepository.instance(MessageEntity::class.java)
```

## 2. 单个保存(id存在就是修改, 否则就是插入)
```kotlin
@Test
fun testSave() {
    val e = buildEntity(1)
    val r = rep.save(e)
    println("插入单个文档: " + r)
}
```


## 3. 批量保存
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

## 4. 增量更新
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

## 5. 单个删除
```kotlin
@Test
fun testDeleteById() {
    rep.deleteById("1")
    println("删除id=1文档")
}
```

## 6. 批量删除
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

## 7. 根据id查询单个
```kotlin
@Test
fun testFindById() {
    val id = "1"
    val entity = rep.findById(id)
    println("查单个：" + entity.toString())
}
```

## 8. 查询全部, 并按照id排序
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

## 9. 统计行数
```kotlin
@Test
fun testCount() {
    val query = rep.queryBuilder()
            //.must("fromUid", ">=", 0)
    val size = rep.count(query)
    println("查到 $size 个文档")
}
```

## 10. 有游标的搜索
```
@Test
fun testScrollAll() {
    val query = rep.queryBuilder()
            //.must("fromUid", ">=", 0)
            .orderByField("id") // 排序
            //.limit(1) // 分页无效, 都是查所有数据
    val list = rep.scrollAll(query, 5)
    println("查到 ${list.size} 个文档")
    for (item in list)
        println(item)
}
```