# 场景getEmpsByConditionIf代码

jkorm很简单

mybatis很啰嗦, 多了dao与mapper.xml, 烦

=> mybatis分层多两层, 代码是jkorm的两倍
而且mybatis使用对象传递多个条件参数时, 不好指定limit参数

## 生成sql
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


## jkorm
JkormBenchmarkPlayer

```
    /**
     * 条件查询
     */
    public fun getEmpsByConditionIf(i: Int): Int {
        val query = Employee.queryBuilder()
        if (i % 2 == 1)
            query.where("id", randomInt(1000))
        else
            query.where("gender", if (randomBoolean()) "男" else "女")
        val emps = query
                .orderBy("id", true)
                .limit(10, 0)
                .findModels<Employee>()

        return 1
    }
```

## mybatis

### 1 调用层
MybatisBenchmarkPlayer
```
    /**
     * 条件查询
     */
    public fun getEmpsByConditionIf(i: Int): Int {
        val emp = Employee()
        if (i % 2 == 1)
            emp.id = randomInt(1000)
        else
            emp.gender = if (randomBoolean()) "男" else "女"
        val emps = empDao.getEmpsByConditionIf(emp)

        return 1
    }
```

### 2 dao层

EmployeeDao
```
    /**
     * 条件查询
     * @param emp
     * @return
     */
    List<Employee> getEmpsByConditionIf(Employee emp);
```

### 3 mapper.xml层

EmployeeMapper.xml
```
    <select id="getEmpsByConditionIf" resultType="net.jkcode.jkbenchmark.orm.mybatis.model.Employee">
        select * from employee
        <where>
            <if test="id!=null">
                and id = #{id}
            </if>
            <if test="title!=null">
                and title like #{title}
            </if>
            <if test="email!=null">
                and email like #{email}
            </if>
            <if test="gender!=null">
                and gender = #{gender}
            </if>
            <if test="dep!=null">
                and dep_id = #{dep.id}
            </if>
        </where>
        order by id desc
        LIMIT 0,10;
    </select>
```