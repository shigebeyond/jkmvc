<?php
// ob_start(); //打开缓冲区
for ($i=0; $i <= 9999; $i++)
{
    echo "Hello $name\n";
    foreach($friends as $f){
        if($i % 2 == 0)
            echo "-$f\n";
        else
            echo "+$f\n";
    }
}