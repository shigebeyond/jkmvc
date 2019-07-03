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
支持读写分离, 支持多主键, 重构orm校验, 抽取公共的模型`GeneralModel`,

# 1.6
优化id生成, 完善orm校验, 支持orm的持久化事件(如`beforeCreate/afterCreate`等), 优化redis client类性能(使用`ShardedJedisPool`)

# 1.7
1. 调整包名, 将包`com.jkmvc`重命名为`net.jkcode.jkmvc`

2. 支持servlet3.0, 即异步servlet

3. 支持action方法返回 CompletableFuture

4. 支持 ThreadLocalInheritableThreadPool 可继承ThreadLocal的线程池

5. 支持 ConsistentHash

6. 扩展 BitSet 相关的集合处理, 并优化 FixedKeyMapFactory 支持map的完整语义(遍历与删除)

7. 将session模块合并到http模块中

8. 完善model代码生成

9. 独立 OrmEntity, 不继承 IOrm, 支持 Entity 与 Model 的分离, 适用场景如rpc, Entity只管数据不管存储, Model继承Entity还管存储, rpc中参数只使用Entity即可

10. 优化 GeneralOrmMeta, 在 Entity 与 Model 分离的场景下, Model既要继承Entity, 也要实现IOrm, 则使用 GeneralOrmMeta 来代理实现 IOrm

11. 支持 Entity 的序列化, 用在rpc中
