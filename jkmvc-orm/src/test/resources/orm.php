<?php
use php\jkmvc\orm\Db;
use php\jkmvc\orm\Model;
$uid = 1;

// db操作
$db = Db::instance("default");
echo "查找所有用户\n";
$users = $db->query("select * from user");
var_dump($users);

$uid = 0;
if($users){
    echo "查找最大的用户id\n";
    $uid = $db->query("select max(id) as mid from user")[0]['mid'];
    echo "最大的用户id: $uid\n";

    echo "更新用户: $uid\n";
    $db->execute("update user set age = age + 1 where id = ?", [$uid]);
}

// model操作
$model = new Model("net.jkcode.jkmvc.tests.model.UserModel");
echo "创建用户\n";
$model->id = $uid + 1;
$model->age = mt_rand(0, 10);
$name = 'shi-'.$uid;
$model->username = $name;
$model->password = $name;
$model->name = $name;
$model->create();
if($uid == 0)
    $uid = $model->id;

echo "加载用户: $uid\n";
$model->load($uid);
echo "$model \n";

echo "查找用户: $uid\n";
$user = $model->find($uid);
echo "$user \n";

// db query builder
echo "查找用户: $uid\n";
$user = $db->queryBuilder()->table("user")->where('id', '=', $uid)->findRow();
var_dump($user);

echo "查找所有用户\n";
$users = $db->queryBuilder()->table("user")->findRows();
var_dump($users);

// orm query builder
echo "查找用户: $uid\n";
$user = $model->queryBuilder()->with('home')->with('addresses')->where('user.id', '=', $uid)->findModel();
echo "$user \n";

echo "查找所有用户\n";
$users = $model->queryBuilder()->with('addresses')->findModels();
foreach($users as $user)
    echo "$user \n";

echo "更新用户: $uid\n";
$user->age = $user->age + 2;
$user->save();
