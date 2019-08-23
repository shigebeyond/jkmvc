# ORM

ORM：对象与关系映射, 是在数据库与你的应用之间架设了一层。 你可以用面向对象的方式，来操作数据库。通俗一点说，在数据库里面有一个表，那么在应用程序里就有一个类与这个表相对应，类中的成员变量名与表的列名一一对应，该类的实例对应表中的一行数据。

Jkmvc提供一个强大的ORM模块，它采用的是`active record`设计模式，同时它可以自行维护表的列信息，无需手动配置。

ORM 允许你像操作java对象一样，操作数据库。一旦你定义好元数据, ORM 就可以让你在不用写一句sql的前提下，从数据库中读写数据。 

通过定义好模型之间的关联关系，ORM 会大量减少增删改查的代码。所有关联关系都会被自动处理，你只需要像访问普通对象属性一样，去访问关联数据。

## 开始

在使用 ORM 之前，你必须先定义好数据库配置：

vim src/main/resources/dataSources.yaml

```
# 数据库名
default:
    driverClass: com.mysql.jdbc.Driver
    url: jdbc:mysql://127.0.0.1/test?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root
    # 字段名是下划线命名
    columnUnderline: true
    # 字段名全大写
    columnUpperCase: false
```

Java 使用的是驼峰命名，因此 Jkmvc 会根据上面的 `columnUnderline` and `columnUpperCase` 配置项，在 ORM 对象与数据库字段名之间，自动转换命名。

vim src/main/resources/db.yaml

```
# 字段名是下划线命名的数据源, 用于逗号分隔
columnUnderline: default,test
# 字段名全大写的数据源, 用逗号分隔
columnUpperCase:
```
 
现在你就可以创建 [模型](model.cn.md)，并[使用 ORM](using.cn.md)。
