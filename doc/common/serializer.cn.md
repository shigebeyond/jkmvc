# 序列器
主要做序列化处理

## 实现类, 见配置文件 serializer.yaml

```
# 序列器的类型
jdk: net.jkcode.jkmvc.serialize.JdkSerializer
fst: net.jkcode.jkmvc.serialize.FstSerializer
kryo: net.jkcode.jkmvc.serialize.KryoSerializer
hessian: net.jkcode.jkmvc.serialize.HessianSerializer
protostuff: net.jkcode.jkmvc.serialize.ProtostuffSerializer
```

## 获得序列器

```
val serializer: ISerializer = ISerializer.instance("jdk")
```

## 序列化

```
val dest = serializer.serialize(src)
```

## 反序列化

```
val src = serializer.unserialize(dest)
```