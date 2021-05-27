# 场景updateEmpOnDynFields代码

jkorm很简单

mybatis很啰嗦, 多了dao与mapper.xml, 烦

=> mybatis分层多两层, 代码是jkorm的两倍

## 生成sql
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

## jkorm
JkormBenchmarkPlayer

```
    /**
     * 更新动态字段
     */
    public fun updateEmpOnDynFields(i: Int): Int {
        // 先查后改
        val emp = Employee.findByPk<Employee>(i)
        if (emp != null) {
            if (i % 2 == 1) {
                val isMan = emp.gender == "男"
                emp.title = (if (isMan) "Mr " else "Miss ") + randomString(5);
            } else {
                emp.email = randomString(5) + "@qq.com"
            }

            emp.update()
        }

        return 2
    }
```

## mybatis

### 1 调用层
MybatisBenchmarkPlayer
```
    /**
     * 更新动态字段
     */
    public fun updateEmpOnDynFields(i: Int): Int {
        // 先查后改
        val emp0 = empDao.getEmpById(i)
        if (emp0 != null) {
            val emp = Employee()
            emp.id = i

            if (i % 2 == 1) {
                val isMan = emp.gender == "男"
                emp.title = (if (isMan) "Mr " else "Miss ") + randomString(5);
            } else {
                emp.email = randomString(5) + "@qq.com"
            }

            empDao.updateEmpOnDynFields(emp)
            session.commit()
        }

        return 2
    }
```

### 2 dao层

EmployeeDao
```
    /**
     * 更新动态字段
     * @param emp
     * @return
     */
    boolean updateEmpOnDynFields(Employee emp);
```

### 3 mapper.xml层

EmployeeMapper.xml
```
    <update id="updateEmpOnDynFields">
        update employee
        <!--
        trim：
            prefix：给拼串后的整个字符串加一个前缀
            prefixOverrides：去掉整个字符串前面多余的字符，支持或（|）
            suffix：给拼串后的整个字符串加一个后缀
            suffixOverrides：去掉整个字符串后面多余的字符，支持或（|）
        -->
        <trim prefix="set" suffixOverrides=",">
            <if test="title!=null">
                title = #{title},
            </if>
            <if test="email!=null">
                email = #{email},
            </if>
            <if test="gender!=null">
                gender = #{gender},
            </if>
            <if test="dep!=null">
                dep_id = #{dep.id},
            </if>
        </trim>
        where id = #{id}
    </update>
```