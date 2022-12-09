# jkorm-canal库
jkorm-canal库, 是对[canal-client](https://github.com/alibaba/canal)的二次封装, 提供简洁的binlog增删改事件处理器类族, 来帮助你快速编写处理db数据变更处理的代码.

# 处理器类族
```
ICanalLogHandler -- 处理器接口，处理增删改日志，不同处理器可以处理不同表
CanalLogHandler -- 处理器抽象类，继承ICanalLogHandler
```

# demo

```kotlin
import net.jkcode.jkmvc.canal.*
        
// 处理单表(test库的user表)的binlog处理器
val handler1 = object:CanalLogHandler("test", "user"){
    override fun handleUpdate(oldRow: Map<String, String?>, newRow: Map<String, String?>) {
        if("age" in newRow)
            println("处理单表 age change: from [${oldRow["age"]}] to ${newRow["age"]}")
    }
}
CanalClient.addLogHandler(handler1)
// 处理所有表(test库的所有表)的binlog处理器
val handler2 = object:CanalLogHandler("test", "*"){
    override fun handleInsert(row: Map<String, String?>) {
        println("处理所有表 handleInsert: $row")
    }
    override fun handleDelete(row: Map<String, String?>) {
        println("处理所有表 handleDelete: $row")
    }
    override fun handleUpdate(oldRow: Map<String, String?>, newRow: Map<String, String?>) {
        println("处理所有表 handleUpdate: \n\tfrom: $oldRow \n\tto: $newRow")
    }
}
CanalClient.addLogHandler(handler2)
// 连接canal server，并处理收到的binlog
CanalClient.connectServer()
```
