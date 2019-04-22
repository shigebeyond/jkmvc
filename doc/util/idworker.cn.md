# id生成

使用的是 Twitter的Snowflake的id算法

## 配置 src/main/resources/snow-flake-id.properties

```
# snow flake id
# 开始时间戳: 2019-01-01
startTimestamp=1546272000000
# 工作机器ID(0~31)
workerId=0
# 数据中心ID(0~31)
datacenterId=0
```

## 使用

```
val idWorker = SnowflakeIdWorker()
for (i in 0..999) {
    val id = idWorker.nextId() // 生成id
    println(java.lang.Long.toBinaryString(id))
    println(id)
}
```