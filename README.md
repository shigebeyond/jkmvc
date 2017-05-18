# kotlin-jkmvc
Jkmvc is an elegant, powerful and lightweight MVC web framework built using kotlin. It aims to be swift, secure, and small. It will turn java's heavy development into kotlin's simple pleasure.

# usage - web
## 1 Create Filter

JkFilter is a Filter for your web application, you can configure your route rules and other initial options.

```
package com.jkmvc.example

import com.jkmvc.http.ControllerLoader
import com.jkmvc.http.JkFilter
import com.jkmvc.http.Route
import com.jkmvc.http.Router
import javax.servlet.FilterConfig

class MyFilter: JkFilter() {

    override fun init(filterConfig: FilterConfig) {
        super.init(filterConfig);
        // 添加路由规则
        // add route rule
        Router.addRoute("default",
                Route("<controller>(\\/<action>(\\/<id>)?)?", // url正则 | url pattern
                    mapOf("id" to "\\d+"), // 参数子正则 | param pattern
                    mapOf("controller" to "welcome", "action" to "index"))); // default param
        // 添加扫描controller的包
        // add package path to scan Controller
        ControllerLoader.addPackage("com.jkmvc.example.controller");
    }
}

```

## 2 Configure your Filter in web.xml

vim src/main/webapp/WEB-INF/web.xml

```
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://java.sun.com/xml/ns/javaee" xmlns:web="http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd" xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd" id="WebApp_ID" version="2.5">
	<filter>
		<filter-name>jkmvc</filter-name>
		<filter-class>com.jkmvc.example.MyFilter</filter-class>
		<init-param>
			<param-name>baseUrl</param-name>
			<param-value>/jkmvc/</param-value>
		</init-param>
	</filter>
	
	<filter-mapping>
		<filter-name>jkmvc</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>
</web-app>
```

## 3 Create Controller

Controller handles request, and render data to response.

It has property `req` to represent request, `res` to represent response.

```
package com.jkmvc.example.controller

import com.jkmvc.http.Controller

/**
 * 主页
 */
class WelcomeController: Controller() {

    /**
     * 主页
     */
    public fun actionIndex() {
        res.render("hello world");
    }

}
```

## 4 Run web server

