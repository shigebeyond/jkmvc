# Configuration

By default, configuration files are in the directory `src/main/resources/ `

Jkmvc supports two types of configuration files

1. properties
2. yaml

## 1 Define configuration file

### 1.1 properties configuration file

Only supports 1 level of key-value pairs

eg. `jkmvc/jkmvc-http/src/main/resources/cookie.properties`

```
expiry = 604800
path = /
domain = localhost
# only in https/ssl protocol
secure = false
# forbid js reading
httponly = false
```

### 1.2 yaml configuration file

Supports multiple level of key-value pairs

eg. `jkmvc/jkmvc-orm/src/main/resources/dataSources.yaml`

```
#  database name
default:
  # master database
  master:
    driverClass: com.mysql.jdbc.Driver
    url: jdbc:mysql://127.0.0.1/test?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root
  # multiple slave databases
  slaves:
    -
      driverClass: com.mysql.jdbc.Driver
      url: jdbc:mysql://127.0.0.1/test?useUnicode=true&characterEncoding=utf-8
      username: root
      password: root
```

## 2 Use configuration

### 2.1 Use properties configuration

```
// get the configuration object, the first parameter is the configuration file  name, the second parameter is the configuration file type (default is properties)
val cookieConfig = Config.instance("cookie");
// val cookieConfig = Config.instance("cookie", "properties");

// get single configuration item
val maxAage:Int? = cookieConfig.getInt("expiry", 10); // use getInt() to get the configuration item and explicitly specify return type
val path:String? = cookieConfig["path"]; // use the [] operator to get the configuration item and implicitly specify return type
```

### 2.2 Use yaml configuration

```
// get the configuration object
// first parameter: configure file name and multiple level keys, separated by "."
// second parameter: configuration file type
val dbConfig: Config = Config.instance("dataSources.default.master", "yaml")
// get single configuration item, same as 2.1
val username: String? = config["username"]
```
