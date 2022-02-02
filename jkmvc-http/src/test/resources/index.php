<?php
/*
use java\io\File;
$file = new File('index.php');
echo "$file \n";
*/
echo "Hello $name\n";

// use php\io\File;
// $file = new File('/ohome/shi/code/jphp/jphp/sandbox/src/JPHP-INF/launcher.conf');
// echo 'exist: '. $file->exists() . "\n";

/*
use php\lang\JavaClass;
$cls = new JavaClass("java.util.HashMap");
$obj = $cls->newInstance(); // 返回的是 JavaObject
var_dump($obj);
// echo 'size: '. $obj->size() . "\n"; // 报错: Call to undefined method php\lang\JavaObject::size()
$method = $cls->getDeclaredMethod('size');
echo 'size: '. $method->invoke($obj) . "\n";
echo 'size: '. $method->invokeArgs($obj, []) . "\n";
*/