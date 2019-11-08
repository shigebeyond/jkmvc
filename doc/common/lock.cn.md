# 锁

## 锁实现, 见配置文件 lock.yaml

```
# 缓存实现
local: net.jkcode.jkmvc.lock.LocalKeyLock
jedis: net.jkcode.jkmvc.lock.JedisKeyLock
```

## 获得锁

```
val lock: IKeyLock = IKeyLock.instance("jedis")
```

## 加锁/解锁

```
// 加锁
val locked = lock.quickLock(key, 5)
if(locked){
    // 业务处理
    println("do sth")

    // 解锁
    lock.unlock(key)
}
```

等同于

```
 // 加锁, 并自动解锁
val locked = lock.quickLockCleanly(key, 5){
    // 业务处理
    println("do sth")
}
```

