# 1.1 
1. 实现一些公共的类, 如Config等

2. 实现http路由

3. 实现自动扫描controller类, 并注册

# 1.2 
实现orm, 支持session

# 1.3
完善文档, 优化代码

# 1.4
接入实际项目，支持缓存, 完善会话, 支持多数据库(如mysql+oracle), 支持自动生成代码, 修复bug

# 1.5
1. 支持读写分离

2. 支持多主键

3. 重构orm校验

4. 抽取公共的模型`GeneralModel`

# 1.6
优化id生成, 完善orm校验, 支持orm的持久化事件(如`beforeCreate/afterCreate`等), 优化redis client类性能(使用`ShardedJedisPool`)

# 1.7
1. 调整包名, 将包`com.jkmvc`重命名为`net.jkcode.jkmvc`

2. 支持servlet3.0, 即异步servlet, 从而实现servlet级别的异步

3. 支持action方法返回 CompletableFuture, 从而实现action方法级别的异步

4. 支持 ThreadLocalInheritableThreadPool 可继承ThreadLocal的线程池

5. 支持 ConsistentHash

6. 扩展 BitSet 相关的集合处理, 并优化 FixedKeyMapFactory 支持map的完整语义(遍历与删除)

7. 将session模块合并到http模块中

8. 完善model代码生成

9. 独立 OrmEntity, 不继承 IOrm, 支持 Entity 与 Model 的分离, 适用场景如rpc, Entity只管数据不管存储, Model继承Entity还管存储, rpc中参数只使用Entity即可

10. 优化 GeneralOrmMeta, 在 Entity 与 Model 分离的场景下, Model既要继承Entity, 也要实现IOrm, 则使用 GeneralOrmMeta 来代理实现 IOrm

11. 支持 Entity 的序列化, 用在rpc中

12. 添加 TreeJsonFactory, 支持orm列表转树节点

13. 添加BitSet的工具类, 支持比特的迭代

14. 支持http server的拦截器

15. 添加 ArrayFilteredTransformedIterator / CollectionFilteredTransformedIterator, 支持对数组或集合进行有过滤条件有转换元素的迭代

16. 完善 IFlusher 类族体系, 支持 CounterFlusher 与 RequestQueueFlusher

17. 重构拦截器, 由原来的before()/after()优化为链式包装拦截处理

18. 改进 trySupplierFinally(), 用 trySupplierFuture() 代替

19. HttpRequestHandler 支持 method guard, 但是暂时不开放

20. IQuotaFlusher 暴露open属性 executor 来让子类可以修改执行线程, 主要是给 TopicMessagesExector 使用, 用于控制消息消费的并发或串行.

# 1.8

1. 重构db, 兼容sharding-jdbc的dataSource

2. db事务支持回调

3. orm支持序列化某些字段

4. 支持orm与entity的互转

5. 支持有作用域的可传递的 ThreadLocal

6. 支持异步的http client, 基于asynchttpclient实现

7. gradle支持发布到maven中央库

8. 优化fst, 支持自定义序列器

9. 优化Config, 支持合并多个同名的配置文件的数据