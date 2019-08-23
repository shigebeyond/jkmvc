# ORM

ORM：Object Relational Mapper, is the layer that sits between your database and your application. You can manipulate database in the Object-Oriented Programming way.

Jkmvc provides a powerful ORM module that uses the active record pattern and database introspection to determine a model's column information. 

The ORM allows for manipulation and control of data within a database as though it was a java object. Once you define the meta data, ORM allows you to pull data from your database, manipulate the data in any way you like, and then save the result back to the database without the use of SQL. 

By creating relationships between models that follow convention over configuration, much of the repetition of writing queries to create, read, update, and delete information from the database can be reduced or entirely removed. All of the relationships can be handled automatically by the ORM library and you can access related data as standard object properties.

## Getting started

Before we use ORM, we must define database configuration.

vim src/main/resources/dataSources.yaml

```
# database name
default:
    driverClass: com.mysql.jdbc.Driver
    url: jdbc:mysql://127.0.0.1/test?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root
```

Java use camel-case naming, so jkmvc will transform between ORM object's property name and table's column name, according to configuration item `columnUnderline` and `columnUpperCase`

vim src/main/resources/db.yaml

```
debug: true
# sharding database names, it uses sharding-jdbc
shardingDbs: shardorder
# Column name is underlined for these database names
columnUnderlineDbs: default,test
# Column name is all uppercase for these database names
columnUpperCaseDbs:
```

You can now create your [model](model.md) and [use ORM](using.md).
