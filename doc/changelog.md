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
2. 添加限流算法
