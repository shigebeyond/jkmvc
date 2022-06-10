# 1.1
1. 新加db模块

1.1 db连接管理

1.2 支持query builder

1.3 重构DbQueryBuilderAction的sql编译，设计sql模板机制以精简代码；同时使用StringBuilder来优化sql拼接的性能

1.4 改进query builder，使得find()/findAll()可以根据泛型来直接将db数据转换为对应的数据类型

2. 新加orm模块

2.1 抽象orm元数据，包含表名、主键、关联关系等

2.2 抽象orm类族，包含 OrmEntity/OrmValid/OrmPersistent/OrmRelated 等多层级子类

2.3 抽象多种关联关系：belongsTo, hasOne, hasMany

3. 新加http模块

3.1 添加mvc组件，包含请求、响应、路由、控制器等

3.2 设计 ControllerLoader, 支持根据包名自动扫描控制器类

3.3 简化路由配置，根据`约定重于配置`的原则，使用控制器类名与方法名来充当默认的路由规则，不用每个类与方法都写注解配置

3.4 完善视图

4. 升级kotlin版本为 1.1.1

# 1.2
1. 优化orm模块

1.1 OrmValid支持校验规则表达式

1.2 Orm构造函数支持传id来查询数据，内部支持根据id缓存查询结果，以便优化性能

2. 完善http模块

2.1 添加 jetty gradle 插件

2.2 路由支持定义带正则的路由规则

2.3 请求与响应支持中文

2.4 优化文件上传，完善上传图片的引用url

2.4 对静态文件或上传文件的目录的url，跳过路由解析

3. 完善db模块

3.1 优化DbQueryBuilderAction的sql编译, 正则回调替换sql模板中的变量

3.2 优化 DbQueryBuilderDecoration的sql编译，抽象where/group by/order by/limit对应的子句

3.3 抽象 CompiledSql 来缓存编译后的sql

3.3 db与query builder支持批处理多个sql

# 1.3

1. 优化文件遍历的性能

2. 添加velocity模板视图

3. 支持从jar中扫描控制器类

4. 添加 Auth，支持用户验证

5. orm添加增删改查前置后置事件

6. orm读写数据时，支持属性名与db字段名自动转换（命名规范不同）

7. db兼容oracle

8. 支持跨域

9. 解决联查hasMany关联对象时的放大查询问题

10. 支持有中间表的关联查询（扩展关联关系：hasOneThrough, hasManyThrough），进一步扩展中间表联查也带条件

11. 优化redis库

12. 支持model的代码生成器

13. 添加kotlin文档的gradle插件

14. 支持DbExpr(db表达式)，主要用在insert/update语句中的字段值，在生成sql时不转义值，以为sql编译增加更多灵活性

15. 优化limit部分的sql生成，兼容各类db的特异的语法

16. query builder支持子查询

17. 完善请求的校验

18. 添加RequestHandledHook（请求处理后事件的钩子），用于关闭资源，如关闭db

19. query builder中如果in参数超过1000个，则拆分为多次调用

20. 为request添加生成curl命令的方法

21. Db支持sql预览


# 1.4
1. 支持读写分离

2. db/orm模块改造，支持多主键

3. db模块优化查询结果处理: 在_Connection.kt中实现全局共享的可复用的用于存储一行数据的 HashMap 对象池

4. 添加GeneralModel，代表通用orm模型，不需要根据表来单独定义模型类，以增加模型处理的动态性

5. 重构校验器

# 1.5

1. 更换gradle的jetty插件：将jetty换为greety

2. 新增server-jetty模块，直接内嵌jetty启动

3. http模块支持使用jetty的异步请求机制

4. 优化校验表达式的解析

5. 支持全局的bean单例池（bean容器）

# 1.6 

1. 添加熔断、降级、限流、合并请求等流浪守护代码，随后将代码抽到单独的项目 jkguard

2. 完善各种工具类：分布式锁、缓存、一致性哈希、雪花id

4. 修正异步请求bug：对静态文件请求不做异步处理, 否则默认的servlet处理不了

5. http模块添加拦截器机制

# 1.7 
1. 使用 Servlet 3 MultiPartConfigElement实现文件上传

2. 引入sharding-jdbc来做分库处理

# 1.8 

1. orm支持序列化某些字段

2. 支持有作用域的可传递的threadlocal，特别适用于单个请求的作用域（生命周期）

2.1 添加ScopedTransferableThreadLocal，支持有作用域的可传递的 ThreadLocal

2.2 添加SttlThreadPool，支持可传递ScopedTransferableThreadLocal的线程池

3. 支持http client

4. gradle支持发布到maven中央库

5. 支持自定义序列化器

6. 重构orm模块

7. controller支持方法级流量守护

8. 优化orm缓存

9. 完善db元数据（表结构）

9.1 新增 DbTable/DbColumn 等类，标识db的表与字段，用于生成建表、改表、删表的元数据操作sql

9.2 支持db函数映射(DbFunction), 将逻辑函数调用，转换为物理db(mysql/oracle等)的物理函数调用

10. http模块路由的快速匹配

11. 添加tag模块，扩展jsp标签

12. orm支持自动填充创建时间/人, 修改时间/人

13. 扩展路由配置：控制器支持方法级注解路由

14. 扩展query builder: 支持在同一个sql中联查HasMany关联表

15. 支持内部嵌套请求

16. 重构关联关系与关联查询

17. 添加e库，极大的提高es开发效率

# 1.9

1. 构建dockerfile

2. 引入jphp，用作php模块视图

3. 将jphp的扩展抽取到单独的工程 jphp-java-ext

# 2.0

1. 添加jphp的jkmvc扩展，支持同构异语言, 以便支持更多的动态性

1.1 支持用php写控制器，内嵌一个简单的php mvc框架

1.2 支持php控制器接受jkguard流量守护

2. 优化插件机制