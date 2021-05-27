# model代码
两个框架的model代码差不多, 都挺简单的
jkorm多了表名与关联关系配置

## jkorm的model
1 部门model, 包含表名与关联关系配置

```
/**
 * 部门
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2021-04-13 14:18:07
 */
class Department(vararg pks: Any): Orm(*pks) {

	public constructor() : this(*arrayOf())

	// 伴随对象就是元数据
 	companion object m: OrmMeta(Department::class, "部门", "department", DbKeyNames("id")){
		init {
			hasMany("emps", Employee::class, "dep_id")
		}
	}

	// 代理属性读写
	public var id:Integer by property() //

	public var title:String by property() //  

	public var intro:String by property() //

	public var emps: List<Employee> by listProperty() // 有多个员工

}
```

2 员工model

```
/**
 * 员工
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2021-04-13 14:18:34
 */
class Employee(vararg pks: Any): Orm(*pks) {

	public constructor() : this(*arrayOf())

	// 伴随对象就是元数据
 	companion object m: OrmMeta(Employee::class, "员工", "employee", DbKeyNames("id")){
		init {
			belongsTo("dep", Department::class, "dep_id")
		}
	}

	// 代理属性读写
	public var id:Integer by property() //

	public var title:String by property() //  

	public var email:String by property() //  

	public var gender:String by property() //  

	public var depId:Integer by property() //

	public var dep:Department by property() // 从属于一个部门

}
```

## mybatis的model

1 部门pojo
```
public class Department {
    private Integer id;
    private String title;
    private String intro;
    private List<Employee> emps;

    public Department(){

    }

    public Department(Integer id, String title, String intro) {
        this.id = id;
        this.title = title;
        this.intro = intro;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public List<Employee> getEmps() {
        return emps;
    }

    public void setEmps(List<Employee> emps) {
        this.emps = emps;
    }
}
```

2 员工pojo

```
public class Employee {
    private Integer id;
    private String title;
    private String email;
    private String gender;
    private Department dep;

    public Employee(){

    }

    public Employee(Integer id, String title, String email, String gender, Department dep) {
        this.id = id;
        this.title = title;
        this.email = email;
        this.gender = gender;
        this.dep = dep;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Department getDep() {
        return dep;
    }

    public void setDep(Department dep) {
        this.dep = dep;
    }
}
```