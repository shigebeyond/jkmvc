# 场景delete代码

jkorm很简单

mybatis很啰嗦, 多了dao与mapper.xml, 烦

=> mybatis分层多两层, 代码是jkorm的两倍

## 生成sql
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

## jkorm
JkormBenchmarkPlayer

```
    /**
     * 删除
     */
    public fun delete(i: Int): Int {
        val emp = Employee.findByPk<Employee>(i)
        if (emp != null)
            emp.delete()

        return 2
    }
```

## mybatis

### 1 调用层
MybatisBenchmarkPlayer
```
    /**
     * 删除
     */
    public fun delete(i: Int): Int {
        // 先查后删
        val emp = empDao.getEmpById(i)
        if (emp != null)
            empDao.delEmpById(i);
        session.commit()

        return 2
    }
```

### 2 dao层

EmployeeDao
```
    // update中用到过的select, 不重复写

    /**
     * 删
     * @param id
     * @return
     */
    Long delEmpById(Integer id);
```

### 3 mapper.xml层

EmployeeMapper.xml
```
    // update中用到过的select, 不重复写

    <delete id="delEmpById">
		delete from employee where id=#{id}
	</delete>
```