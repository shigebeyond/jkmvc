# 概述
jkmvc 整合jphp技术, 支持同构异语言(java/php)及相互调用, 以便支持更多的动态性, 可以用php来写db代码或模型代码, 主要是为了方便php controller调用。

一般而言, 整合jphp(动态语言)给java平台添加动态性的动机, 主要是用在网关或视图引擎上, 特别是网关上的路由、转发、聚合服务、熔断降级限流等的动态修改, 代码修改无须重启java服务, 同时php也会编译为字节码来保证性能, 另外php从语法、学习成本、使用成本、web应用、流行度、招聘等都是较好选择, 因此该整合技术是兼顾了效率与性能的较"实惠"的技术。

# 使用

1. db操作
```php
<?php
use php\jkmvc\orm\Db;
$db = Db::instance("default");
$users = $db->query("select * from user");
echo "查找所有用户\n";
var_dump($users);

if($users){
    $uid = $users[0]['id'];
    echo "更新用户: $uid\n";
    $db->execute("update user set age = age + 1 where id = ?", [$uid]);
}
```

2. model操作
```php
use php\jkmvc\orm\Model;
$model = new Model("net.jkcode.jkmvc.tests.model.UserModel");

echo "创建用户\n";
$model->age = 10;
$model->username = 'shi';
$model->password = 'shi';
$model->name = 'shi';
$model->create();

$uid = $model->id;
echo "加载用户数据: $uid\n";
$model->load($uid);
echo "$model \n";

echo "查找用户: $uid\n";
$user = $model->find($uid);
echo "$user \n";

echo "更新用户: $uid\n";
$user->age = $user->age + 2;
$user->save();
```

3. QueryBuilder调用
3.1 从 Db 中引用 QueryBuilder
```php
$uid = 1;
echo "查找用户: $uid\n";
$user = $db->queryBuilder()->table("user")->where('id', '=', $uid)->findRow();
var_dump($user);

echo "查找所有用户\n";
$users = $db->queryBuilder()->table("user")->findRows();
var_dump($users);
```

3.2 从 Model 中引用 QueryBuilder
```php
echo "查找用户: $uid\n";
$user = $model->queryBuilder()->with('home')->with('addresses')->where('user.id', '=', $uid)->findModel();
echo "$user \n";

echo "查找所有用户\n";
$users = $model->queryBuilder()->with('addresses')->findModels();
foreach($users as $user)
    echo "$user \n";
```