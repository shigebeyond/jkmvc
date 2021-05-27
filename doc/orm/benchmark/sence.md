# 概述
## 测试对象
针对orm框架 jkorm 与 mybatis 性能对比
1. jkorm + hikari连接池
2. jkorm + druid连接池
3. mybatis

## 测试代码
mybatis的测试代码, 参考 
https://gitee.com/laputaqsh/mybatis-demo/

jkorm仿造写类似的代码

具体参考其他文档

## 测试结果
做了两次测试
第一次: 每个场景的方法都会分别运行 1w/5w/10w 次的测试, 来获得测试结果
http://106.53.240.212:7000/web/#/107?page_id=1035

第二次: 优化第一次的测试过程, 每个场景的测试都会运行`3轮`, 取性能最好(耗时最短)的结果, 测试更可靠
http://106.53.240.212:7000/web/#/107?page_id=1044

## 测试机器
![](img/607917878ca90.png)

## 测试数据: 部门表+员工表
```
-- 部门表
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department` (
   `id` int(11) NOT NULL auto_increment,
   `title` varchar(20) NOT NULL,
   `intro` varchar(50) NOT NULL,
   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 员工表
DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee` (
    `id` int(11) NOT NULL auto_increment,
    `title` varchar(20) NOT NULL,
    `email` varchar(50) NOT NULL,
    `gender` char(1) NOT NULL,
    `dep_id` int(11) NOT NULL DEFAULT 0,
    KEY `idx_dep_id` (`dep_id`),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `employee2`;
CREATE TABLE `employee2` like `employee`;
```

## 测试场景
每个场景的方法都会分别运行 1w/5w/10w 次, 来获得测试结果

1. 场景 `add` -- 分别往 department/employee 表中插入一条记录

2. 场景 `update` -- 先查一条 employee, 然后更新其 title

3. 场景 `delete` ---- 先查一条 employee, 然后删除该记录

4. 场景 `getDepWithEmps` -- 先查一条 department, 然后联查 employee

5. 场景 `getEmpsByConditionIf` -- 根据动态条件来查询10条 employee 记录

6. 场景 `updateEmpOnDynFields` -- 先查一条 employee, 然后更新其动态字段

7. 场景 `getEmpsByIds` -- 根据多个id 来查询 employee

## 生成sql总览

### 1 场景 `add`
1 jkorm 生成sql
```
INSERT INTO `department`  (`intro`, `id`, `title`) VALUES ('', 1, '部1') 
INSERT INTO `employee`  (`gender`, `dep_id`, `id`, `title`, `email`) VALUES ('女', 1, 1, 'Miss 1hoKy', 'Miss 1hoKy@qq.com') 
```

2 mybatis 生成sql
```
==>  Preparing: insert into department(title,intro) values(?,?)
==> Parameters: 部1(String), (String)
<==    Updates: 1

==>  Preparing: insert into employee(title,email,gender) values(?,?,?)
==> Parameters: Mr tOrDJ(String), Mr tOrDJ@qq.com(String), 男(String)
<==    Updates: 1
```


### 2 场景 `update`
1 jkorm 生成sql
```
SELECT  * FROM `employee` `employee`  WHERE  `id` = 1   LIMIT 1
UPDATE `employee` `employee`  SET `title` = 'Miss zJxbk' WHERE  `id` = 1  
```

2 mybatis 生成sql
```
==>  Preparing: select * from employee where id = ?
==> Parameters: 1(Integer)
<==    Columns: id, title, email, gender, dep_id
<==        Row: 1, Mr tOrDJ, Mr tOrDJ@qq.com, 男, 0
<==      Total: 1

==>  Preparing: update employee set title=?, email=?, gender=? where id=?
==> Parameters: Mr vTmu8(String), Mr tOrDJ@qq.com(String), 男(String), 1(Integer)
<==    Updates: 1
```


### 3 场景 `delete`
1 jkorm 生成sql
```
SELECT  * FROM `employee` `employee`  WHERE  `id` = 1   LIMIT 1
DELETE  `employee`  FROM `employee` `employee`  WHERE  `id` = 1  
```

2 mybatis 生成sql
````
==>  Preparing: select * from employee where id = ?
==> Parameters: 1(Integer)
<==    Columns: id, title, email, gender, dep_id
<==        Row: 1, Mr vTmu8, Mr tOrDJ@qq.com, 男, 0
<==      Total: 1

==>  Preparing: delete from employee where id=?
==> Parameters: 1(Integer)
<==    Updates: 1
```

### 4 场景 `getDepWithEmps`
1 jkorm 生成sql
```
SELECT  * FROM `department` `department`  WHERE  `id` = 1   LIMIT 1
SELECT  * FROM `employee` `employee`  WHERE  `employee`.`dep_id` = 1  
```

2 mybatis 生成sql

```
==>  Preparing: select * from department where id = ?
==> Parameters: 1(Integer)
<==    Columns: id, title, intro
<==        Row: 1, 部1, 

====>  Preparing: select * from employee where dep_id = ?
====> Parameters: 1(Integer)
<====      Total: 0
<==      Total: 1
```


### 5 场景 `getEmpsByConditionIf`
1 jkorm 生成sql
```
SELECT  * FROM `employee` `employee`  WHERE  `id` = 603  ORDER BY `id` DESC   LIMIT 10
```

2 mybatis 生成sql

```
==>  Preparing: select * from employee WHERE id = ? order by id desc LIMIT 0,10;
==> Parameters: 127(Integer)
<==      Total: 0
```


### 6 场景 `updateEmpOnDynFields`
1 jkorm 生成sql
```
SELECT  * FROM `employee` `employee`  WHERE  `id` = 1   LIMIT 1
UPDATE `employee` `employee`  SET `title` = 'Miss kMYyZ' WHERE  `id` = 1  
```

2 mybatis 生成sql
```
==>  Preparing: select * from employee where id = ?
==> Parameters: 1(Integer)
<==    Columns: id, title, email, gender, dep_id
<==        Row: 1, Mr tOrDJ, Mr tOrDJ@qq.com, 男, 0
<==      Total: 1

==>  Preparing: update employee set title = ? where id = ?
==> Parameters: Miss y79wA(String), 1(Integer)
<==    Updates: 1
```


### 7 场景 `getEmpsByIds`
1 jkorm 生成sql
```
SELECT  * FROM `employee` `employee`  WHERE  `id` IN (185, 837, 248)  
```

2 mybatis 生成sql
```
==>  Preparing: select * from employee where id in( ? , ? , ? )
==> Parameters: 353(Integer), 973(Integer), 724(Integer)
<==      Total: 0
```

## 注意
禁用mybatis缓存, 否则select语句缓存结果, 影响性能判断
```
    <settings>
        <!-- 去缓存: 有效 -->
        <setting name="localCacheScope" value="STATEMENT"/>
        <!-- 去缓存: 无效 -->
        <!--<setting name="cacheEnabled" value="false"/>-->
    </settings>
```