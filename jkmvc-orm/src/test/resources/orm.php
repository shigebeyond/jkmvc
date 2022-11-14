<?php
use php\jkmvc\orm\Db;
use php\jkmvc\orm\Model;
$uid = 1;

// db操作
$db = Db::instance("default");
/*
$users = $db->query("select * from user", []);
echo "查找所有用户\n";
var_dump($users);
$uid = 0;
if($users){
    $uid = $users[0]['id'];
    echo "更新用户: $uid\n";
    $db->execute("update user set age = age + 1 where id = ?", [$uid]);
}
*/

// model操作
$model = new Model("net.jkcode.jkmvc.tests.model.UserModel");
/*
echo "创建用户: $uid\n";
$model->id = 10;
$model->age = 11;
$model->username = 'shi';
$model->password = 'shi';
$model->create();

echo "加载用户: $uid\n";
$model->load($uid);
echo "$model \n";
echo "查找用户: $uid\n";
$user = $model->find($uid);
var_dump($user);
*/

// query builder
echo "查找用户: $uid\n";
$qb = $db->queryBuilder();
$user = $qb->table("user", null)->where('id', '=', $uid)->find([]);
var_dump($user);
