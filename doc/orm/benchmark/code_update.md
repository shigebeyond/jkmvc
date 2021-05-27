# 场景update代码

jkorm很简单

mybatis很啰嗦, 多了dao与mapper.xml, 烦

=> mybatis分层多两层, 代码是jkorm的两倍

## 生成sql
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

## jkorm
JkormBenchmarkPlayer

```
    /**
     * 更新
     */
    public fun update(i: Int): Int {
        val emp = Employee.findByPk<Employee>(i)!!
        val isMan = emp.gender == "男"
        emp.title =  (if (isMan) "Mr " else "Miss ") + randomString(5)
        emp.update()
        return 2
    }
```

## mybatis

### 1 调用层
MybatisBenchmarkPlayer
```
    /**
     * 更新
     */
    public fun update(i: Int): Int {
        val emp: Employee = empDao.getEmpById(i);
        val isMan = emp.gender == "男"
        emp.title = (if (isMan) "Mr " else "Miss ") + randomString(5);
        empDao.updateEmp(emp)
        session.commit()

        return 2
    }
```

### 2 dao层

EmployeeDao
```
    /**
     * 查单个
     * @param id
     * @return
     */
    Employee getEmpById(Integer id);
	
    /**
     * 改
     * @param emp
     * @return
     */
    Long updateEmp(Employee emp);
EmployeeDao
```

### 3 mapper.xml层

EmployeeMapper.xml
```
    <select id="getEmpById" parameterType="int" resultType="net.jkcode.jkbenchmark.orm.mybatis.model.Employee">
		select * from employee where id = #{id}
	</select>
	
    <update id="updateEmp">
		update employee
		set title=#{title}, email=#{email}, gender=#{gender}
		where id=#{id}
	</update>
```