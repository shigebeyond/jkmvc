<?php
// 变量
echo "Hello $name\n";

/* // 数组
var_dump($info);
echo $info['age']."\n";
$info['sex'] = 'man';
var_dump($info); */

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
/* // 包装string类型
$obj = new WrapJavaObject($name);
// echo $obj->length()."\n";
// echo $obj->concat(" is hero")."\n";
echo $obj->substring(3)."\n"; */
// 包装hashmap类型
echo 'size: '. $javaobj->size()."\n";
$javaobj->put('price', '11.1');
echo 'size: '. $javaobj->size()."\n";
echo $javaobj->get('goods_name')."\n";
echo $javaobj->get('price')."\n";