![](https://raw.githubusercontent.com/shigebeyond/kotlin-jkmvc/master/runserver.png)

## 5 Visit web page

visit http://localhost:8081/jkmvc/

![](https://raw.githubusercontent.com/shigebeyond/kotlin-jkmvc/master/webpage.png)

# usage - view

## 1 Render View in Controller

```
package com.jkmvc.example.controller

import com.jkmvc.http.Controller

/**
 * 主页
 */
class WelcomeController: Controller() {

    /**
     * 显示jsp视图
     * render jsp view
     */
    public fun actionJsp(){
        res.render(view("index" /* view file */, mutableMapOf("name" to "shijianhang") /* view data */))
    }

}
```
## 2 Visit web page

visit http://localhost:8081/jkmvc/welcome/jsp

![](https://raw.githubusercontent.com/shigebeyond/kotlin-jkmvc/master/webview.png)


# usage - orm

Orm　provides object-oriented way to mainpulate db data.

It has 2 concepts:

1 Orm meta data: include information as follows

1.1 mapping from object to table

1.2 mapping from object's property to table's column

1.3 mapping from object's property to other object

2 Orm object | Model

2.1 visit property

you can use operator `[]` to visit orm object's property, and also use property delegate `public var id:Int by property<Int>();` to visit it

2.2 method

`query_builder()` return a query builder to query data from table

`create()` create data

`update()` update data

`delete()` delete data

## 1 Create tables

user table

```
CREATE TABLE `user` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '用户编号',
  `name` varchar(50) NOT NULL DEFAULT '' COMMENT '用户名',
  `age` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '年龄',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8 COMMENT='用户'
```

address table

```
CREATE TABLE `address` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '地址编号',
  `user_id` int(11) unsigned NOT NULL COMMENT '用户编号',
  `addr` varchar(50) NOT NULL DEFAULT '' COMMENT '地址',
  `tel` varchar(50) NOT NULL DEFAULT '' COMMENT '电话',
  `name` varchar(50) NOT NULL DEFAULT '' COMMENT '用户名',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8 COMMENT='地址';
```

## 2 Create Model

use model, extends Orm

```
package com.jkmvc.example.model

import com.jkmvc.orm.MetaData
import com.jkmvc.orm.Orm

/**
 * 用户模型
 * User　model
 */
class UserModel(id:Int? = null): Orm(id) {
    // 伴随对象就是元数据
    // company object is meta data for model
    companion object m: MetaData(UserModel::class){
        init {
            // 添加标签 + 规则
            // add label and rule for field
            addRule("name", "姓名", "notEmpty");
            addRule("age", "年龄", "between(1,120)");

            // 添加关联关系
            // add relaction for other model
            hasOne("address", AddressModel::class)
            hasMany("addresses", AddressModel::class)
        }
    }

    // 代理属性读写
    // delegate property
    public var id:Int by property<Int>();

    public var name:String by property<String>();

    public var age:Int by property<Int>();

    // 关联地址：一个用户有一个地址
    // relate to AddressModel: user has an address
    public var address:AddressModel by property<AddressModel>();

    // 关联地址：一个用户有多个地址
    // relate to AddressModel: user has many addresses
    public var addresses:List<AddressModel> by property<List<AddressModel>>();
}
```

address model, extends Orm

```
package com.jkmvc.example.model

import com.jkmvc.orm.MetaData
import com.jkmvc.orm.Orm

/**
 * 地址模型
 */
class AddressModel(id:Int? = null): Orm(id) {
    // 伴随对象就是元数据
    // company object is meta data for model
    companion object m: MetaData(AddressModel::class){
        init {
            // 添加标签 + 规则
            // add label and rule for field
            addRule("user_id", "用户", "notEmpty");
            addRule("addr", "地址", "notEmpty");
            addRule("tel", "电话", "notEmpty && digit");

            // 添加关联关系
            // add relaction for other model
            belongsTo("user", UserModel::class, "user_id")
        }
    }

    // 代理属性读写
    // delegate property
    public var id:Int by property<Int>();

    public var user_id:Int by property<Int>();

    public var addr:String by property<String>();

    public var tel:String by property<String>();

    // 关联用户：一个地址从属于一个用户
    public var user:UserModel by property<UserModel>()
}
```

## 3 Use Model in Controller

```
package com.jkmvc.example.controller

import com.jkmvc.example.model.UserModel
import com.jkmvc.http.Controller
import com.jkmvc.orm.isLoaded
import java.io.File

/**
 * 用户管理
 * user manage
 */
class UserController: Controller()
{
    /**
     * 列表页
     * list page
     */
    public fun actionIndex()
    {
        // 查询所有用户 | find all users
        val users = UserModel.queryBuilder().findAll<UserModel>()
        // 渲染视图 | render view
        res.render(view("user/index", mutableMapOf("users" to users)))
    }

    /**
     * 详情页
     * detail page
     */
    public fun actionDetail()
    {
        // 获得路由参数id: 2种写法 | 2 ways to get route parameter: "id"
        // val id = req.getIntRouteParameter("id"); // req.getRouteParameter["xxx"]
        val id:Int? = req["id"] // req["xxx"]
        // 查询单个用户 | find a user
        //val user = UserModel.queryBuilder().where("id", id).find<UserModel>()
        val user = UserModel(id)
        if(!user.isLoaded()){
            res.render("用户[$id]不存在")
            return
        }
        // 渲染视图 | render view
        val view = view("user/detail")
        view["user"] = user; // 设置视图参数 | set view data
        res.render(view)
    }

    /**
     * 新建页
     * new page
     */
    public fun actionNew()
    {
        // 处理请求 | handle request
        if(req.isPost()){ //  post请求：保存表单数据 | post request: save form data
            // 创建空的用户 | create user model
            val user = UserModel()
            // 获得请求参数：3种写法 | 3 ways to get request parameter
            /* // 1 req.getParameter("xxx");
            user.name = req.getParameter("name");
            user.age = req.getIntParameter("age", 0)!!; // 带默认值 | default value
            */
            // 2 req["xxx"]
            user.name = req["name"];
            user.age = req["age"];

            // 3 Orm.values(req)
            user.values(req)
            user.create(); // create user
            // 重定向到列表页 | redirect to list page
            redirect("user/index");
        }else{ // get请求： 渲染视图 | get request: render view
            val view = view() // 默认视图为action名： user/new | default view's name = action：　user/new
            res.render(view)
        }
    }

    /**
     * 编辑页
     * edit page
     */
    public fun actionEdit()
    {
        // 查询单个用户 | find a user
        val user = UserModel(req["id"])
        if(!user.isLoaded()){
            res.render("用户[" + req["id"] + "]不存在")
            return
        }
        // 处理请求 | handle request
        if(req.isPost()){ //  post请求：保存表单数据 | post request: save form data
            // 获得请求参数：3种写法 | 3 way to get request parameter
            /* // 1 req.getParameter("xxx");
            user.name = req.getParameter("name");
            user.age = req.getIntParameter("age", 0)!!; // 带默认值 | default value
            */
            /*// 2 req["xxx"]
            user.name = req["name"];
            user.age = req["age"];
            */
            // 3 Orm.values(req)
            user.values(req)
            user.update() // update user
            // 重定向到列表页 | redirect to list page
            redirect("user/index");
        }else{ // get请求： 渲染视图 | get request: render view
            val view = view() // 默认视图为action名： user/edit | default view's name = action：　user/edit
            view["user"] = user; // 设置视图参数 |  set view data
            res.render(view)
        }
    }

    /**
     * 删除
     * delete action
     */
    public fun actionDelete()
    {
        val id:Int? = req["id"] 
        // 查询单个用户 | find a user
        val user = UserModel(id)
        if(!user.isLoaded()){
            res.render("用户[$id]不存在")
            return
        }
        // 删除 | delete user
        user.delete();
        // 重定向到列表页 | redirect to list page
        redirect("user/index");
    }
}
```

# demo

download source and run web server

```
git clone https://github.com/shigebeyond/kotlin-jkmvc.git
cd kotlin-jkmvc
gradle jettyRun
```

visit url

http://localhost:8081/jkmvc/user/index

![](https://raw.githubusercontent.com/shigebeyond/kotlin-jkmvc/master/actionindex.png)

http://localhost:8081/jkmvc/user/detail

![](https://raw.githubusercontent.com/shigebeyond/kotlin-jkmvc/master/actiondetail.png)

http://localhost:8081/jkmvc/user/new

![](https://raw.githubusercontent.com/shigebeyond/kotlin-jkmvc/master/actionnew.png)

http://localhost:8081/jkmvc/user/edit

![](https://raw.githubusercontent.com/shigebeyond/kotlin-jkmvc/master/actionedit.png)
