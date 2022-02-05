<?php
// 变量
echo "Hello $name\n";

/* // 数组
var_dump($maparray);
echo $maparray['age']."\n";
$maparray['sex'] = 'man';
var_dump($maparray); */

/* // 文件
use php\io\File;
$file = new File('/ohome/shi/code/jphp/jphp/sandbox/src/JPHP-INF/launcher.conf');
echo "$file \n";
echo 'exist: '. $file->exists() . "\n";
 */

/* // java对象创建+调用
use php\lang\JavaClass;
$cls = new JavaClass("java.util.HashMap");
$obj = $cls->newInstance(); // 返回的是 JavaObject
var_dump($obj);
// echo 'size: '. $obj->size() . "\n"; // 报错: Call to undefined method php\lang\JavaObject::size()
$method = $cls->getDeclaredMethod('size');
echo 'size: '. $method->invoke($obj) . "\n";
echo 'size: '. $method->invokeArgs($obj, []) . "\n";
*/

// 包装java，方便调用java方法
use php\lang\WrapJavaObject;
/* // 包装string类型java对象
$strjo = new WrapJavaObject($name);
// echo $strjo->length()."\n";
// echo $strjo->concat(" is hero")."\n";
echo $strjo->substring(3)."\n"; */

// 包装hashmap类型java对象
echo 'size: '. $mapjo->size()."\n";
$mapjo->put('price', '11.1');
echo 'size: '. $mapjo->size()."\n";
echo $mapjo->get('goods_name')."\n";
echo $mapjo->get('price')."\n";

// 包装简单java对象
echo $pojo->getKey()."\n"; // 调用方法
echo $pojo->getMessage()."\n";
$pojo->key = 'title2'; // 写属性，先尝试调用setter方法，然后写属性
echo $pojo->key."\n"; // 读属性, 先尝试调用getter方法，然后读属性