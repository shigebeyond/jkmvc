# 场景getDepWithEmps代码

jkorm很简单

mybatis很啰嗦, 多了dao与mapper.xml, 烦

=> mybatis分层多两层, 代码是jkorm的3倍

## 生成sql
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

## jkorm
JkormBenchmarkPlayer

```
    /**
     * 部门联查员工
     */
    public fun getDepWithEmps(i: Int): Int {
        val dep = Department.queryBuilder()
                .where("id", i)
                .findModel<Department>()

        val emps = dep?.emps
        return 2
    }
```

## mybatis

### 1 调用层
MybatisBenchmarkPlayer
```

    /**
     * 部门联查员工
     */
    public fun getDepWithEmps(i: Int): Int {
        val dep = depDao.getDepByIdWithEmps2sql(i)
        val emps = dep.emps
        return 2
    }
```

### 2 dao层

EmployeeDao
```
    /**
     * 部门联查员工, 用2条sql
     * @param id
     * @return
     */
    Department getDepByIdWithEmps2sql(Integer id);
```

### 3 mapper.xml层

EmployeeMapper.xml
```
    <resultMap id="depCascadeEmps2" type="net.jkcode.jkbenchmark.orm.mybatis.model.Department">
        <!-- 设置主键映射 -->
        <id column="id" property="id"/>
        <!-- 普通字段映射 -->
        <result column="title" property="title"/>
        <result column="intro" property="intro"/>
        <!-- 级联查询员工 -->
        <collection property="emps"
                    select="net.jkcode.jkbenchmark.orm.mybatis.dao.EmployeeDao.getEmpByDepId"
                    column="id">
        </collection>
    </resultMap>
    <select id="getDepByIdWithEmps2sql" resultMap="depCascadeEmps2">
        select *
        from department
        where id = #{id}
    </select>
```