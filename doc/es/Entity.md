# 实体类及注解
实体类, 主要用于映射物理的es索引, 以方便用面向对象的API来读写索引

## demo

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

1. 实体类, 我们继承了db orm中OrmEntity类体系, 主要是方便db orm与es orm相互调用, 也可统一2类存储实现上的实体类

2. 两个注解：

2.1 `@EsDoc` 作用在类，标记实体类为文档对象，一般有3个属性
```kotlin
annotation class EsDoc(
        public val index: String, // 索引名
        public val type: String = "_doc", // 类型
        public val esName: String = "default" // es配置名
)
```

2.2 `@EsId` 作用在成员变量，标记一个字段作为_id主键

这2个注解是非常精简的, 仅仅是关注索引名与_id主键, 没有过多关注索引存储(如分片数/副本数)与字段元数据(字段类型/分词器)等等, 这些都是在框架之外由运维人员自行维护的, 从而达到简化代码的目的.

仅在kotlin中有效

如果在java中, 请使用注解`@JestId`, 这是在jest中定义的, 详见`io.searchbox.annotations.JestId`
