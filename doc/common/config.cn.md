# 配置

jkmvc的配置文件一般放在 `src/main/resources/` 目录下

jkmvc支持以下2种类型的配置文件

1. properties
2. yaml

## 1 配置文件定义

### 1.1 properties 类型的配置文件

只支持定义一层的键值对

如 `jkmvc/jkmvc-http/src/main/resources/cookie.properties`

```
expiry = 604800
path = /
domain = localhost
# 仅用于https/ssl
secure = false
# 禁止js脚本读取到cookie, 防止XSS攻击
httponly = false
```

### 1.2 yaml 类型的配置文件

支持定义多层的键值对

如 `jkmvc/jkmvc-orm/src/main/resources/dataSources.yaml`

```
# 数据库名
default:
  # 主库
  master:
    driverClass: com.mysql.jdbc.Driver
    url: jdbc:mysql://127.0.0.1/test?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root
  # 多个从库, 可省略
  slaves:
    -
      driverClass: com.mysql.jdbc.Driver
      url: jdbc:mysql://127.0.0.1/test?useUnicode=true&characterEncoding=utf-8
      username: root
      password: root
```

## 2 使用配置数据

### 2.1 使用 properties 类型的配置数据

```
// 获得配置对象，第一个参数是配置文件名，第二个参数是配置文件类型（或扩展名），默认是properties
val cookieConfig = Config.instance("cookie");
// val cookieConfig = Config.instance("cookie", "properties");
// 获得单个配置项
val maxAage:Int? = cookieConfig.getInt("expiry", 10); // 使用getInt()来获得配置项，并显式指定返回值类型
val path:String? = cookieConfig["path"]; // 使用[]操作符来获得配置项，隐式指定返回值类型
```

### 2.2 使用 yaml 类型的配置数据

```
// 获得配置对象
// 第一个参数：配置文件名+多层的键，以"."分隔
// 第二个参数：配置文件类型
val dbConfig: Config = Config.instance("dataSources.default.master", "yaml")
// 获得单个配置项，与2.1一样
val username: String? = config["username"]
```
