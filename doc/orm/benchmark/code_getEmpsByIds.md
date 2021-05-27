# 场景getEmpsByIds代码

jkorm很简单

mybatis很啰嗦, 多了dao与mapper.xml, 烦

=> mybatis分层多两层, 代码是jkorm的两倍

## 生成sql
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


## jkorm
JkormBenchmarkPlayer

```
    /**
     * 多id查询(循环多id拼where in)
     */
    public fun getEmpsByIds(i: Int): Int {
        val ids = listOf(randomInt(1000), randomInt(1000), randomInt(1000))
        val emps = Employee.queryBuilder().where("id", ids).findModels<Employee>()
        return 1
    }

```

## mybatis

### 1 调用层
MybatisBenchmarkPlayer
```
    /**
     * 多id查询(循环多id拼where in)
     */
    public fun getEmpsByIds(i: Int): Int {
        val ids = listOf(randomInt(1000), randomInt(1000), randomInt(1000))
        val emps = empDao.getEmpsByConditionForeach(ids)

        return 1
    }
```

### 2 dao层

EmployeeDao
```
    /**
     * 多id查询(循环多id拼where in)
     * @param ids
     * @return
     */
    List<Employee> getEmpsByConditionForeach(@Param("ids") List<Integer> ids);
```

### 3 mapper.xml层

EmployeeMapper.xml
```
    <select id="getEmpsByConditionForeach" resultType="net.jkcode.jkbenchmark.orm.mybatis.model.Employee">
        select * from employee
        <!--
            foreach：
                collection：指定要遍历的集合（list类型的参数会特殊处理封装在map中，map的key就叫list）
                item：将当前遍历出的元素赋值给指定的变量
                separator：每个元素之间的分隔符
                open：遍历出所有结果拼接一个开始的字符串
                close：遍历出所有结果拼接一个结束的字符串
                index：遍历list的时候，index就是索引，item就是当前值
                       遍历map的时候，index就是map的key，item就是map[key]的值

            #{变量名}就能取出变量的值也就是当前遍历出的元素
         -->
        <foreach collection="ids" item="id" separator="," open="where id in(" close=")">
            #{id}
        </foreach>
    </select>
```