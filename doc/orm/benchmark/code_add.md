# 场景add代码

jkorm很简单

mybatis很啰嗦, 多了dao与mapper.xml, 两个model的新增要写两份, 烦

=> mybatis分层多两层, 代码是jkorm的两倍

## 生成sql
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

## jkorm
JkormBenchmarkPlayer

```
   public fun add(i: Int): Int {
        Department.db.transaction {
            // 新增部门
            val dep = Department()
            dep.id = Integer(i)
            dep.title = "部" + i
            dep.intro = ""
            dep.create()

            // 新增员工
            val isMan = randomBoolean()
            val title = (if (isMan) "Mr " else "Miss ") + randomString(5);
            val gender = if (isMan) "男" else "女";
            val emp = Employee();
            emp.id = Integer(i)
            emp.title = title
            emp.email = "$title@qq.com"
            emp.gender = gender
            emp.depId = dep.id
            emp.create()
        }

        return 2
    }
```

## mybatis

### 1 调用层
MybatisBenchmarkPlayer
```
    /**
     * 新增
     */
    public fun add(i: Int): Int {
        // 新增部门
        val dep = Department(i, "部" + i, "")
        depDao.addDep(dep)

        // 新增员工
        val isMan = randomBoolean()
        val title = (if (isMan) "Mr " else "Miss ") + randomString(5);
        val gender = if (isMan) "男" else "女";
        val emp = Employee(i, title, "$title@qq.com", gender, dep);
        empDao.addEmp(emp)

        session.commit()

        return 2
    }
```

### 2 dao层
DepartmentDao
```
public interface DepartmentDao {

    /**
     * 添加部门
     * @param dep
     * @return
     */
    Long addDep(Department dep);
```

EmployeeDao
```
public interface EmployeeDao {
    /**
     * 增
     * @param emp
     * @return
     */
    Long addEmp(Employee emp);
```

### 3 mapper.xml层
DepartmentMapper.xml
```
    <insert id="addDep" parameterType="net.jkcode.jkbenchmark.orm.mybatis.model.Employee" useGeneratedKeys="true" keyProperty="id">
		insert into department(title,intro)
		values(#{title},#{intro})
	</insert>
```

EmployeeMapper.xml
```
    <insert id="addEmp" parameterType="net.jkcode.jkbenchmark.orm.mybatis.model.Employee" useGeneratedKeys="true" keyProperty="id">
		insert into employee(title,email,gender)
		values(#{title},#{email},#{gender})
	</insert>
```