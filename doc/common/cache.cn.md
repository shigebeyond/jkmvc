# 缓存

## 缓存实现类, 见配置文件 src/main/resources/cache.yaml

```
# 缓存实现
lru: net.jkcode.jkmvc.cache.LRUCache
jedis: net.jkcode.jkmvc.cache.JedisCache
```

下面主要介绍的是jedis的缓存实现

## jedis配置文件 src/main/resources/redis.yaml

```
default:
    address: 127.0.0.1:6379 # 可多个节点, 使用逗号分隔
    password:
    dbname:
    # 序列器类型
    serializer: jdk
```
## 获得缓存实例

```
val cache = ICache.instance("jedis")
```

## 写缓存 put(key, value, expireSencond)

```
cache.put("test", "hello world", 15) // 过期时间是15秒
```

## 读缓存 get(key): Any?

```
val data = cache.get("test")
```

## 缓存不命中自动写缓存 getOrPut(key: Any, expireSeconds:Long, waitMillis:Long = 200, dataLoader: () -> Any?): CompletableFuture<Any?>

```
// 尝试获得键为test的缓存, 如果缓存不存在, 则写缓存, 缓存时间为15秒
val dataFuture = cache.getOrPut("test", 15){
    "hello " + randString(3)
}
println("get cache: " + dataFuture.get())
```